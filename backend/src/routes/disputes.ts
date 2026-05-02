import { Router, Response } from 'express';
import { body, query, validationResult } from 'express-validator';
import * as disputeService from '../services/disputeService';
import { authMiddleware, AuthRequest } from '../middleware';

const router = Router();

const validate = (req: any, res: Response, next: Function) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ success: false, errors: errors.array() });
    }
    next();
};

// Get user's disputes
router.get('/', authMiddleware,
    query('status').optional(),
    query('page').optional().isInt({ min: 1 }),
    query('limit').optional().isInt({ min: 1, max: 50 }),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const result = await disputeService.getDisputes(
                req.user!.userId,
                req.query.status as string,
                req.query.page ? Number(req.query.page) : 1,
                req.query.limit ? Number(req.query.limit) : 20
            );
            res.json({ success: true, data: result });
        } catch (error: any) {
            res.status(500).json({ success: false, error: error.message });
        }
    }
);

// Get single dispute
router.get('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const dispute = await disputeService.getDisputeById(Number(req.params.id));
        if (!dispute) {
            return res.status(404).json({ success: false, error: 'Dispute not found' });
        }
        res.json({ success: true, data: dispute });
    } catch (error: any) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// Raise dispute
router.post('/', authMiddleware,
    body('contractId').isInt(),
    body('reason').trim().notEmpty(),
    body('description').trim().notEmpty(),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const dispute = await disputeService.raiseDispute(req.user!.userId, req.body);
            res.status(201).json({ success: true, data: dispute });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// Add evidence
router.post('/:id/evidence', authMiddleware,
    body('fileUrl').notEmpty(),
    body('fileType').isIn(['image', 'pdf', 'document']),
    body('description').optional(),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const evidence = await disputeService.addDisputeEvidence(
                Number(req.params.id),
                req.user!.userId,
                req.body.fileUrl,
                req.body.fileType,
                req.body.description
            );
            res.status(201).json({ success: true, data: evidence });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

export default router;
