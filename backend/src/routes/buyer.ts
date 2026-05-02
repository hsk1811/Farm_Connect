import { Router, Response } from 'express';
import { body, query, validationResult } from 'express-validator';
import * as favoriteService from '../services/favoriteService';
import * as listingService from '../services/listingService';
import { authMiddleware, requireRole, AuthRequest } from '../middleware';

const router = Router();

const validate = (req: any, res: Response, next: Function) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ success: false, errors: errors.array() });
    }
    next();
};

// Browse listings (Buyer optimized)
router.get('/listings', authMiddleware, requireRole('buyer'),
    query('cropType').optional(),
    query('minPrice').optional().isNumeric(),
    query('maxPrice').optional().isNumeric(),
    query('minQuantity').optional().isNumeric(),
    query('page').optional().isInt({ min: 1 }),
    query('limit').optional().isInt({ min: 1, max: 50 }),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const filters = {
                cropType: req.query.cropType as string,
                minPrice: req.query.minPrice ? Number(req.query.minPrice) : undefined,
                maxPrice: req.query.maxPrice ? Number(req.query.maxPrice) : undefined,
                minQuantity: req.query.minQuantity ? Number(req.query.minQuantity) : undefined,
                status: 'active' as const,
                page: req.query.page ? Number(req.query.page) : 1,
                limit: req.query.limit ? Number(req.query.limit) : 20
            };

            const result = await listingService.getListings(filters);
            res.json({ success: true, data: result });
        } catch (error: any) {
            res.status(500).json({ success: false, error: error.message });
        }
    }
);

// Get favorites
router.get('/favorites', authMiddleware, requireRole('buyer'),
    query('page').optional().isInt({ min: 1 }),
    query('limit').optional().isInt({ min: 1, max: 50 }),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const result = await favoriteService.getFavorites(
                req.user!.userId,
                req.query.page ? Number(req.query.page) : 1,
                req.query.limit ? Number(req.query.limit) : 20
            );
            res.json({ success: true, data: result });
        } catch (error: any) {
            res.status(500).json({ success: false, error: error.message });
        }
    }
);

// Add to favorites
router.post('/favorites', authMiddleware, requireRole('buyer'),
    body('listingId').isInt(),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const favorite = await favoriteService.addFavorite(req.user!.userId, req.body.listingId);
            res.status(201).json({ success: true, data: favorite });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// Remove from favorites
router.delete('/favorites/:listingId', authMiddleware, requireRole('buyer'),
    async (req: AuthRequest, res: Response) => {
        try {
            await favoriteService.removeFavorite(req.user!.userId, Number(req.params.listingId));
            res.json({ success: true, message: 'Removed from favorites' });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// Check if listing is favorite
router.get('/favorites/:listingId/check', authMiddleware, requireRole('buyer'),
    async (req: AuthRequest, res: Response) => {
        try {
            const isFavorite = await favoriteService.isFavorite(req.user!.userId, Number(req.params.listingId));
            res.json({ success: true, data: { isFavorite } });
        } catch (error: any) {
            res.status(500).json({ success: false, error: error.message });
        }
    }
);

export default router;
