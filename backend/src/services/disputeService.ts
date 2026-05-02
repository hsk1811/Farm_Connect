import db from '../database';
import { Dispute, DisputeEvidence, DisputeStatus } from '../types';
import { logAudit, paginate } from '../utils';

export interface CreateDisputeData {
  contractId: number;
  reason: string;
  description: string;
}

export async function raiseDispute(userId: number, data: CreateDisputeData): Promise<Dispute> {
  const contract = await (await db.prepare('SELECT * FROM contracts WHERE id = ?')).get(data.contractId) as any;
  if (!contract) throw new Error('Contract not found');
  if (userId !== contract.farmer_id && userId !== contract.buyer_id) throw new Error('Not authorized');
  if (!['active', 'in_progress'].includes(contract.status)) throw new Error('Can only raise disputes for active or in-progress contracts');

  const existing = await (await db.prepare(`SELECT id FROM disputes WHERE contract_id = ? AND status IN ('open', 'under_review')`)).get(data.contractId);
  if (existing) throw new Error('There is already an open dispute for this contract');

  const result = await (await db.prepare(`
        INSERT INTO disputes (contract_id, raised_by, reason, description) VALUES (?, ?, ?, ?)
    `)).run(data.contractId, userId, data.reason, data.description);

  await (await db.prepare(`UPDATE contracts SET status = 'disputed', updated_at = CURRENT_TIMESTAMP WHERE id = ?`)).run(data.contractId);

  const dispute = await (await db.prepare('SELECT * FROM disputes WHERE id = ?')).get(result.lastInsertRowid) as Dispute;
  logAudit(userId, 'DISPUTE_RAISE', 'dispute', dispute.id, null, dispute);

  return dispute;
}

export async function addDisputeEvidence(disputeId: number, userId: number, fileUrl: string, fileType: 'image' | 'pdf' | 'document', description?: string): Promise<DisputeEvidence> {
  const dispute = await (await db.prepare(`
        SELECT d.*, c.farmer_id, c.buyer_id FROM disputes d JOIN contracts c ON d.contract_id = c.id WHERE d.id = ?
    `)).get(disputeId) as any;
  if (!dispute) throw new Error('Dispute not found');
  if (userId !== dispute.farmer_id && userId !== dispute.buyer_id) throw new Error('Not authorized');
  if (['resolved', 'closed'].includes(dispute.status)) throw new Error('Cannot add evidence to resolved or closed disputes');

  const result = await (await db.prepare(`
        INSERT INTO dispute_evidence (dispute_id, uploaded_by, file_url, file_type, description) VALUES (?, ?, ?, ?, ?)
    `)).run(disputeId, userId, fileUrl, fileType, description || null);

  return await (await db.prepare('SELECT * FROM dispute_evidence WHERE id = ?')).get(result.lastInsertRowid) as DisputeEvidence;
}

export async function getDisputeById(disputeId: number): Promise<any> {
  const dispute = await (await db.prepare(`
        SELECT d.*, c.contract_number, c.crop_type, c.quantity, c.agreed_price,
          rp.full_name as raised_by_name, fp.full_name as farmer_name, bp.full_name as buyer_name
        FROM disputes d
        JOIN contracts c ON d.contract_id = c.id
        JOIN user_profiles rp ON d.raised_by = rp.user_id
        JOIN user_profiles fp ON c.farmer_id = fp.user_id
        JOIN user_profiles bp ON c.buyer_id = bp.user_id
        WHERE d.id = ?
    `)).get(disputeId);
  if (!dispute) return null;

  const evidence = await (await db.prepare(`
        SELECT de.*, up.full_name as uploaded_by_name FROM dispute_evidence de
        JOIN user_profiles up ON de.uploaded_by = up.user_id WHERE de.dispute_id = ? ORDER BY de.created_at ASC
    `)).all(disputeId);

  return { ...dispute, evidence };
}

export async function getDisputes(userId: number, status?: string, page = 1, limit = 20): Promise<any> {
  let query = `
        SELECT d.*, c.contract_number, c.crop_type, rp.full_name as raised_by_name
        FROM disputes d JOIN contracts c ON d.contract_id = c.id
        JOIN user_profiles rp ON d.raised_by = rp.user_id
        WHERE (c.farmer_id = ? OR c.buyer_id = ?)
    `;
  let countQuery = `SELECT COUNT(*) as total FROM disputes d JOIN contracts c ON d.contract_id = c.id WHERE (c.farmer_id = ? OR c.buyer_id = ?)`;
  const params: any[] = [userId, userId];

  if (status) {
    query += ' AND d.status = ?';
    countQuery += ' AND d.status = ?';
    params.push(status);
  }

  const countRow = await (await db.prepare(countQuery)).get(...params) as { total: number };
  const total = Number(countRow?.total ?? 0);

  query += ' ORDER BY d.created_at DESC LIMIT ? OFFSET ?';
  const disputes = await (await db.prepare(query)).all(...params, limit, (page - 1) * limit);

  return { disputes, pagination: paginate(page, limit, total) };
}
