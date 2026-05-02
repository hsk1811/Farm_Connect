import db from '../database';
import { Contract, ContractStatus, Negotiation } from '../types';
import { logAudit, generateContractNumber, calculateTotalValue, paginate } from '../utils';

export async function createContractFromNegotiation(
  negotiationId: number,
  qualityGrade?: string,
  paymentTerms?: string,
  transportResponsibility?: string,
  additionalTerms?: string
): Promise<Contract> {
  const negotiation = await (await db.prepare(`
    SELECT n.*, l.crop_type, l.variety, l.unit, l.location_address
    FROM negotiations n
    JOIN listings l ON n.listing_id = l.id
    WHERE n.id = ? AND n.status = 'accepted'
  `)).get(negotiationId) as any;

  if (!negotiation) throw new Error('Accepted negotiation not found');

  const existing = await (await db.prepare('SELECT id FROM contracts WHERE negotiation_id = ?')).get(negotiationId);
  if (existing) throw new Error('Contract already exists for this negotiation');

  const contractNumber = generateContractNumber();
  const totalValue = calculateTotalValue(negotiation.proposed_quantity, negotiation.proposed_price);

  const result = await (await db.prepare(`
    INSERT INTO contracts (
      contract_number, negotiation_id, listing_id, farmer_id, buyer_id,
      crop_type, variety, quantity, unit, agreed_price, total_value,
      delivery_address, quality_grade, payment_terms, transport_responsibility, additional_terms, status
    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'pending')
  `)).run(
    contractNumber, negotiationId, negotiation.listing_id, negotiation.farmer_id, negotiation.buyer_id,
    negotiation.crop_type, negotiation.variety, negotiation.proposed_quantity, negotiation.unit,
    negotiation.proposed_price, totalValue, negotiation.location_address,
    qualityGrade || null, paymentTerms || null, transportResponsibility || null, additionalTerms || null
  );

  const contract = await (await db.prepare('SELECT * FROM contracts WHERE id = ?')).get(result.lastInsertRowid) as Contract;
  logAudit(negotiation.buyer_id, 'CONTRACT_CREATE', 'contract', contract.id, null, contract);

  return contract;
}

export async function confirmContract(contractId: number, userId: number): Promise<Contract> {
  const contract = await (await db.prepare('SELECT * FROM contracts WHERE id = ?')).get(contractId) as Contract;
  if (!contract) throw new Error('Contract not found');
  if (contract.status !== 'pending') throw new Error('Contract is not in pending status');

  const isFarmer = userId === contract.farmer_id;
  const isBuyer = userId === contract.buyer_id;
  if (!isFarmer && !isBuyer) throw new Error('Not authorized to confirm this contract');

  const now = new Date().toISOString();

  if (isFarmer) {
    await (await db.prepare(`
      UPDATE contracts SET farmer_confirmed = 1, farmer_confirmed_at = ?, updated_at = CURRENT_TIMESTAMP, changed_by = ? WHERE id = ?
    `)).run(now, userId, contractId);
  } else {
    await (await db.prepare(`
      UPDATE contracts SET buyer_confirmed = 1, buyer_confirmed_at = ?, updated_at = CURRENT_TIMESTAMP, changed_by = ? WHERE id = ?
    `)).run(now, userId, contractId);
  }

  const updated = await (await db.prepare('SELECT * FROM contracts WHERE id = ?')).get(contractId) as Contract;
  if (updated.farmer_confirmed && updated.buyer_confirmed) {
    await (await db.prepare(`UPDATE contracts SET status = 'active', updated_at = CURRENT_TIMESTAMP WHERE id = ?`)).run(contractId);
    await (await db.prepare(`UPDATE listings SET status = 'sold', updated_at = CURRENT_TIMESTAMP WHERE id = ?`)).run(updated.listing_id);
    logAudit(userId, 'CONTRACT_ACTIVE', 'contract', contractId, { status: 'pending' }, { status: 'active' });
    logAudit(userId, 'LISTING_SOLD', 'listing', updated.listing_id, { status: 'active' }, { status: 'sold' });
  }

  logAudit(userId, 'CONTRACT_CONFIRM', 'contract', contractId, null, { confirmedBy: isFarmer ? 'farmer' : 'buyer' });

  return await (await db.prepare('SELECT * FROM contracts WHERE id = ?')).get(contractId) as Contract;
}

