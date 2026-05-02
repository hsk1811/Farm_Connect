import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import rateLimit from 'express-rate-limit';
import path from 'path';
import fs from 'fs';

import { initDatabase, initializeDatabase } from './database';
import { errorHandler, notFound } from './middleware';
import {
    authRoutes,
    listingRoutes,
    negotiationRoutes,
    contractRoutes,
    disputeRoutes,
    buyerRoutes,
    adminRoutes,
    uploadRoutes,
    notificationRoutes
} from './routes';

// Load environment variables
const PORT = process.env.PORT || 3000;

// Initialize Express app
const app = express();

// Storage is handled natively by Supabase. No local dirs needed.


// Security middleware
app.use(helmet());
app.use(cors({
    origin: '*', // Configure for production
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE'],
    allowedHeaders: ['Content-Type', 'Authorization']
}));

// Rate limiting
const limiter = rateLimit({
    windowMs: 60 * 1000, // 1 minute
    max: 100, // 100 requests per minute
    message: { success: false, error: 'Too many requests, please try again later' }
});
app.use(limiter);

// Body parsing
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Files are now served directly from Supabase Storage instead of the local filesystem

// Health check
app.get('/api/health', (req, res) => {
    res.json({ success: true, message: 'FarmConnect API is running', timestamp: new Date().toISOString() });
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/listings', listingRoutes);
app.use('/api/negotiations', negotiationRoutes);
app.use('/api/contracts', contractRoutes);
app.use('/api/disputes', disputeRoutes);
app.use('/api/buyer', buyerRoutes);
app.use('/api/admin', adminRoutes);
app.use('/api/upload', uploadRoutes);
app.use('/api/notifications', notificationRoutes);

// Error handling
app.use(notFound);
app.use(errorHandler);

// Initialize database and start server
async function startServer() {
    try {
        // Connect to Supabase PostgreSQL and create schema if needed
        await initDatabase();
        await initializeDatabase();

        console.log('Database initialized');

        // Start server
        app.listen(PORT, () => {
            console.log(`
╔═══════════════════════════════════════════════════════════╗
║                                                           ║
║   🌾 FarmConnect API Server                               ║
║   Contract Farming System Backend                         ║
║                                                           ║
║   Server running on: http://localhost:${PORT}               ║
║                                                           ║
║   Endpoints:                                              ║
║   • POST /api/auth/register - Register new user           ║
║   • POST /api/auth/login - User login                     ║
║   • GET  /api/listings - Browse listings                  ║
║   • POST /api/negotiations - Start negotiation            ║
║   • GET  /api/contracts - View contracts                  ║
║   • POST /api/disputes - Raise dispute                    ║
║   • GET  /api/admin/stats - Admin dashboard               ║
║                                                           ║
╚═══════════════════════════════════════════════════════════╝
      `);
        });
    } catch (error) {
        console.error('Failed to start server:', error);
        process.exit(1);
    }
}

// Only start the server locally. Vercel will import the app directly.
if (process.env.NODE_ENV !== 'production' && !process.env.VERCEL) {
    startServer();
}

export default app;
