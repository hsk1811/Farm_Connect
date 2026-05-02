import { Router, Response } from 'express';
import { body, query, validationResult } from 'express-validator';
import * as contractService from '../services/contractService';
import * as contractDocService from '../services/contractDocumentService';
import * as fulfillmentService from '../services/fulfillmentService';
import * as paymentService from '../services/paymentService';
import { authMiddleware, AuthRequest } from '../middleware';

const router = Router();

const validate = (req: any, res: Response, next: Function) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ success: false, errors: errors.array() });
    next();
};

// Get user's contracts
router.get('/', authMiddleware, query('status').optional(), query('page').optional().isInt({ min: 1 }), query('limit').optional().isInt({ min: 1, max: 50 }), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const result = await contractService.getContracts(req.user!.userId, req.user!.role as 'farmer' | 'buyer', req.query.status as string, req.query.page ? Number(req.query.page) : 1, req.query.limit ? Number(req.query.limit) : 20);
            res.json({ success: true, data: result });
        } catch (error: any) { res.status(500).json({ success: false, error: error.message }); }
    }
);

// Get single contract
router.get('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const contract = await contractService.getContractById(Number(req.params.id));
        if (!contract) return res.status(404).json({ success: false, error: 'Contract not found' });
        if (req.user!.role !== 'admin' && req.user!.userId !== contract.buyer_id && req.user!.userId !== contract.farmer_id)
            return res.status(403).json({ success: false, error: 'Access denied' });
        res.json({ success: true, data: contract });
    } catch (error: any) { res.status(500).json({ success: false, error: error.message }); }
});

// Confirm contract
router.post('/:id/confirm', authMiddleware, body('confirmed').isBoolean(), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            if (!req.body.confirmed) return res.status(400).json({ success: false, error: 'Confirmation required' });
            const contract = await contractService.confirmContract(Number(req.params.id), req.user!.userId);
            res.json({ success: true, data: contract });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

// Update contract status
router.patch('/:id/status', authMiddleware, body('status').isIn(['in_progress', 'completed', 'cancelled']), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const contract = await contractService.updateContractStatus(Number(req.params.id), req.user!.userId, req.body.status);
            res.json({ success: true, data: contract });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

// Finalize contract - generates hash and PDF
router.post('/:id/finalize', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const contract = await contractService.getContractById(Number(req.params.id));
        if (!contract) return res.status(404).json({ success: false, error: 'Contract not found' });
        if (req.user!.userId !== contract.farmer_id && req.user!.userId !== contract.buyer_id)
            return res.status(403).json({ success: false, error: 'Access denied' });
        if (contract.status !== 'active' && contract.status !== 'in_progress')
            return res.status(400).json({ success: false, error: 'Contract must be active before finalizing' });
        const result = await contractDocService.finalizeContract(Number(req.params.id));
        res.json({ success: true, data: result, message: 'Contract finalized with immutable hash and PDF generated' });
    } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
});

// Verify contract integrity
router.get('/:id/verify', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const result = await contractDocService.getContractWithVerification(Number(req.params.id));
        if (!result) return res.status(404).json({ success: false, error: 'Contract not found' });
        res.json({ success: true, data: { contractId: result.contract.id, contractNumber: result.contract.contract_number, isVerified: result.isVerified, hash: result.hash, pdfUrl: result.pdfUrl, message: result.isVerified ? 'Contract integrity verified - no tampering detected' : 'Warning: Contract verification failed or not yet finalized' } });
    } catch (error: any) { res.status(500).json({ success: false, error: error.message }); }
});

// ========== FULFILLMENT ==========

// Get contract fulfillment
router.get('/:id/fulfillment', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const milestones = await fulfillmentService.getMilestonesByContract(Number(req.params.id));
        res.json({ success: true, data: milestones });
    } catch (error: any) { res.status(500).json({ success: false, error: error.message }); }
});

// Add milestone
router.post('/:id/fulfillment/milestones', authMiddleware, body('milestoneType').isIn(['scheduled', 'dispatched', 'delivered', 'completed']), body('scheduledDate').optional().isISO8601(), body('notes').optional(), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const milestone = await fulfillmentService.createMilestone(Number(req.params.id), req.user!.userId, req.body);
            res.status(201).json({ success: true, data: milestone });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

// Update milestone
router.put('/fulfillment/milestones/:milestoneId', authMiddleware, body('status').isIn(['pending', 'in_progress', 'completed', 'skipped']), body('completedDate').optional().isISO8601(), body('notes').optional(), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const milestone = await fulfillmentService.updateMilestone(Number(req.params.milestoneId), req.user!.userId, req.body.status, req.body.completedDate, req.body.notes);
            if (req.body.status === 'completed') await fulfillmentService.checkContractCompletion((milestone as any).contract_id);
            res.json({ success: true, data: milestone });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

// Upload milestone proof
router.post('/fulfillment/milestones/:milestoneId/proof', authMiddleware, body('proofUrl').notEmpty(), body('proofType').isIn(['image', 'pdf']), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const milestone = await fulfillmentService.uploadMilestoneProof(Number(req.params.milestoneId), req.user!.userId, req.body.proofUrl, req.body.proofType);
            res.json({ success: true, data: milestone });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

// ========== PAYMENTS ==========

// Get contract payments
router.get('/:id/payments', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const [payments, summary] = await Promise.all([paymentService.getPaymentsByContract(Number(req.params.id)), paymentService.getPaymentSummary(Number(req.params.id))]);
        res.json({ success: true, data: { payments, summary } });
    } catch (error: any) { res.status(500).json({ success: false, error: error.message }); }
});

// Record payment
router.post('/:id/payments', authMiddleware, body('amount').isNumeric(), body('paymentMethod').isIn(['cash', 'upi', 'bank_transfer']), body('paymentStatus').isIn(['pending', 'partial', 'paid', 'failed']), body('transactionId').optional(), body('paymentDate').optional().isISO8601(), body('notes').optional(), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const payment = await paymentService.recordPayment(Number(req.params.id), req.user!.userId, req.body);
            res.status(201).json({ success: true, data: payment });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

// Upload payment receipt
router.post('/payments/:paymentId/receipt', authMiddleware, body('receiptUrl').notEmpty(), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const payment = await paymentService.uploadPaymentReceipt(Number(req.params.paymentId), req.user!.userId, req.body.receiptUrl);
            res.json({ success: true, data: payment });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

export default router;
