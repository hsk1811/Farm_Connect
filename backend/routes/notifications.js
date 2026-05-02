const express = require('express');
const router = express.Router();
const db = require('../db');
const { authenticateToken } = require('../middleware/auth');

// Get user notifications
router.get('/', authenticateToken, async (req, res) => {
    try {
        const { type, isRead, page = 1, limit = 50 } = req.query;
        const userId = req.user.userId;
        const offset = (page - 1) * limit;

        let query = 'SELECT * FROM notifications WHERE user_id = ?';
        const params = [userId];

        if (type) {
            query += ' AND type = ?';
            params.push(type);
        }

        if (isRead !== undefined) {
            query += ' AND is_read = ?';
            params.push(isRead === 'true' ? 1 : 0);
        }

        query += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
        params.push(parseInt(limit), offset);

        const [notifications] = await db.execute(query, params);

        // Get unread count
        const [unreadResult] = await db.execute(
            'SELECT COUNT(*) as count FROM notifications WHERE user_id = ? AND is_read = 0',
            [userId]
        );

        // Parse data JSON for each notification
        const parsedNotifications = notifications.map(notif => ({
            ...notif,
            data: notif.data ? JSON.parse(notif.data) : null
        }));

        res.json({
            success: true,
            data: {
                notifications: parsedNotifications,
                unreadCount: unreadResult[0].count
            }
        });
    } catch (error) {
        console.error('Get notifications error:', error);
        res.status(500).json({ success: false, error: 'Failed to fetch notifications' });
    }
});

// Mark notification as read
router.put('/:id/read', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;
        const userId = req.user.userId;

        // Verify notification belongs to user
        const [notifications] = await db.execute(
            'SELECT * FROM notifications WHERE id = ? AND user_id = ?',
            [id, userId]
        );

        if (notifications.length === 0) {
            return res.status(404).json({ success: false, error: 'Notification not found' });
        }

        await db.execute(
            'UPDATE notifications SET is_read = 1 WHERE id = ?',
            [id]
        );

        const [updated] = await db.execute(
            'SELECT * FROM notifications WHERE id = ?',
            [id]
        );

        const notification = {
            ...updated[0],
            data: updated[0].data ? JSON.parse(updated[0].data) : null
        };

        res.json({ success: true, data: notification });
    } catch (error) {
        console.error('Mark as read error:', error);
        res.status(500).json({ success: false, error: 'Failed to mark notification as read' });
    }
});

// Mark all notifications as read
router.put('/read-all', authenticateToken, async (req, res) => {
    try {
        const userId = req.user.userId;

        await db.execute(
            'UPDATE notifications SET is_read = 1 WHERE user_id = ? AND is_read = 0',
            [userId]
        );

        res.json({ success: true, message: 'All notifications marked as read' });
    } catch (error) {
        console.error('Mark all as read error:', error);
        res.status(500).json({ success: false, error: 'Failed to mark all as read' });
    }
});

// Delete notification
router.delete('/:id', authenticateToken, async (req, res) => {
    try {
        const { id } = req.params;
        const userId = req.user.userId;

        // Verify notification belongs to user
        const [notifications] = await db.execute(
            'SELECT * FROM notifications WHERE id = ? AND user_id = ?',
            [id, userId]
        );

        if (notifications.length === 0) {
            return res.status(404).json({ success: false, error: 'Notification not found' });
        }

        await db.execute('DELETE FROM notifications WHERE id = ?', [id]);

        res.json({ success: true, message: 'Notification deleted' });
    } catch (error) {
        console.error('Delete notification error:', error);
        res.status(500).json({ success: false, error: 'Failed to delete notification' });
    }
});

// Helper function to create notification (used by other routes)
async function createNotification(userId, type, title, message, data = null) {
    try {
        const dataJson = data ? JSON.stringify(data) : null;
        
        await db.execute(
            'INSERT INTO notifications (user_id, type, title, message, data, is_read, created_at) VALUES (?, ?, ?, ?, ?, 0, NOW())',
            [userId, type, title, message, dataJson]
        );
    } catch (error) {
        console.error('Create notification error:', error);
    }
}

module.exports = router;
module.exports.createNotification = createNotification;
