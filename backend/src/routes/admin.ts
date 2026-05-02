import { Router, Response } from 'express';
import { body, query, validationResult } from 'express-validator';
import * as adminService from '../services/adminService';
import { getAuditLogs } from '../utils/audit';
import { authMiddleware, requireRole, AuthRequest } from '../middleware';

const router = Router();

// All admin routes require admin role
router.use(authMiddleware, requireRole('admin'));

const validate = (req: any, res: Response, next: Function) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ success: false, errors: errors.array() });
    }
    next();
};

// Dashboard stats
router.get('/stats', async (req: AuthRequest, res: Response) => {
    try {
        const stats = await adminService.getAdminStats();
        res.json({ success: true, data: stats });
    } catch (error: any) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// ========== USER MANAGEMENT ==========

router.get('/users',
    query('role').optional().isIn(['farmer', 'buyer']),
    query('status').optional().isIn(['pending', 'verified', 'suspended']),
    query('page').optional().isInt({ min: 1 }),
    query('limit').optional().isInt({ min: 1, max: 50 }),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const result = await adminService.getUsers({
                role: req.query.role as string,
                status: req.query.status as string,
                page: req.query.page ? Number(req.query.page) : 1,
                limit: req.query.limit ? Number(req.query.limit) : 20
            });
            res.json({ success: true, data: result });
        } catch (error: any) {
            res.status(500).json({ success: false, error: error.message });
        }
    }
);

router.put('/users/:id/status',
    body('status').isIn(['pending', 'verified', 'suspended']),
    body('reason').optional(),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const user = await adminService.updateUserStatus(
                req.user!.userId,
                Number(req.params.id),
                req.body.status,
                req.body.reason
            );
            res.json({ success: true, data: user });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// ========== LISTING MODERATION ==========

router.get('/listings',
    query('status').optional(),
    query('page').optional().isInt({ min: 1 }),
    query('limit').optional().isInt({ min: 1, max: 50 }),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const result = await adminService.getListingsForModeration({
                status: req.query.status as string,
                page: req.query.page ? Number(req.query.page) : 1,
                limit: req.query.limit ? Number(req.query.limit) : 20
            });
            res.json({ success: true, data: result });
        } catch (error: any) {
            res.status(500).json({ success: false, error: error.message });
        }
    }
);

router.put('/listings/:id/moderate',
    body('action').isIn(['approve', 'suspend', 'close']),
    body('reason').optional(),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const listing = await adminService.moderateListing(
                req.user!.userId,
                Number(req.params.id),
                req.body.action,
                req.body.reason
            );
            res.json({ success: true, data: listing });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// ========== DISPUTE RESOLUTION ==========

router.get('/disputes',
    query('status').optional().isIn(['open', 'under_review', 'resolved', 'closed']),
    query('page').optional().isInt({ min: 1 }),
    query('limit').optional().isInt({ min: 1, max: 50 }),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const result = await adminService.getDisputesForAdmin({
                status: req.query.status as string,
                page: req.query.page ? Number(req.query.page) : 1,
                limit: req.query.limit ? Number(req.query.limit) : 20
            });
            res.json({ success: true, data: result });
        } catch (error: any) {
            res.status(500).json({ success: false, error: error.message });
        }
    }
);

router.put('/disputes/:id/resolve',
    body('status').isIn(['resolved', 'closed']),
    body('resolutionNotes').trim().notEmpty(),
    body('contractAction').optional().isIn(['resume', 'cancel']),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const dispute = await adminService.resolveDispute(
                req.user!.userId,
                Number(req.params.id),
                req.body.status,
                req.body.resolutionNotes,
                req.body.contractAction
            );
            res.json({ success: true, data: dispute });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// ========== AUDIT LOGS ==========

router.get('/audit-logs',
    query('userId').optional().isInt(),
    query('entityType').optional(),
    query('entityId').optional().isInt(),
    query('startDate').optional().isISO8601(),
    query('endDate').optional().isISO8601(),
    query('page').optional().isInt({ min: 1 }),
    query('limit').optional().isInt({ min: 1, max: 100 }),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const result = await getAuditLogs({
                userId: req.query.userId ? Number(req.query.userId) : undefined,
                entityType: req.query.entityType as string,
                entityId: req.query.entityId ? Number(req.query.entityId) : undefined,
                startDate: req.query.startDate as string,
                endDate: req.query.endDate as string,
                page: req.query.page ? Number(req.query.page) : 1,
                limit: req.query.limit ? Number(req.query.limit) : 50
            });
            res.json({ success: true, data: result });
        } catch (error: any) {
            res.status(500).json({ success: false, error: error.message });
        }
    }
);

export default router;
