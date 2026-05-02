import db from '../database';
import { User, Dispute, DisputeStatus } from '../types';
import { logAudit, paginate } from '../utils';

export async function getUsers(filters: { role?: string; status?: string; page?: number; limit?: number }): Promise<any> {
  const { role, status, page = 1, limit = 20 } = filters;

  let query = `
        SELECT u.id, u.email, u.role, u.status, u.created_at, u.updated_at,
          up.full_name, up.phone, up.city, up.state
        FROM users u JOIN user_profiles up ON u.id = up.user_id WHERE u.role != 'admin'
    `;
  let countQuery = "SELECT COUNT(*) as total FROM users WHERE role != 'admin'";
  const params: any[] = [];

  if (role) { query += ' AND u.role = ?'; countQuery += ' AND role = ?'; params.push(role); }
  if (status) { query += ' AND u.status = ?'; countQuery += ' AND status = ?'; params.push(status); }

  const countRow = await (await (await db.prepare(countQuery))).get(...params) as { total: number };
  const total = Number(countRow?.total ?? 0);

  query += ' ORDER BY u.created_at DESC LIMIT ? OFFSET ?';
  const users = await (await (await db.prepare(query))).all(...params, limit, (page - 1) * limit);

  return { users, pagination: paginate(page, limit, total) };
}

export async function updateUserStatus(adminId: number, userId: number, status: 'pending' | 'verified' | 'suspended', reason?: string): Promise<any> {
  const user = await (await (await db.prepare('SELECT * FROM users WHERE id = ?'))).get(userId) as User;
  if (!user) throw new Error('User not found');
  if (user.role === 'admin') throw new Error('Cannot modify admin users');

  await (await (await db.prepare(`UPDATE users SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?`))).run(status, userId);
  await (await (await db.prepare(`INSERT INTO admin_actions (admin_id, action_type, target_type, target_id, reason) VALUES (?, 'USER_STATUS_CHANGE', 'user', ?, ?)`))).run(adminId, userId, reason || `Status changed to ${status}`);

  logAudit(adminId, 'ADMIN_USER_STATUS', 'user', userId, { status: user.status }, { status });

  return await (await (await db.prepare(`SELECT u.id, u.email, u.role, u.status, up.full_name FROM users u JOIN user_profiles up ON u.id = up.user_id WHERE u.id = ?`))).get(userId);
}

export async function getListingsForModeration(filters: { status?: string; flagged?: boolean; page?: number; limit?: number }): Promise<any> {
  const { status, page = 1, limit = 20 } = filters;

  let query = `SELECT l.*, up.full_name as farmer_name FROM listings l JOIN user_profiles up ON l.farmer_id = up.user_id WHERE 1=1`;
  let countQuery = 'SELECT COUNT(*) as total FROM listings WHERE 1=1';
  const params: any[] = [];

  if (status) { query += ' AND l.status = ?'; countQuery += ' AND status = ?'; params.push(status); }

  const countRow = await (await (await db.prepare(countQuery))).get(...params) as { total: number };
  const total = Number(countRow?.total ?? 0);

  query += ' ORDER BY l.created_at DESC LIMIT ? OFFSET ?';
  const listings = await (await (await db.prepare(query))).all(...params, limit, (page - 1) * limit);

  return { listings, pagination: paginate(page, limit, total) };
}

export async function moderateListing(adminId: number, listingId: number, action: 'approve' | 'suspend' | 'close', reason?: string): Promise<any> {
  const listing = await (await (await db.prepare('SELECT * FROM listings WHERE id = ?'))).get(listingId);
  if (!listing) throw new Error('Listing not found');

  const newStatus = action === 'approve' ? 'active' : action === 'suspend' ? 'paused' : 'closed';
  await (await (await db.prepare(`UPDATE listings SET status = ?, updated_at = CURRENT_TIMESTAMP, changed_by = ? WHERE id = ?`))).run(newStatus, adminId, listingId);
  await (await (await db.prepare(`INSERT INTO admin_actions (admin_id, action_type, target_type, target_id, reason) VALUES (?, 'LISTING_MODERATE', 'listing', ?, ?)`))).run(adminId, listingId, reason || `Listing ${action}`);

  logAudit(adminId, 'ADMIN_LISTING_MODERATE', 'listing', listingId, listing, { status: newStatus });

  return await (await (await db.prepare('SELECT * FROM listings WHERE id = ?'))).get(listingId);
}

