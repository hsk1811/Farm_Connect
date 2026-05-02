import { getDatabase } from './connection';

export async function initializeDatabase() {
  const db = getDatabase();

  // Users table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS users (
            id SERIAL PRIMARY KEY,
            email VARCHAR(255) UNIQUE NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            role TEXT CHECK(role IN ('farmer', 'buyer', 'admin')) NOT NULL,
            status TEXT CHECK(status IN ('pending', 'verified', 'suspended')) DEFAULT 'pending',
            fcm_token TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    `);

  // User profiles table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS user_profiles (
            id SERIAL PRIMARY KEY,
            user_id INTEGER UNIQUE NOT NULL,
            full_name VARCHAR(255) NOT NULL,
            phone VARCHAR(20),
            address TEXT,
            city VARCHAR(100),
            state VARCHAR(100),
            pincode VARCHAR(10),
            profile_photo VARCHAR(500),
            business_name VARCHAR(255),
            business_type VARCHAR(100),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
    `);

  // Listings table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS listings (
            id SERIAL PRIMARY KEY,
            farmer_id INTEGER NOT NULL,
            crop_type VARCHAR(100) NOT NULL,
            variety VARCHAR(100),
            quantity DECIMAL(10,2) NOT NULL,
            unit TEXT CHECK(unit IN ('kg', 'ton', 'quintal')) NOT NULL,
            quality_grade TEXT CHECK(quality_grade IN ('A', 'B', 'C', 'Premium')),
            min_price DECIMAL(10,2) NOT NULL,
            max_price DECIMAL(10,2) NOT NULL,
            harvest_start_date DATE NOT NULL,
            harvest_end_date DATE NOT NULL,
            latitude DECIMAL(10,8),
            longitude DECIMAL(11,8),
            location_address TEXT,
            description TEXT,
            status TEXT CHECK(status IN ('draft', 'active', 'inactive', 'paused', 'closed', 'sold')) DEFAULT 'draft',
            changed_by INTEGER,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (farmer_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (changed_by) REFERENCES users(id)
        )
    `);

  // Listing photos table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS listing_photos (
            id SERIAL PRIMARY KEY,
            listing_id INTEGER NOT NULL,
            photo_url VARCHAR(500) NOT NULL,
            is_primary INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE
        )
    `);

  // Favorites table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS favorites (
            id SERIAL PRIMARY KEY,
            user_id INTEGER NOT NULL,
            listing_id INTEGER NOT NULL,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
            UNIQUE(user_id, listing_id)
        )
    `);

  // Negotiations table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS negotiations (
            id SERIAL PRIMARY KEY,
            listing_id INTEGER NOT NULL,
            buyer_id INTEGER NOT NULL,
            farmer_id INTEGER NOT NULL,
            proposed_price DECIMAL(10,2) NOT NULL,
            proposed_quantity DECIMAL(10,2) NOT NULL,
            status TEXT CHECK(status IN ('open', 'accepted', 'rejected', 'expired', 'cancelled')) DEFAULT 'open',
            expires_at TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (listing_id) REFERENCES listings(id) ON DELETE CASCADE,
            FOREIGN KEY (buyer_id) REFERENCES users(id) ON DELETE CASCADE,
            FOREIGN KEY (farmer_id) REFERENCES users(id) ON DELETE CASCADE
        )
    `);

  // Negotiation messages table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS negotiation_messages (
            id SERIAL PRIMARY KEY,
            negotiation_id INTEGER NOT NULL,
            sender_id INTEGER NOT NULL,
            message_type TEXT CHECK(message_type IN ('text', 'proposal', 'counter_proposal', 'accept', 'reject')) NOT NULL,
            message TEXT,
            proposed_price DECIMAL(10,2),
            proposed_quantity DECIMAL(10,2),
            is_immutable INTEGER DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (negotiation_id) REFERENCES negotiations(id) ON DELETE CASCADE,
            FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
        )
    `);

  // Contracts table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS contracts (
            id SERIAL PRIMARY KEY,
            contract_number VARCHAR(50) UNIQUE NOT NULL,
            negotiation_id INTEGER NOT NULL,
            listing_id INTEGER NOT NULL,
            farmer_id INTEGER NOT NULL,
            buyer_id INTEGER NOT NULL,
            crop_type VARCHAR(100) NOT NULL,
            variety VARCHAR(100),
            quantity DECIMAL(10,2) NOT NULL,
            unit VARCHAR(20) NOT NULL,
            agreed_price DECIMAL(10,2) NOT NULL,
            total_value DECIMAL(12,2) NOT NULL,
            delivery_address TEXT,
            delivery_date DATE,
            quality_grade TEXT,
            payment_terms TEXT,
            transport_responsibility TEXT,
            additional_terms TEXT,
            terms_and_conditions TEXT,
            farmer_confirmed INTEGER DEFAULT 0,
            farmer_confirmed_at TIMESTAMP,
            buyer_confirmed INTEGER DEFAULT 0,
            buyer_confirmed_at TIMESTAMP,
            status TEXT CHECK(status IN ('pending', 'active', 'in_progress', 'completed', 'cancelled', 'disputed')) DEFAULT 'pending',
            contract_hash VARCHAR(64),
            pdf_path VARCHAR(500),
            changed_by INTEGER,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (negotiation_id) REFERENCES negotiations(id),
            FOREIGN KEY (listing_id) REFERENCES listings(id),
            FOREIGN KEY (farmer_id) REFERENCES users(id),
            FOREIGN KEY (buyer_id) REFERENCES users(id),
            FOREIGN KEY (changed_by) REFERENCES users(id)
        )
    `);

  // Fulfillment milestones table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS fulfillment_milestones (
            id SERIAL PRIMARY KEY,
            contract_id INTEGER NOT NULL,
            milestone_type TEXT CHECK(milestone_type IN ('scheduled', 'dispatched', 'delivered', 'completed')) NOT NULL,
            status TEXT CHECK(status IN ('pending', 'in_progress', 'completed', 'skipped')) DEFAULT 'pending',
            scheduled_date DATE,
            completed_date DATE,
            notes TEXT,
            proof_url VARCHAR(500),
            proof_type TEXT CHECK(proof_type IN ('image', 'pdf')),
            updated_by INTEGER,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
            FOREIGN KEY (updated_by) REFERENCES users(id)
        )
    `);

  // Payments table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS payments (
            id SERIAL PRIMARY KEY,
            contract_id INTEGER NOT NULL,
            amount DECIMAL(12,2) NOT NULL,
            payment_method TEXT CHECK(payment_method IN ('cash', 'upi', 'bank_transfer')) NOT NULL,
            payment_status TEXT CHECK(payment_status IN ('pending', 'partial', 'paid', 'failed')) DEFAULT 'pending',
            transaction_id VARCHAR(100),
            receipt_url VARCHAR(500),
            payment_date TIMESTAMP,
            notes TEXT,
            recorded_by INTEGER,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
            FOREIGN KEY (recorded_by) REFERENCES users(id)
        )
    `);

  // Disputes table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS disputes (
            id SERIAL PRIMARY KEY,
            contract_id INTEGER NOT NULL,
            raised_by INTEGER NOT NULL,
            reason VARCHAR(255) NOT NULL,
            description TEXT NOT NULL,
            status TEXT CHECK(status IN ('open', 'under_review', 'resolved', 'closed')) DEFAULT 'open',
            resolution_notes TEXT,
            resolved_by INTEGER,
            resolved_at TIMESTAMP,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE CASCADE,
            FOREIGN KEY (raised_by) REFERENCES users(id),
            FOREIGN KEY (resolved_by) REFERENCES users(id)
        )
    `);

  // Dispute evidence table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS dispute_evidence (
            id SERIAL PRIMARY KEY,
            dispute_id INTEGER NOT NULL,
            uploaded_by INTEGER NOT NULL,
            file_url VARCHAR(500) NOT NULL,
            file_type TEXT CHECK(file_type IN ('image', 'pdf', 'document')) NOT NULL,
            description TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (dispute_id) REFERENCES disputes(id) ON DELETE CASCADE,
            FOREIGN KEY (uploaded_by) REFERENCES users(id)
        )
    `);

  // Audit logs table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS audit_logs (
            id SERIAL PRIMARY KEY,
            user_id INTEGER,
            action VARCHAR(100) NOT NULL,
            entity_type VARCHAR(50) NOT NULL,
            entity_id INTEGER,
            old_values TEXT,
            new_values TEXT,
            ip_address VARCHAR(45),
            user_agent TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id)
        )
    `);

  // Admin actions table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS admin_actions (
            id SERIAL PRIMARY KEY,
            admin_id INTEGER NOT NULL,
            action_type VARCHAR(50) NOT NULL,
            target_type VARCHAR(50) NOT NULL,
            target_id INTEGER NOT NULL,
            reason TEXT,
            details TEXT,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (admin_id) REFERENCES users(id)
        )
    `);

  // Notifications table
  await db.exec(`
        CREATE TABLE IF NOT EXISTS notifications (
            id SERIAL PRIMARY KEY,
            user_id INTEGER NOT NULL,
            type TEXT CHECK(type IN ('negotiation', 'contract', 'payment', 'general')) NOT NULL,
            title VARCHAR(255) NOT NULL,
            message TEXT NOT NULL,
            data TEXT,
            is_read INTEGER DEFAULT 0,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
        )
    `);

  // Indexes
  await db.exec(`
        CREATE INDEX IF NOT EXISTS idx_listings_farmer ON listings(farmer_id);
        CREATE INDEX IF NOT EXISTS idx_listings_status ON listings(status);
        CREATE INDEX IF NOT EXISTS idx_listings_crop ON listings(crop_type);
        CREATE INDEX IF NOT EXISTS idx_negotiations_buyer ON negotiations(buyer_id);
        CREATE INDEX IF NOT EXISTS idx_negotiations_farmer ON negotiations(farmer_id);
        CREATE INDEX IF NOT EXISTS idx_negotiations_status ON negotiations(status);
        CREATE INDEX IF NOT EXISTS idx_contracts_farmer ON contracts(farmer_id);
        CREATE INDEX IF NOT EXISTS idx_contracts_buyer ON contracts(buyer_id);
        CREATE INDEX IF NOT EXISTS idx_contracts_status ON contracts(status);
        CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id);
        CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
        CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
        CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(is_read);
    `);

  console.log('✅ Database schema initialized on Supabase');
}
