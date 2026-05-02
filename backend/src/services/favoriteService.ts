import db from '../database';
import { Favorite } from '../types';
import { paginate } from '../utils';

export async function addFavorite(userId: number, listingId: number): Promise<Favorite> {
    const listing = await (await db.prepare('SELECT id, status FROM listings WHERE id = ?')).get(listingId) as any;
    if (!listing) throw new Error('Listing not found');
    if (listing.status !== 'active') throw new Error('Cannot favorite inactive listings');

    try {
        const result = await (await db.prepare(`INSERT INTO favorites (user_id, listing_id) VALUES (?, ?)`)).run(userId, listingId);
        return await (await db.prepare('SELECT * FROM favorites WHERE id = ?')).get(result.lastInsertRowid) as Favorite;
    } catch (error: any) {
        if (error.code === '23505') { // PostgreSQL unique violation
            throw new Error('Listing already in favorites');
        }
        throw error;
    }
}

export async function removeFavorite(userId: number, listingId: number): Promise<void> {
    const result = await (await db.prepare(`DELETE FROM favorites WHERE user_id = ? AND listing_id = ?`)).run(userId, listingId);
    if (result.changes === 0) throw new Error('Favorite not found');
}

export async function getFavorites(userId: number, page = 1, limit = 20): Promise<any> {
    const countRow = await (await db.prepare('SELECT COUNT(*) as total FROM favorites WHERE user_id = ?')).get(userId) as { total: number };
    const total = Number(countRow?.total ?? 0);

    const favorites = await (await db.prepare(`
        SELECT f.*, l.crop_type, l.variety, l.quantity, l.unit, l.min_price, l.max_price, l.status,
          up.full_name as farmer_name,
          (SELECT photo_url FROM listing_photos WHERE listing_id = l.id AND is_primary = 1 LIMIT 1) as primary_photo
        FROM favorites f
        JOIN listings l ON f.listing_id = l.id
        JOIN user_profiles up ON l.farmer_id = up.user_id
        WHERE f.user_id = ?
        ORDER BY f.created_at DESC LIMIT ? OFFSET ?
    `)).all(userId, limit, (page - 1) * limit);

    return { favorites, pagination: paginate(page, limit, total) };
}

export async function isFavorite(userId: number, listingId: number): Promise<boolean> {
    const favorite = await (await db.prepare('SELECT id FROM favorites WHERE user_id = ? AND listing_id = ?')).get(userId, listingId);
    return !!favorite;
}