export async function getDisputesForAdmin(filters: { status?: string; page?: number; limit?: number }): Promise<any> {
  const { status, page = 1, limit = 20 } = filters;

  let query = `
        SELECT d.*, c.contract_number, c.crop_type, c.total_value,
          rp.full_name as raised_by_name, fp.full_name as farmer_name, bp.full_name as buyer_name
        FROM disputes d JOIN contracts c ON d.contract_id = c.id
        JOIN user_profiles rp ON d.raised_by = rp.user_id
        JOIN user_profiles fp ON c.farmer_id = fp.user_id
        JOIN user_profiles bp ON c.buyer_id = bp.user_id WHERE 1=1
    `;
  let countQuery = 'SELECT COUNT(*) as total FROM disputes WHERE 1=1';
  const params: any[] = [];

  if (status) { query += ' AND d.status = ?'; countQuery += ' AND status = ?'; params.push(status); }

  const countRow = await (await (await db.prepare(countQuery))).get(...params) as { total: number };
  const total = Number(countRow?.total ?? 0);

  query += ' ORDER BY d.created_at DESC LIMIT ? OFFSET ?';
  const disputes = await (await (await db.prepare(query))).all(...params, limit, (page - 1) * limit);

  return { disputes, pagination: paginate(page, limit, total) };
}

export async function resolveDispute(adminId: number, disputeId: number, status: 'resolved' | 'closed', resolutionNotes: string, contractAction?: 'resume' | 'cancel'): Promise<any> {
  const dispute = await (await (await db.prepare('SELECT * FROM disputes WHERE id = ?'))).get(disputeId) as Dispute;
  if (!dispute) throw new Error('Dispute not found');
  if (['resolved', 'closed'].includes(dispute.status)) throw new Error('Dispute is already resolved or closed');

  await (await (await db.prepare(`
        UPDATE disputes SET status = ?, resolution_notes = ?, resolved_by = ?, resolved_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ?
    `))).run(status, resolutionNotes, adminId, disputeId);

  if (contractAction === 'resume') await (await (await db.prepare(`UPDATE contracts SET status = 'in_progress', updated_at = CURRENT_TIMESTAMP WHERE id = ?`))).run(dispute.contract_id);
  else if (contractAction === 'cancel') await (await (await db.prepare(`UPDATE contracts SET status = 'cancelled', updated_at = CURRENT_TIMESTAMP WHERE id = ?`))).run(dispute.contract_id);

  await (await (await db.prepare(`INSERT INTO admin_actions (admin_id, action_type, target_type, target_id, reason, details) VALUES (?, 'DISPUTE_RESOLVE', 'dispute', ?, ?, ?)`))).run(adminId, disputeId, resolutionNotes, JSON.stringify({ contractAction }));

  logAudit(adminId, 'ADMIN_DISPUTE_RESOLVE', 'dispute', disputeId, { status: dispute.status }, { status, resolutionNotes });

  return await (await (await db.prepare('SELECT * FROM disputes WHERE id = ?'))).get(disputeId);
}

export async function getAdminStats(): Promise<any> {
  const [userStats, listingStats, contractStats, disputeStats] = await Promise.all([
    (await (await db.prepare(`SELECT COUNT(*) as total, SUM(CASE WHEN role = 'farmer' THEN 1 ELSE 0 END) as farmers, SUM(CASE WHEN role = 'buyer' THEN 1 ELSE 0 END) as buyers, SUM(CASE WHEN status = 'pending' THEN 1 ELSE 0 END) as pending_verification FROM users WHERE role != 'admin'`))).get(),
    (await (await db.prepare(`SELECT COUNT(*) as total, SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) as active, SUM(CASE WHEN status = 'paused' THEN 1 ELSE 0 END) as paused FROM listings`))).get(),
    (await (await db.prepare(`SELECT COUNT(*) as total, SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) as active, SUM(CASE WHEN status = 'in_progress' THEN 1 ELSE 0 END) as in_progress, SUM(CASE WHEN status = 'completed' THEN 1 ELSE 0 END) as completed, SUM(CASE WHEN status = 'disputed' THEN 1 ELSE 0 END) as disputed FROM contracts`))).get(),
    (await (await db.prepare(`SELECT COUNT(*) as total, SUM(CASE WHEN status = 'open' THEN 1 ELSE 0 END) as open, SUM(CASE WHEN status = 'under_review' THEN 1 ELSE 0 END) as under_review FROM disputes`))).get(),
  ]);

  return { users: userStats, listings: listingStats, contracts: contractStats, disputes: disputeStats };
}
