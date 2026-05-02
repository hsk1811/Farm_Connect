import db from '../database';
import { AuditLog } from '../types';

export function logAudit(
    userId: number | null,
    action: string,
    entityType: string,
    entityId: number | null,
    oldValues: any = null,
    newValues: any = null,
    ipAddress: string | null = null,
    userAgent: string | null = null
): void {
    // Fire-and-forget async insert — does not block callers
    (async () => {
        try {
            await (await db.prepare(`
                INSERT INTO audit_logs (user_id, action, entity_type, entity_id, old_values, new_values, ip_address, user_agent)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            `)).run(
                userId, action, entityType, entityId,
                oldValues ? JSON.stringify(oldValues) : null,
                newValues ? JSON.stringify(newValues) : null,
                ipAddress, userAgent
            );
        } catch (err: any) {
            console.error('Audit log error:', err);
        }
    })();
}

export async function getAuditLogs(filters: {
    userId?: number;
    entityType?: string;
    entityId?: number;
    startDate?: string;
    endDate?: string;
    page?: number;
    limit?: number;
}): Promise<{ logs: AuditLog[]; total: number }> {
    const { userId, entityType, entityId, startDate, endDate, page = 1, limit = 20 } = filters;

    let query = 'SELECT * FROM audit_logs WHERE 1=1';
    let countQuery = 'SELECT COUNT(*) as total FROM audit_logs WHERE 1=1';
    const params: any[] = [];

    if (userId) { query += ' AND user_id = ?'; countQuery += ' AND user_id = ?'; params.push(userId); }
    if (entityType) { query += ' AND entity_type = ?'; countQuery += ' AND entity_type = ?'; params.push(entityType); }
    if (entityId) { query += ' AND entity_id = ?'; countQuery += ' AND entity_id = ?'; params.push(entityId); }
    if (startDate) { query += ' AND created_at >= ?'; countQuery += ' AND created_at >= ?'; params.push(startDate); }
    if (endDate) { query += ' AND created_at <= ?'; countQuery += ' AND created_at <= ?'; params.push(endDate); }

    const countRow = await (await db.prepare(countQuery)).get(...params) as { total: number };
    const total = Number(countRow?.total ?? 0);

    query += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
    const logs = await (await db.prepare(query)).all(...params, limit, (page - 1) * limit) as AuditLog[];

    return { logs, total };
}
