import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import db from '../database';
import { User, UserProfile, AuthPayload } from '../types';
import { logAudit } from '../utils';

const JWT_SECRET = process.env.JWT_SECRET || 'your-super-secret-jwt-key';
const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '24h';

export interface RegisterData {
    email: string;
    password: string;
    role: 'farmer' | 'buyer';
    fullName: string;
    phone?: string;
}

export interface LoginData {
    email: string;
    password: string;
}

export async function registerUser(data: RegisterData, ipAddress?: string): Promise<{
    user: Omit<User, 'password_hash'>;
    profile: UserProfile;
    token: string;
}> {
    const { email, password, role, fullName, phone } = data;

    const existing = await (await (await db.prepare('SELECT id FROM users WHERE email = ?'))).get(email);
    if (existing) {
        throw new Error('Email already registered');
    }

    const salt = await bcrypt.genSalt(12);
    const passwordHash = await bcrypt.hash(password, salt);

    const userResult = await (await (await db.prepare(`
        INSERT INTO users (email, password_hash, role, status)
        VALUES (?, ?, ?, 'pending')
    `))).run(email, passwordHash, role);

    const userId = userResult.lastInsertRowid;
    console.log('Created user with ID:', userId);

    if (!userId || userId === 0) {
        throw new Error('Failed to create user - invalid ID returned');
    }

    await (await (await db.prepare(`
        INSERT INTO user_profiles (user_id, full_name, phone)
        VALUES (?, ?, ?)
    `))).run(userId, fullName, phone || null);

    const user = await (await (await db.prepare('SELECT id, email, role, status, created_at, updated_at FROM users WHERE id = ?'))).get(userId) as Omit<User, 'password_hash'> | undefined;
    console.log('Fetched user:', user);

    if (!user) {
        throw new Error('Failed to fetch created user');
    }

    const profile = await (await (await db.prepare('SELECT * FROM user_profiles WHERE user_id = ?'))).get(userId) as UserProfile;

    const payload: AuthPayload = { userId: user.id, email: user.email, role: user.role };
    const token = jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN } as jwt.SignOptions);

    logAudit(user.id, 'USER_REGISTER', 'user', user.id, null, { email, role }, ipAddress);

    return { user, profile, token };
}

export async function loginUser(data: LoginData, ipAddress?: string): Promise<{
    user: Omit<User, 'password_hash'>;
    profile: UserProfile;
    token: string;
}> {
    const { email, password } = data;

    const user = await (await (await db.prepare('SELECT * FROM users WHERE email = ?'))).get(email) as User | undefined;
    if (!user) {
        throw new Error('Invalid credentials');
    }

    if (user.status === 'suspended') {
        throw new Error('Account suspended. Please contact support.');
    }

    const isValid = await bcrypt.compare(password, user.password_hash);
    if (!isValid) {
        throw new Error('Invalid credentials');
    }

    const profile = await (await (await db.prepare('SELECT * FROM user_profiles WHERE user_id = ?'))).get(user.id) as UserProfile;

    const payload: AuthPayload = { userId: user.id, email: user.email, role: user.role };
    const token = jwt.sign(payload, JWT_SECRET, { expiresIn: JWT_EXPIRES_IN } as jwt.SignOptions);

    logAudit(user.id, 'USER_LOGIN', 'user', user.id, null, null, ipAddress);

    const { password_hash, ...safeUser } = user;
    return { user: safeUser, profile, token };
}

export async function getUserById(userId: number): Promise<{ user: Omit<User, 'password_hash'>; profile: UserProfile } | null> {
    const user = await (await (await db.prepare('SELECT id, email, role, status, created_at, updated_at FROM users WHERE id = ?'))).get(userId) as Omit<User, 'password_hash'> | undefined;
    if (!user) return null;

    const profile = await (await (await db.prepare('SELECT * FROM user_profiles WHERE user_id = ?'))).get(userId) as UserProfile;
    return { user, profile };
}

export async function updateProfile(userId: number, data: Partial<UserProfile>): Promise<UserProfile> {
    const fields: string[] = [];
    const values: any[] = [];

    const allowedFields = ['full_name', 'phone', 'address', 'city', 'state', 'pincode', 'profile_photo', 'business_name', 'business_type'];

    for (const [key, value] of Object.entries(data)) {
        const dbKey = key.replace(/([A-Z])/g, '_$1').toLowerCase();
        if (allowedFields.includes(dbKey)) {
            fields.push(`${dbKey} = ?`);
            values.push(value);
        }
    }

    if (fields.length === 0) {
        throw new Error('No valid fields to update');
    }

    fields.push('updated_at = CURRENT_TIMESTAMP');
    values.push(userId);

    await (await (await db.prepare(`UPDATE user_profiles SET ${fields.join(', ')} WHERE user_id = ?`))).run(...values);

    return await (await (await db.prepare('SELECT * FROM user_profiles WHERE user_id = ?'))).get(userId) as UserProfile;
}

export async function updateFcmToken(userId: number, token: string): Promise<void> {
    await (await (await db.prepare('UPDATE users SET fcm_token = ? WHERE id = ?'))).run(token, userId);
}

export async function changePassword(userId: number, currentPassword: string, newPassword: string): Promise<void> {
    const user = await (await (await db.prepare('SELECT password_hash FROM users WHERE id = ?'))).get(userId) as { password_hash: string } | undefined;
    if (!user) {
        throw new Error('User not found');
    }

    const isValid = await bcrypt.compare(currentPassword, user.password_hash);
    if (!isValid) {
        throw new Error('Current password is incorrect');
    }

    const salt = await bcrypt.genSalt(12);
    const newHash = await bcrypt.hash(newPassword, salt);

    await (await (await db.prepare('UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?'))).run(newHash, userId);

    logAudit(userId, 'PASSWORD_CHANGE', 'user', userId, null, null);
}
