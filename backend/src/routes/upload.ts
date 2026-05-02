import { Router, Request, Response } from 'express';
import multer from 'multer';
import path from 'path';
import { v4 as uuidv4 } from 'uuid';
import { AuthRequest, authMiddleware } from '../middleware';
import { supabaseClient } from '../config/supabaseClient';

const router = Router();

// Configure storage entirely in memory
const storage = multer.memoryStorage();

// Filter for images only
const fileFilter = (req: any, file: Express.Multer.File, cb: multer.FileFilterCallback) => {
    if (file.mimetype.startsWith('image/')) {
        cb(null, true);
    } else {
        cb(new Error('Only image files are allowed!'));
    }
};

const upload = multer({
    storage: storage,
    fileFilter: fileFilter,
    limits: {
        fileSize: 5 * 1024 * 1024 // 5MB limit
    }
});

/**
 * @route POST /api/upload
 * @desc Upload one or multiple files
 * @access Private
 */
router.post('/', authMiddleware, upload.array('files', 5), async (req: AuthRequest, res: Response) => {
    try {
        if (!req.files || (req.files as Express.Multer.File[]).length === 0) {
            return res.status(400).json({ success: false, message: 'No files uploaded' });
        }

        const files = req.files as Express.Multer.File[];
        const fileUrls: string[] = [];

        for (const file of files) {
            const ext = path.extname(file.originalname);
            const filename = `${uuidv4()}${ext}`;
            const filePath = `images/${filename}`;

            // Upload the file buffer to Supabase
            const { data, error } = await supabaseClient
                .storage
                .from('farmconnect-uploads')
                .upload(filePath, file.buffer, {
                    contentType: file.mimetype,
                    upsert: false
                });

            if (error) {
                console.error('Supabase upload error for file', file.originalname, error);
                throw error;
            }

            // Retrieve the public URL
            const { data: publicUrlData } = supabaseClient
                .storage
                .from('farmconnect-uploads')
                .getPublicUrl(filePath);

            fileUrls.push(publicUrlData.publicUrl);
        }

        res.json({
            success: true,
            message: 'Files uploaded successfully',
            data: {
                urls: fileUrls
            }
        });
    } catch (error: any) {
        console.error('Upload error:', error);
        res.status(500).json({ success: false, error: 'Server error during upload' });
    }
});

export default router;
