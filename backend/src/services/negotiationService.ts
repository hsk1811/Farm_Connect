import db from '../database';
import { Negotiation, NegotiationMessage } from '../types';
import { logAudit, calculateExpiryDate, paginate } from '../utils';

export interface CreateNegotiationData {
  listingId: number;
  proposedPrice: number;
  proposedQuantity: number;
  message?: string;
}

export async function createNegotiation(buyerId: number, data: CreateNegotiationData): Promise<Negotiation> {
  const { listingId, proposedPrice, proposedQuantity, message } = data;

  const listing = await db.prepare('SELECT * FROM listings WHERE id = ?').get(listingId) as any;
  if (!listing) throw new Error('Listing not found');
  if (listing.status !== 'active') throw new Error('Listing is not active');
  if (listing.farmer_id === buyerId) throw new Error('Cannot negotiate on your own listing');

  const existing = await db.prepare(`
    SELECT id FROM negotiations WHERE listing_id = ? AND buyer_id = ? AND status = 'open'
  `).get(listingId, buyerId);
  if (existing) throw new Error('You already have an open negotiation for this listing');

  const expiresAt = calculateExpiryDate(7);

  const result = await db.prepare(`
    INSERT INTO negotiations (listing_id, buyer_id, farmer_id, proposed_price, proposed_quantity, expires_at)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(listingId, buyerId, listing.farmer_id, proposedPrice, proposedQuantity, expiresAt);

  const negotiationId = result.lastInsertRowid as number;

  await db.prepare(`
    INSERT INTO negotiation_messages (negotiation_id, sender_id, message_type, message, proposed_price, proposed_quantity)
    VALUES (?, ?, 'proposal', ?, ?, ?)
  `).run(negotiationId, buyerId, message || 'Initial proposal', proposedPrice, proposedQuantity);

  const negotiation = await db.prepare('SELECT * FROM negotiations WHERE id = ?').get(negotiationId) as Negotiation;
  logAudit(buyerId, 'NEGOTIATION_CREATE', 'negotiation', negotiationId, null, negotiation);

  return negotiation;
}

export async function addMessage(
  negotiationId: number,
  senderId: number,
  messageType: 'text' | 'proposal' | 'counter_proposal' | 'accept' | 'reject',
  message?: string,
  proposedPrice?: number,
  proposedQuantity?: number
): Promise<NegotiationMessage> {
  const negotiation = await db.prepare('SELECT * FROM negotiations WHERE id = ?').get(negotiationId) as Negotiation;
  if (!negotiation) throw new Error('Negotiation not found');
  if (negotiation.status !== 'open' && negotiation.status !== 'accepted') throw new Error('Negotiation is not open or accepted');
  if (senderId !== negotiation.buyer_id && senderId !== negotiation.farmer_id) throw new Error('Not authorized to send messages in this negotiation');

  const result = await db.prepare(`
    INSERT INTO negotiation_messages (negotiation_id, sender_id, message_type, message, proposed_price, proposed_quantity)
    VALUES (?, ?, ?, ?, ?, ?)
  `).run(negotiationId, senderId, messageType, message || null, proposedPrice || null, proposedQuantity || null);

  if (messageType === 'counter_proposal' && proposedPrice && proposedQuantity) {
    await db.prepare(`
      UPDATE negotiations SET proposed_price = ?, proposed_quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
    `).run(proposedPrice, proposedQuantity, negotiationId);
  }

  return await db.prepare('SELECT * FROM negotiation_messages WHERE id = ?').get(result.lastInsertRowid) as NegotiationMessage;
}

export async function acceptNegotiation(negotiationId: number, userId: number, finalPrice: number, finalQuantity: number): Promise<Negotiation> {
  const negotiation = await db.prepare('SELECT * FROM negotiations WHERE id = ?').get(negotiationId) as Negotiation;
  if (!negotiation) throw new Error('Negotiation not found');
  if (negotiation.status !== 'open') throw new Error('Negotiation is not open');
  if (userId !== negotiation.buyer_id && userId !== negotiation.farmer_id) throw new Error('Not authorized');

  await addMessage(negotiationId, userId, 'accept', 'Terms accepted', finalPrice, finalQuantity);

  await db.prepare(`
    UPDATE negotiations SET status = 'accepted', proposed_price = ?, proposed_quantity = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
  `).run(finalPrice, finalQuantity, negotiationId);

  logAudit(userId, 'NEGOTIATION_ACCEPT', 'negotiation', negotiationId, { status: 'open' }, { status: 'accepted' });

  return await db.prepare('SELECT * FROM negotiations WHERE id = ?').get(negotiationId) as Negotiation;
}

export async function rejectNegotiation(negotiationId: number, userId: number, reason?: string): Promise<Negotiation> {
  const negotiation = await db.prepare('SELECT * FROM negotiations WHERE id = ?').get(negotiationId) as Negotiation;
  if (!negotiation) throw new Error('Negotiation not found');
  if (negotiation.status !== 'open') throw new Error('Negotiation is not open');
  if (userId !== negotiation.buyer_id && userId !== negotiation.farmer_id) throw new Error('Not authorized');

  await addMessage(negotiationId, userId, 'reject', reason || 'Negotiation rejected');

  await db.prepare(`
    UPDATE negotiations SET status = 'rejected', updated_at = CURRENT_TIMESTAMP WHERE id = ?
  `).run(negotiationId);

  logAudit(userId, 'NEGOTIATION_REJECT', 'negotiation', negotiationId, { status: 'open' }, { status: 'rejected' });

  return await db.prepare('SELECT * FROM negotiations WHERE id = ?').get(negotiationId) as Negotiation;
}

export async function getNegotiationById(negotiationId: number): Promise<any> {
  const negotiation = await db.prepare(`
    SELECT n.*,
      l.crop_type, l.variety, l.quantity as listing_quantity, l.unit, l.min_price, l.max_price,
      COALESCE(bp.full_name, 'Buyer') as buyer_name,
      COALESCE(fp.full_name, 'Farmer') as farmer_name
    FROM negotiations n
    JOIN listings l ON n.listing_id = l.id
    LEFT JOIN user_profiles bp ON n.buyer_id = bp.user_id
    LEFT JOIN user_profiles fp ON n.farmer_id = fp.user_id
    WHERE n.id = ?
  `).get(negotiationId);

  if (!negotiation) return null;

  const messages = await db.prepare(`
    SELECT nm.*, COALESCE(up.full_name, 'User') as sender_name
    FROM negotiation_messages nm
    LEFT JOIN user_profiles up ON nm.sender_id = up.user_id
    WHERE nm.negotiation_id = ?
    ORDER BY nm.created_at ASC
  `).all(negotiationId);

  return { ...negotiation, messages };
}

export async function getNegotiations(userId: number, role: 'farmer' | 'buyer', status?: string, page = 1, limit = 20): Promise<any> {
  const roleField = role === 'farmer' ? 'farmer_id' : 'buyer_id';
  let query = `
    SELECT n.*,
      l.crop_type, l.variety,
      COALESCE(bp.full_name, 'Buyer') as buyer_name,
      COALESCE(fp.full_name, 'Farmer') as farmer_name
    FROM negotiations n
    JOIN listings l ON n.listing_id = l.id
    LEFT JOIN user_profiles bp ON n.buyer_id = bp.user_id
    LEFT JOIN user_profiles fp ON n.farmer_id = fp.user_id
    WHERE n.${roleField} = ?
  `;
  let countQuery = `SELECT COUNT(*) as total FROM negotiations WHERE ${roleField} = ?`;
  const params: any[] = [userId];

  if (status) {
    query += ' AND n.status = ?';
    countQuery += ' AND status = ?';
    params.push(status);
  }

  const countRow = await db.prepare(countQuery).get(...params) as { total: number };
  const total = Number(countRow?.total ?? 0);

  query += ' ORDER BY n.updated_at DESC LIMIT ? OFFSET ?';
  const negotiations = await db.prepare(query).all(...params, limit, (page - 1) * limit);

  return { negotiations, pagination: paginate(page, limit, total) };
}

export async function expireOldNegotiations(): Promise<number> {
  const result = await db.prepare(`
    UPDATE negotiations SET status = 'expired', updated_at = CURRENT_TIMESTAMP
    WHERE status = 'open' AND expires_at < NOW()
  `).run();

  return result.changes;
}
