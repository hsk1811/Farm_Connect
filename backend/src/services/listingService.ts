import db from '../database';
import { Listing, ListingPhoto, ListingStatus } from '../types';
import { logAudit, paginate } from '../utils';

export interface CreateListingData {
    cropType: string;
    variety?: string;
    quantity: number;
    unit: 'kg' | 'ton' | 'quintal';
    qualityGrade?: 'A' | 'B' | 'C' | 'Premium';
    minPrice: number;
    maxPrice: number;
    harvestStartDate: string;
    harvestEndDate: string;
    latitude?: number;
    longitude?: number;
    locationAddress?: string;
    description?: string;
    photos?: string[];
}

export interface ListingFilters {
    cropType?: string;
    variety?: string;
    minPrice?: number;
    maxPrice?: number;
    minQuantity?: number;
    maxQuantity?: number;
    qualityGrade?: string;
    city?: string;
    state?: string;
    harvestStartAfter?: string;
    harvestEndBefore?: string;
    status?: ListingStatus;
    farmerId?: number;
    latitude?: number;
    longitude?: number;
    radius?: number;
    sortBy?: 'price' | 'quantity' | 'date';
    sortOrder?: 'asc' | 'desc';
    page?: number;
    limit?: number;
}

export async function createListing(farmerId: number, data: CreateListingData): Promise<Listing> {
    const result = await (await (await db.prepare(`
        INSERT INTO listings (
          farmer_id, crop_type, variety, quantity, unit, quality_grade,
          min_price, max_price, harvest_start_date, harvest_end_date,
          latitude, longitude, location_address, description, status, changed_by
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'active', ?)
    `))).run(
        farmerId, data.cropType, data.variety || null, data.quantity, data.unit, data.qualityGrade || null,
        data.minPrice, data.maxPrice, data.harvestStartDate, data.harvestEndDate,
        data.latitude || null, data.longitude || null, data.locationAddress || null, data.description || null, farmerId
    );

    const listingId = result.lastInsertRowid;

    if (data.photos && data.photos.length > 0) {
        for (let i = 0; i < data.photos.length; i++) {
            await (await db.prepare(`INSERT INTO listing_photos (listing_id, photo_url, is_primary) VALUES (?, ?, ?)`)).run(listingId, data.photos[i], i === 0 ? 1 : 0);
        }
    }

    const newListing = await (await (await db.prepare('SELECT * FROM listings WHERE id = ?'))).get(listingId) as Listing;
    logAudit(farmerId, 'LISTING_CREATE', 'listing', newListing.id, null, newListing);

    return newListing;
}

export async function updateListing(listingId: number, userId: number, data: Partial<CreateListingData>): Promise<Listing> {
    const existing = await (await (await db.prepare('SELECT status, farmer_id FROM listings WHERE id = ?'))).get(listingId) as Listing;
    if (!existing) throw new Error('Listing not found');
    if (existing.farmer_id !== userId) throw new Error('Unauthorized');
    if (existing.status !== 'active' && existing.status !== 'paused') {
        throw new Error('Can only update active or paused listings');
    }

    const fieldMap: Record<string, string> = {
        cropType: 'crop_type', variety: 'variety', quantity: 'quantity', unit: 'unit',
        qualityGrade: 'quality_grade', minPrice: 'min_price', maxPrice: 'max_price',
        harvestStartDate: 'harvest_start_date', harvestEndDate: 'harvest_end_date',
        latitude: 'latitude', longitude: 'longitude', locationAddress: 'location_address', description: 'description'
    };

    const updates: string[] = [];
    const values: any[] = [];

    for (const [key, value] of Object.entries(data)) {
        if (fieldMap[key]) { updates.push(`${fieldMap[key]} = ?`); values.push(value); }
    }

    if (updates.length === 0) return existing;

    updates.push('updated_at = CURRENT_TIMESTAMP', 'changed_by = ?');
    values.push(userId, listingId);

    await (await (await db.prepare(`UPDATE listings SET ${updates.join(', ')} WHERE id = ?`))).run(...values);

    const updated = await getListingById(listingId);
    logAudit(userId, 'LISTING_UPDATE', 'listing', listingId, data, updated || undefined);
    return updated!;
}

