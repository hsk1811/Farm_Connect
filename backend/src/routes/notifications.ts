import { Router, Response } from 'express';
import db from '../database';
import { authMiddleware, AuthRequest } from '../middleware/auth';

const router = Router();

// Get user notifications
router.get('/', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const { type, isRead, page = '1', limit = '50' } = req.query;
        const userId = (req.user as any).userId;
        const pageNum = parseInt(page as string);
        const limitNum = parseInt(limit as string);
        const offset = (pageNum - 1) * limitNum;

        let query = 'SELECT * FROM notifications WHERE user_id = ?';
        const params: any[] = [userId];

        if (type) { query += ' AND type = ?'; params.push(type); }
        if (isRead !== undefined) { query += ' AND is_read = ?'; params.push(isRead === 'true' ? 1 : 0); }

        query += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
        params.push(limitNum, offset);

        const notifications = await (await db.prepare(query)).all(...params) as any[];
        const unreadResult = await (await db.prepare('SELECT COUNT(*) as count FROM notifications WHERE user_id = ? AND is_read = 0')).get(userId) as any;

        const parsedNotifications = notifications.map(notif => ({
            ...notif,
            data: notif.data ? JSON.parse(notif.data) : null,
            is_read: Boolean(notif.is_read)
        }));

        res.json({ success: true, data: { notifications: parsedNotifications, unreadCount: Number(unreadResult?.count ?? 0) } });
    } catch (error) {
        console.error('Get notifications error:', error);
        res.status(500).json({ success: false, error: 'Failed to fetch notifications' });
    }
});

// Mark notification as read
router.put('/:id/read', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const { id } = req.params;
        const userId = (req.user as any).userId;

        const notification = await (await db.prepare('SELECT * FROM notifications WHERE id = ? AND user_id = ?')).get(id, userId);
        if (!notification) return res.status(404).json({ success: false, error: 'Notification not found' });

        await (await db.prepare('UPDATE notifications SET is_read = 1 WHERE id = ?')).run(id);
        const updated = await (await db.prepare('SELECT * FROM notifications WHERE id = ?')).get(id) as any;

        res.json({ success: true, data: { ...updated, data: updated.data ? JSON.parse(updated.data) : null, is_read: Boolean(updated.is_read) } });
    } catch (error) {
        console.error('Mark as read error:', error);
        res.status(500).json({ success: false, error: 'Failed to mark notification as read' });
    }
});

// Mark all notifications as read
router.put('/read-all', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const userId = (req.user as any).userId;
        await (await db.prepare('UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0')).run(userId);
        res.json({ success: true, message: 'All notifications marked as read' });
    } catch (error) {
        console.error('Mark all as read error:', error);
        res.status(500).json({ success: false, error: 'Failed to mark all as read' });
    }
});

// Delete notification
router.delete('/:id', authMiddleware, async (req: AuthRequest, res: Response) => {
    try {
        const { id } = req.params;
        const userId = (req.user as any).userId;

        const notification = await (await db.prepare('SELECT * FROM notifications WHERE id = ? AND user_id = ?')).get(id, userId);
        if (!notification) return res.status(404).json({ success: false, error: 'Notification not found' });

        await (await db.prepare('DELETE FROM notifications WHERE id = ?')).run(id);
        res.json({ success: true, message: 'Notification deleted' });
    } catch (error) {
        console.error('Delete notification error:', error);
        res.status(500).json({ success: false, error: 'Failed to delete notification' });
    }
});

export function createNotification(userId: number, type: string, title: string, message: string, data: any = null) {
    const dataJson = data ? JSON.stringify(data) : null;
    (async () => {
        try {
            await (await db.prepare('INSERT INTO notifications (user_id, type, title, message, data, is_read) VALUES (?, ?, ?, ?, ?, 0)'))
                .run(userId, type, title, message, dataJson);
        } catch (err: any) {
            console.error('Create notification error:', err);
        }
    })();
}

export default router;
