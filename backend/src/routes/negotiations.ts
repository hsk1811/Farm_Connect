import { Router, Response } from 'express';
import { body, query, validationResult } from 'express-validator';
import * as negotiationService from '../services/negotiationService';
import * as contractService from '../services/contractService';
import { authMiddleware, AuthRequest } from '../middleware';
import { createNotification } from './notifications';
import db from '../database';

const router = Router();

const validate = (req: any, res: Response, next: Function) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) return res.status(400).json({ success: false, errors: errors.array() });
    next();
};

// Get user's negotiations
router.get('/', authMiddleware, query('status').optional(), query('page').optional().isInt({ min: 1 }), query('limit').optional().isInt({ min: 1, max: 50 }), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const result = await negotiationService.getNegotiations(req.user!.userId, req.user!.role as 'farmer' | 'buyer', req.query.status as string, req.query.page ? Number(req.query.page) : 1, req.query.limit ? Number(req.query.limit) : 20);
            res.json({ success: true, data: result });
        } catch (error: any) { res.status(500).json({ success: false, error: error.message }); }
    }
);

// Get single negotiation
router.get('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const negotiation = await negotiationService.getNegotiationById(Number(req.params.id));
        if (!negotiation) return res.status(404).json({ success: false, error: 'Negotiation not found' });
        if (req.user!.userId !== negotiation.buyer_id && req.user!.userId !== negotiation.farmer_id)
            return res.status(403).json({ success: false, error: 'Access denied' });
        res.json({ success: true, data: negotiation });
    } catch (error: any) { res.status(500).json({ success: false, error: error.message }); }
});

// Create negotiation (Buyer only)
router.post('/', authMiddleware, body('listingId').isInt(), body('proposedPrice').isNumeric(), body('proposedQuantity').isNumeric(), body('message').optional(), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            if (req.user!.role !== 'buyer') return res.status(403).json({ success: false, error: 'Only buyers can initiate negotiations' });

            const negotiation = await negotiationService.createNegotiation(req.user!.userId, req.body);

            // Create notification for farmer (fire-and-forget)
            (async () => {
                try {
                    const listing = await db.prepare('SELECT farmer_id, crop_type, unit FROM listings WHERE id = ?').get(req.body.listingId) as any;
                    if (listing) createNotification(listing.farmer_id, 'negotiation', 'New Offer Received', `New offer of ₹${req.body.proposedPrice} for ${req.body.proposedQuantity} ${listing.unit} of ${listing.crop_type}`, { negotiation_id: negotiation.id, listing_id: req.body.listingId });
                } catch (e: any) {
                    console.error('Notification error:', e);
                }
            })();

            res.status(201).json({ success: true, data: negotiation });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

// Add message to negotiation
router.post('/:id/messages', authMiddleware, body('messageType').isIn(['text', 'counter_proposal']), body('message').optional(), body('proposedPrice').optional().isNumeric(), body('proposedQuantity').optional().isNumeric(), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const message = await negotiationService.addMessage(Number(req.params.id), req.user!.userId, req.body.messageType, req.body.message, req.body.proposedPrice, req.body.proposedQuantity);

            // Create notification for the other party (fire-and-forget)
            (async () => {
                try {
                    const neg = await db.prepare('SELECT buyer_id, farmer_id, listing_id FROM negotiations WHERE id = ?').get(req.params.id) as any;
                    if (!neg) return;
                    const recipientId = neg.buyer_id === req.user!.userId ? neg.farmer_id : neg.buyer_id;
                    const listing = await db.prepare('SELECT crop_type FROM listings WHERE id = ?').get(neg.listing_id) as any;
                    let notifTitle = 'New Message';
                    let notifMessage = req.body.message || 'You have a new message';
                    if (req.body.messageType === 'counter_proposal') {
                        notifTitle = 'Counter Offer Received';
                        notifMessage = `New counter offer: ₹${req.body.proposedPrice} for ${req.body.proposedQuantity} ${listing?.crop_type || 'units'}`;
                    }
                    createNotification(recipientId, 'negotiation', notifTitle, notifMessage, { negotiation_id: Number(req.params.id), listing_id: neg.listing_id });
                } catch (e: any) {
                    console.error('Notification error:', e);
                }
            })();

            res.status(201).json({ success: true, data: message });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

// Accept negotiation
router.post('/:id/accept', authMiddleware, body('finalPrice').isNumeric(), body('finalQuantity').isNumeric(), validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const negotiation = await negotiationService.acceptNegotiation(Number(req.params.id), req.user!.userId, req.body.finalPrice, req.body.finalQuantity);
            const contract = await contractService.createContractFromNegotiation(negotiation.id, req.body.qualityGrade, req.body.paymentTerms, req.body.transportResponsibility, req.body.additionalTerms);

            const otherUserId = negotiation.farmer_id === req.user!.userId ? negotiation.buyer_id : negotiation.farmer_id;
            createNotification(otherUserId, 'contract', 'Negotiation Accepted', `Contract #${contract.id} created for ₹${negotiation.proposed_price}`, { contract_id: contract.id, negotiation_id: negotiation.id });

            res.json({ success: true, data: { negotiation, contract }, message: 'Negotiation accepted. Contract created.' });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

// Reject negotiation
router.post('/:id/reject', authMiddleware, body('reason').optional(),
    async (req: AuthRequest, res: Response) => {
        try {
            const negotiation = await negotiationService.rejectNegotiation(Number(req.params.id), req.user!.userId, req.body.reason);
            res.json({ success: true, data: negotiation });
        } catch (error: any) { res.status(400).json({ success: false, error: error.message }); }
    }
);

export default router;