export async function updateListingStatus(listingId: number, userId: number, status: ListingStatus): Promise<Listing> {
    const existing = await (await (await db.prepare('SELECT * FROM listings WHERE id = ?'))).get(listingId) as Listing;
    if (!existing) throw new Error('Listing not found');
    if (existing.farmer_id !== userId) throw new Error('Not authorized to update this listing');

    const validTransitions: Record<ListingStatus, ListingStatus[]> = {
        draft: ['active'],
        active: ['inactive', 'paused', 'closed', 'sold'],
        inactive: ['active', 'closed'],
        paused: ['active', 'closed'],
        closed: [],
        sold: ['active']
    };

    if (!validTransitions[existing.status]?.includes(status)) {
        throw new Error(`Cannot transition from ${existing.status} to ${status}`);
    }

    await (await (await db.prepare(`UPDATE listings SET status = ?, updated_at = CURRENT_TIMESTAMP, changed_by = ? WHERE id = ?`))).run(status, userId, listingId);

    const updated = await (await (await db.prepare('SELECT * FROM listings WHERE id = ?'))).get(listingId) as Listing;
    logAudit(userId, 'LISTING_STATUS_CHANGE', 'listing', listingId, { status: existing.status }, { status });

    return updated;
}

export async function deleteListing(listingId: number, userId: number): Promise<void> {
    const listing = await (await (await db.prepare('SELECT * FROM listings WHERE id = ?'))).get(listingId) as Listing;
    if (!listing) throw new Error('Listing not found');
    if (listing.farmer_id !== userId) throw new Error('Not authorized to delete this listing');

    const activeNeg = await (await (await db.prepare(`SELECT COUNT(*) as count FROM negotiations WHERE listing_id = ? AND status IN ('pending', 'accepted')`))).get(listingId) as { count: number };
    if (Number(activeNeg?.count) > 0) throw new Error('Cannot delete listing with active negotiations');

    const contracts = await (await (await db.prepare(`SELECT COUNT(*) as count FROM contracts WHERE listing_id = ?`))).get(listingId) as { count: number };
    if (Number(contracts?.count) > 0) throw new Error('Cannot delete listing with existing contracts');

    await (await (await db.prepare(`UPDATE listings SET status = 'closed', updated_at = CURRENT_TIMESTAMP, changed_by = ? WHERE id = ?`))).run(userId, listingId);
    logAudit(userId, 'LISTING_DELETE', 'listing', listingId, listing, { status: 'closed' });
}

export async function getListingById(listingId: number): Promise<(Listing & { photos: ListingPhoto[]; farmer: any }) | null> {
    const listing = await (await (await db.prepare('SELECT * FROM listings WHERE id = ?'))).get(listingId) as Listing | undefined;
    if (!listing) return null;

    const photosResult = await (await (await db.prepare('SELECT * FROM listing_photos WHERE listing_id = ?'))).all(listingId);
    const photos = (photosResult as any[]).map(p => ({ ...p, is_primary: Boolean(p.is_primary) })) as ListingPhoto[];

    const farmer = await (await (await db.prepare(`
        SELECT u.id, up.full_name, up.phone, up.city, up.state
        FROM users u JOIN user_profiles up ON u.id = up.user_id WHERE u.id = ?
    `))).get(listing.farmer_id);

    return { ...listing, photos, farmer };
}

