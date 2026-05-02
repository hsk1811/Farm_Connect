import { Router, Request, Response } from 'express';
import { body, validationResult } from 'express-validator';
import * as authService from '../services/authService';
import { authMiddleware, AuthRequest } from '../middleware';

const router = Router();

// Validation middleware
const validate = (req: Request, res: Response, next: Function) => {
    const errors = validationResult(req);
    if (!errors.isEmpty()) {
        return res.status(400).json({ success: false, errors: errors.array() });
    }
    next();
};

// Register
router.post('/register',
    body('email').isEmail().normalizeEmail(),
    body('password').isLength({ min: 8 }),
    body('role').isIn(['farmer', 'buyer']),
    body('fullName').trim().notEmpty(),
    body('phone').optional().isMobilePhone('any'),
    validate,
    async (req: Request, res: Response) => {
        try {
            const { email, password, role, fullName, phone } = req.body;
            const ipAddress = req.ip;

            const result = await authService.registerUser(
                { email, password, role, fullName, phone },
                ipAddress
            );

            res.status(201).json({
                success: true,
                message: 'Registration successful',
                data: {
                    userId: result.user.id,
                    email: result.user.email,
                    role: result.user.role,
                    token: result.token
                }
            });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// Login
router.post('/login',
    body('email').isEmail().normalizeEmail(),
    body('password').notEmpty(),
    validate,
    async (req: Request, res: Response) => {
        try {
            const { email, password } = req.body;
            const ipAddress = req.ip;

            const result = await authService.loginUser({ email, password }, ipAddress);

            res.json({
                success: true,
                data: {
                    userId: result.user.id,
                    email: result.user.email,
                    role: result.user.role,
                    status: result.user.status,
                    profile: result.profile,
                    token: result.token,
                    expiresIn: 86400
                }
            });
        } catch (error: any) {
            res.status(401).json({ success: false, error: error.message });
        }
    }
);

// Get current user
router.get('/me', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const result = await authService.getUserById(req.user!.userId);
        if (!result) {
            return res.status(404).json({ success: false, error: 'User not found' });
        }

        res.json({ success: true, data: result });
    } catch (error: any) {
        res.status(500).json({ success: false, error: error.message });
    }
});

// Update profile
router.put('/profile', authMiddleware,
    body('fullName').optional().trim().notEmpty(),
    body('phone').optional(),
    body('address').optional(),
    body('city').optional(),
    body('state').optional(),
    body('pincode').optional(),
    body('businessName').optional(),
    body('businessType').optional(),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const profile = await authService.updateProfile(req.user!.userId, req.body);
            res.json({ success: true, data: profile });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

// Update FCM Token
router.post('/fcm-token', authMiddleware,
    body('token').trim().notEmpty(),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            await authService.updateFcmToken(req.user!.userId, req.body.token);
            res.json({ success: true, message: 'FCM token updated' });
        } catch (error: any) {
            res.status(500).json({ success: false, error: error.message });
        }
    }
);

// Change password
router.post('/change-password', authMiddleware,
    body('currentPassword').notEmpty(),
    body('newPassword').isLength({ min: 8 }),
    validate,
    async (req: AuthRequest, res: Response) => {
        try {
            const { currentPassword, newPassword } = req.body;
            await authService.changePassword(req.user!.userId, currentPassword, newPassword);
            res.json({ success: true, message: 'Password changed successfully' });
        } catch (error: any) {
            res.status(400).json({ success: false, error: error.message });
        }
    }
);

export default router;