export async function updateContractStatus(contractId: number, userId: number, status: ContractStatus): Promise<Contract> {
  const contract = await (await db.prepare('SELECT * FROM contracts WHERE id = ?')).get(contractId) as Contract;
  if (!contract) throw new Error('Contract not found');
  if (userId !== contract.farmer_id && userId !== contract.buyer_id) throw new Error('Not authorized');

  const validTransitions: Record<ContractStatus, ContractStatus[]> = {
    pending: ['cancelled'],
    active: ['in_progress', 'cancelled', 'disputed'],
    in_progress: ['completed', 'disputed'],
    completed: [],
    cancelled: [],
    disputed: ['in_progress', 'cancelled']
  };

  if (!validTransitions[contract.status].includes(status)) {
    throw new Error(`Cannot transition from ${contract.status} to ${status}`);
  }

  await (await db.prepare(`
    UPDATE contracts SET status = ?, updated_at = CURRENT_TIMESTAMP, changed_by = ? WHERE id = ?
  `)).run(status, userId, contractId);

  logAudit(userId, 'CONTRACT_STATUS_CHANGE', 'contract', contractId, { status: contract.status }, { status });

  return await (await db.prepare('SELECT * FROM contracts WHERE id = ?')).get(contractId) as Contract;
}

export async function getContractById(contractId: number): Promise<any> {
  const contract = await (await db.prepare(`
    SELECT c.*,
      fp.full_name as farmer_name, fp.phone as farmer_phone,
      bp.full_name as buyer_name, bp.phone as buyer_phone
    FROM contracts c
    JOIN user_profiles fp ON c.farmer_id = fp.user_id
    JOIN user_profiles bp ON c.buyer_id = bp.user_id
    WHERE c.id = ?
  `)).get(contractId) as any;

  if (!contract) return null;

  contract.farmer_confirmed = Boolean(contract.farmer_confirmed);
  contract.buyer_confirmed = Boolean(contract.buyer_confirmed);

  const milestones = await (await db.prepare(`SELECT * FROM fulfillment_milestones WHERE contract_id = ? ORDER BY created_at ASC`)).all(contractId);
  const payments = await (await db.prepare(`SELECT * FROM payments WHERE contract_id = ? ORDER BY created_at ASC`)).all(contractId);
  const disputes = await (await db.prepare(`SELECT * FROM disputes WHERE contract_id = ? ORDER BY created_at DESC`)).all(contractId);

  return { ...contract, milestones, payments, disputes };
}

export async function getContracts(userId: number, role: 'farmer' | 'buyer', status?: string, page = 1, limit = 20): Promise<any> {
  const roleField = role === 'farmer' ? 'farmer_id' : 'buyer_id';
  let query = `
    SELECT c.*,
      fp.full_name as farmer_name,
      bp.full_name as buyer_name
    FROM contracts c
    JOIN user_profiles fp ON c.farmer_id = fp.user_id
    JOIN user_profiles bp ON c.buyer_id = bp.user_id
    WHERE c.${roleField} = ?
  `;
  let countQuery = `SELECT COUNT(*) as total FROM contracts WHERE ${roleField} = ?`;
  const params: any[] = [userId];

  if (status) {
    query += ' AND c.status = ?';
    countQuery += ' AND status = ?';
    params.push(status);
  }

  const countRow = await (await db.prepare(countQuery)).get(...params) as { total: number };
  const total = Number(countRow?.total ?? 0);

  query += ' ORDER BY c.updated_at DESC LIMIT ? OFFSET ?';
  const contracts = await (await db.prepare(query)).all(...params, limit, (page - 1) * limit) as any[];

  contracts.forEach(c => {
    c.farmer_confirmed = Boolean(c.farmer_confirmed);
    c.buyer_confirmed = Boolean(c.buyer_confirmed);
  });

  return { contracts, pagination: paginate(page, limit, total) };
}