export async function getListings(filters: ListingFilters): Promise<{ listings: any[]; pagination: any }> {
    const {
        cropType, variety, minPrice, maxPrice, minQuantity, maxQuantity,
        qualityGrade, city, state, harvestStartAfter, harvestEndBefore,
        status, farmerId, sortBy = 'date', sortOrder = 'desc', page = 1, limit = 20
    } = filters;

    console.log('🔍 Received filters:', JSON.stringify(filters, null, 2));

    let query = `
        SELECT l.*, up.full_name as farmer_name, up.city, up.state,
          (SELECT photo_url FROM listing_photos WHERE listing_id = l.id AND is_primary = 1 LIMIT 1) as primary_photo
        FROM listings l
        JOIN users u ON l.farmer_id = u.id
        JOIN user_profiles up ON u.id = up.user_id
        WHERE 1=1
    `;
    let countQuery = `
        SELECT COUNT(*) as total FROM listings l
        JOIN users u ON l.farmer_id = u.id
        JOIN user_profiles up ON u.id = up.user_id WHERE 1=1
    `;
    const params: any[] = [];

    if (cropType) { query += ' AND LOWER(l.crop_type) LIKE LOWER(?)'; countQuery += ' AND LOWER(l.crop_type) LIKE LOWER(?)'; params.push(`%${cropType}%`); }
    if (variety) { query += ' AND LOWER(l.variety) LIKE LOWER(?)'; countQuery += ' AND LOWER(l.variety) LIKE LOWER(?)'; params.push(`%${variety}%`); }
    if (minPrice !== undefined) { query += ' AND l.min_price >= ?'; countQuery += ' AND l.min_price >= ?'; params.push(minPrice); }
    if (maxPrice !== undefined) { query += ' AND l.max_price <= ?'; countQuery += ' AND l.max_price <= ?'; params.push(maxPrice); }
    if (minQuantity !== undefined) { query += ' AND l.quantity >= ?'; countQuery += ' AND l.quantity >= ?'; params.push(minQuantity); }
    if (maxQuantity !== undefined) { query += ' AND l.quantity <= ?'; countQuery += ' AND l.quantity <= ?'; params.push(maxQuantity); }
    if (qualityGrade) { query += ' AND l.quality_grade = ?'; countQuery += ' AND l.quality_grade = ?'; params.push(qualityGrade); }
    if (city) { query += ' AND LOWER(up.city) LIKE LOWER(?)'; countQuery += ' AND LOWER(up.city) LIKE LOWER(?)'; params.push(`%${city}%`); }
    if (state) { query += ' AND LOWER(up.state) LIKE LOWER(?)'; countQuery += ' AND LOWER(up.state) LIKE LOWER(?)'; params.push(`%${state}%`); }
    if (harvestStartAfter) { query += ' AND l.harvest_start_date >= ?'; countQuery += ' AND l.harvest_start_date >= ?'; params.push(harvestStartAfter); }
    if (harvestEndBefore) { query += ' AND l.harvest_end_date <= ?'; countQuery += ' AND l.harvest_end_date <= ?'; params.push(harvestEndBefore); }

    if (status) {
        query += ' AND l.status = ?'; countQuery += ' AND l.status = ?'; params.push(status);
    } else if (!farmerId) {
        query += " AND l.status = 'active'"; countQuery += " AND l.status = 'active'";
    }

    if (farmerId) { query += ' AND l.farmer_id = ?'; countQuery += ' AND l.farmer_id = ?'; params.push(farmerId); }

    const countRow = await (await (await db.prepare(countQuery))).get(...params) as { total: number };
    const total = Number(countRow?.total ?? 0);

    let orderBy = 'l.created_at DESC';
    if (sortBy === 'price') orderBy = sortOrder === 'asc' ? 'l.min_price ASC' : 'l.max_price DESC';
    else if (sortBy === 'quantity') orderBy = `l.quantity ${sortOrder.toUpperCase()}`;
    else if (sortBy === 'date') orderBy = `l.created_at ${sortOrder.toUpperCase()}`;

    query += ` ORDER BY ${orderBy} LIMIT ? OFFSET ?`;
    const listings = await (await (await db.prepare(query))).all(...params, limit, (page - 1) * limit);

    console.log('✅ Found', listings.length, 'listings out of', total, 'total');

    return { listings, pagination: paginate(page, limit, total) };
}

export async function addListingPhoto(listingId: number, photoUrl: string, isPrimary: boolean = false): Promise<ListingPhoto> {
    if (isPrimary) {
        await (await (await db.prepare('UPDATE listing_photos SET is_primary = 0 WHERE listing_id = ?'))).run(listingId);
    }

    const result = await (await (await db.prepare(`INSERT INTO listing_photos (listing_id, photo_url, is_primary) VALUES (?, ?, ?)`))).run(listingId, photoUrl, isPrimary ? 1 : 0);

    return await (await (await db.prepare('SELECT * FROM listing_photos WHERE id = ?'))).get(result.lastInsertRowid) as ListingPhoto;
}

export async function deleteListingPhoto(photoId: number, userId: number): Promise<void> {
    const photo = await (await (await db.prepare(`
        SELECT lp.*, l.farmer_id FROM listing_photos lp JOIN listings l ON lp.listing_id = l.id WHERE lp.id = ?
    `))).get(photoId) as (ListingPhoto & { farmer_id: number }) | undefined;

    if (!photo) throw new Error('Photo not found');
    if (photo.farmer_id !== userId) throw new Error('Not authorized to delete this photo');

    await (await (await db.prepare('DELETE FROM listing_photos WHERE id = ?'))).run(photoId);
}
