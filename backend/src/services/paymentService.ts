import db from '../database';
import { Payment, PaymentMethod, PaymentStatus } from '../types';
import { logAudit, paginate } from '../utils';

export interface CreatePaymentData {
    amount: number;
    paymentMethod: PaymentMethod;
    paymentStatus: PaymentStatus;
    transactionId?: string;
    paymentDate?: string;
    notes?: string;
}

export async function recordPayment(contractId: number, userId: number, data: CreatePaymentData): Promise<Payment> {
    const contract = await (await db.prepare('SELECT * FROM contracts WHERE id = ?')).get(contractId) as any;
    if (!contract) throw new Error('Contract not found');
    if (userId !== contract.farmer_id && userId !== contract.buyer_id) throw new Error('Not authorized');
    if (!['active', 'in_progress', 'completed'].includes(contract.status)) throw new Error('Cannot record payment for this contract status');

    const result = await (await db.prepare(`
        INSERT INTO payments (contract_id, amount, payment_method, payment_status, transaction_id, payment_date, notes, recorded_by)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
    `)).run(contractId, data.amount, data.paymentMethod, data.paymentStatus, data.transactionId || null, data.paymentDate || new Date().toISOString(), data.notes || null, userId);

    const payment = await (await db.prepare('SELECT * FROM payments WHERE id = ?')).get(result.lastInsertRowid) as Payment;
    logAudit(userId, 'PAYMENT_RECORD', 'payment', payment.id, null, payment);

    return payment;
}

export async function uploadPaymentReceipt(paymentId: number, userId: number, receiptUrl: string): Promise<Payment> {
    const payment = await (await db.prepare(`
        SELECT p.*, c.farmer_id, c.buyer_id FROM payments p JOIN contracts c ON p.contract_id = c.id WHERE p.id = ?
    `)).get(paymentId) as any;
    if (!payment) throw new Error('Payment not found');
    if (userId !== payment.farmer_id && userId !== payment.buyer_id) throw new Error('Not authorized');

    await (await db.prepare(`UPDATE payments SET receipt_url = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?`)).run(receiptUrl, paymentId);

    return await (await db.prepare('SELECT * FROM payments WHERE id = ?')).get(paymentId) as Payment;
}

export async function getPaymentsByContract(contractId: number): Promise<Payment[]> {
    return await (await db.prepare(`
        SELECT p.*, up.full_name as recorded_by_name FROM payments p
        LEFT JOIN user_profiles up ON p.recorded_by = up.user_id
        WHERE p.contract_id = ? ORDER BY p.created_at DESC
    `)).all(contractId) as Payment[];
}

export async function getPaymentSummary(contractId: number): Promise<{ totalPaid: number; totalValue: number; remaining: number; status: string }> {
    const contract = await (await db.prepare('SELECT total_value FROM contracts WHERE id = ?')).get(contractId) as { total_value: number };
    const payments = await (await db.prepare(`SELECT COALESCE(SUM(amount), 0) as total FROM payments WHERE contract_id = ? AND payment_status IN ('paid', 'partial')`)).get(contractId) as { total: number };

    const totalPaid = Number(payments?.total ?? 0);
    const totalValue = Number(contract?.total_value ?? 0);
    const remaining = totalValue - totalPaid;
    const status = totalPaid >= totalValue ? 'paid' : totalPaid > 0 ? 'partial' : 'pending';

    return { totalPaid, totalValue, remaining, status };
}
