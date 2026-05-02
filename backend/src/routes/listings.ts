import { Router, Response } from 'express';
import { body, query, param, validationResult } from 'express-validator';
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

// Get all listings (public, with filters)
router.get('/',
    query('cropType').optional(),
    query('variety').optional(),
    query('minPrice').optional().isNumeric(),
    query('maxPrice').optional().isNumeric(),
    query('minQuantity').optional().isNumeric(),
    query('maxQuantity').optional().isNumeric(),
    query('qualityGrade').optional().isIn(['A', 'B', 'C', 'Premium']),
    query('city').optional(),
    query('state').optional(),
    query('harvestStartAfter').optional().isISO8601(),
    query('harvestEndBefore').optional().isISO8601(),
    query('status').optional(),
    query('sortBy').optional().isIn(['price', 'quantity', 'date']),
    query('sortOrder').optional().isIn(['asc', 'desc']),
    query('page').optional().isInt({ min: 1 }),
    query('limit').optional().isInt({ min: 1, max: 50 }),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const filters = {
                cropType: req.query.cropType as string,
                variety: req.query.variety as string,
                minPrice: req.query.minPrice ? Number(req.query.minPrice) : undefined,
                maxPrice: req.query.maxPrice ? Number(req.query.maxPrice) : undefined,
                minQuantity: req.query.minQuantity ? Number(req.query.minQuantity) : undefined,
                maxQuantity: req.query.maxQuantity ? Number(req.query.maxQuantity) : undefined,
                qualityGrade: req.query.qualityGrade as string,
                city: req.query.city as string,
                state: req.query.state as string,
                harvestStartAfter: req.query.harvestStartAfter as string,
                harvestEndBefore: req.query.harvestEndBefore as string,
                status: req.query.status as any,
                sortBy: req.query.sortBy as 'price' | 'quantity' | 'date',
                sortOrder: req.query.sortOrder as 'asc' | 'desc',
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

// Get farmer's own listings
router.get('/my', authMiddleware, requireRole('farmer'),
    query('status').optional(),
    query('page').optional().isInt({ min: 1 }),
    query('limit').optional().isInt({ min: 1, max: 50 }),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const filters = {
                farmerId: req.user!.userId,
                status: req.query.status as any,
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

// Get single listing
router.get('/:id', async (req: AuthRequest, res: Response) => {
    try {
        const listing = await listingService.getListingById(Number(req.params.id));
        if (!listing) {
            return res.status(404).json({ success: false, error: 'Listing not found' });
        }
        res.json({ success: true, data: listing });
    } catch (error: any) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// Create listing (Farmer only)
router.post('/', authMiddleware, requireRole('farmer'),
    body('cropType').trim().notEmpty(),
    body('quantity').isNumeric(),
    body('unit').isIn(['kg', 'ton', 'quintal']),
    body('minPrice').isNumeric(),
    body('maxPrice').isNumeric(),
    body('harvestStartDate').isISO8601(),
    body('harvestEndDate').isISO8601(),
    body('photos').optional().isArray(),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const listing = await listingService.createListing(req.user!.userId, req.body);
            res.status(201).json({ success: true, data: listing });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// Update listing
router.put('/:id', authMiddleware, requireRole('farmer'),
    async (req: AuthRequest, res: Response) => {
        try {
            const listing = await listingService.updateListing(
                Number(req.params.id),
                req.user!.userId,
                req.body
            );
            res.json({ success: true, data: listing });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// Update listing status
router.patch('/:id/status', authMiddleware, requireRole('farmer'),
    body('status').isIn(['active', 'inactive', 'sold', 'paused', 'closed']),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const listing = await listingService.updateListingStatus(
                Number(req.params.id),
                req.user!.userId,
                req.body.status
            );
            res.json({ success: true, data: listing });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// Delete listing (soft delete)
router.delete('/:id', authMiddleware, requireRole('farmer'),
    param('id').isInt(),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            await listingService.deleteListing(Number(req.params.id), req.user!.userId);
            res.json({ success: true, message: 'Listing deleted successfully' });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// Add photo to listing
router.post('/:id/photos', authMiddleware, requireRole('farmer'),
    body('photoUrl').notEmpty(),
    body('isPrimary').optional().isBoolean(),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const photo = await listingService.addListingPhoto(
                Number(req.params.id),
                req.body.photoUrl,
                req.body.isPrimary
            );
            res.status(201).json({ success: true, data: photo });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// Delete photo
router.delete('/photos/:photoId', authMiddleware, requireRole('farmer'),
    async (req: AuthRequest, res: Response) => {
        try {
            await listingService.deleteListingPhoto(Number(req.params.photoId), req.user!.userId);
            res.json({ success: true, message: 'Photo deleted' });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

export default router;
