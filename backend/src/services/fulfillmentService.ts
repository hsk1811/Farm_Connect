import db from '../database';
import { FulfillmentMilestone, MilestoneType, MilestoneStatus } from '../types';
import { logAudit } from '../utils';

export interface CreateMilestoneData {
    milestoneType: MilestoneType;
    scheduledDate?: string;
    notes?: string;
}

export async function createMilestone(contractId: number, userId: number, data: CreateMilestoneData): Promise<FulfillmentMilestone> {
    const contract = await (await db.prepare('SELECT * FROM contracts WHERE id = ?')).get(contractId) as any;
    if (!contract) throw new Error('Contract not found');
    if (userId !== contract.farmer_id && userId !== contract.buyer_id) throw new Error('Not authorized');
    if (!['active', 'in_progress'].includes(contract.status)) throw new Error('Contract must be active or in progress');

    const result = await (await db.prepare(`
        INSERT INTO fulfillment_milestones (contract_id, milestone_type, scheduled_date, notes, updated_by)
        VALUES (?, ?, ?, ?, ?)
    `)).run(contractId, data.milestoneType, data.scheduledDate || null, data.notes || null, userId);

    if (contract.status === 'active') {
        await (await db.prepare(`UPDATE contracts SET status = 'in_progress', updated_at = CURRENT_TIMESTAMP WHERE id = ?`)).run(contractId);
    }

    const milestone = await (await db.prepare('SELECT * FROM fulfillment_milestones WHERE id = ?')).get(result.lastInsertRowid) as FulfillmentMilestone;
    logAudit(userId, 'MILESTONE_CREATE', 'fulfillment', milestone.id, null, milestone);

    return milestone;
}

export async function updateMilestone(milestoneId: number, userId: number, status: MilestoneStatus, completedDate?: string, notes?: string): Promise<FulfillmentMilestone> {
    const milestone = await (await db.prepare(`
        SELECT fm.*, c.farmer_id, c.buyer_id, c.status as contract_status FROM fulfillment_milestones fm
        JOIN contracts c ON fm.contract_id = c.id WHERE fm.id = ?
    `)).get(milestoneId) as any;
    if (!milestone) throw new Error('Milestone not found');
    if (userId !== milestone.farmer_id && userId !== milestone.buyer_id) throw new Error('Not authorized');

    await (await db.prepare(`
        UPDATE fulfillment_milestones SET status = ?, completed_date = ?, notes = COALESCE(?, notes), updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?
    `)).run(status, completedDate || null, notes, userId, milestoneId);

    logAudit(userId, 'MILESTONE_UPDATE', 'fulfillment', milestoneId, { status: milestone.status }, { status });

    return await (await db.prepare('SELECT * FROM fulfillment_milestones WHERE id = ?')).get(milestoneId) as FulfillmentMilestone;
}

export async function uploadMilestoneProof(milestoneId: number, userId: number, proofUrl: string, proofType: 'image' | 'pdf'): Promise<FulfillmentMilestone> {
    const milestone = await (await db.prepare(`
        SELECT fm.*, c.farmer_id, c.buyer_id FROM fulfillment_milestones fm JOIN contracts c ON fm.contract_id = c.id WHERE fm.id = ?
    `)).get(milestoneId) as any;
    if (!milestone) throw new Error('Milestone not found');
    if (userId !== milestone.farmer_id && userId !== milestone.buyer_id) throw new Error('Not authorized');

    await (await db.prepare(`UPDATE fulfillment_milestones SET proof_url = ?, proof_type = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?`)).run(proofUrl, proofType, userId, milestoneId);

    return await (await db.prepare('SELECT * FROM fulfillment_milestones WHERE id = ?')).get(milestoneId) as FulfillmentMilestone;
}

export async function getMilestonesByContract(contractId: number): Promise<FulfillmentMilestone[]> {
    return await (await db.prepare(`
        SELECT fm.*, up.full_name as updated_by_name FROM fulfillment_milestones fm
        LEFT JOIN user_profiles up ON fm.updated_by = up.user_id WHERE fm.contract_id = ? ORDER BY fm.created_at ASC
    `)).all(contractId) as FulfillmentMilestone[];
}

export async function checkContractCompletion(contractId: number): Promise<boolean> {
    const milestones = await (await db.prepare(`SELECT * FROM fulfillment_milestones WHERE contract_id = ?`)).all(contractId) as FulfillmentMilestone[];
    const hasCompleted = milestones.some(m => m.milestone_type === 'completed' && m.status === 'completed');

    if (hasCompleted) {
        await (await db.prepare(`UPDATE contracts SET status = 'completed', updated_at = CURRENT_TIMESTAMP WHERE id = ?`)).run(contractId);
        return true;
    }
    return false;
}
