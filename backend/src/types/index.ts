// User types
export interface User {
    id: number;
    email: string;
    password_hash: string;
    role: 'farmer' | 'buyer' | 'admin';
    status: 'pending' | 'verified' | 'suspended';
    created_at: string;
    updated_at: string;
}

export interface UserProfile {
    id: number;
    user_id: number;
    full_name: string;
    phone?: string;
    address?: string;
    city?: string;
    state?: string;
    pincode?: string;
    profile_photo?: string;
    business_name?: string;
    business_type?: string;
    created_at: string;
    updated_at: string;
}

// Listing types
export type ListingStatus = 'draft' | 'active' | 'inactive' | 'paused' | 'closed' | 'sold';
export type QualityGrade = 'A' | 'B' | 'C' | 'Premium';
export type Unit = 'kg' | 'ton' | 'quintal';

export interface Listing {
    id: number;
    farmer_id: number;
    crop_type: string;
    variety?: string;
    quantity: number;
    unit: Unit;
    quality_grade?: QualityGrade;
    min_price: number;
    max_price: number;
    harvest_start_date: string;
    harvest_end_date: string;
    latitude?: number;
    longitude?: number;
    location_address?: string;
    description?: string;
    status: ListingStatus;
    created_at: string;
    updated_at: string;
    changed_by?: number;
}

export interface ListingPhoto {
    id: number;
    listing_id: number;
    photo_url: string;
    is_primary: boolean;
    created_at: string;
}

export interface Favorite {
    id: number;
    user_id: number;
    listing_id: number;
    created_at: string;
}

// Negotiation types
export type NegotiationStatus = 'open' | 'accepted' | 'rejected' | 'expired' | 'cancelled';
export type MessageType = 'text' | 'proposal' | 'counter_proposal' | 'accept' | 'reject';

export interface Negotiation {
    id: number;
    listing_id: number;
    buyer_id: number;
    farmer_id: number;
    proposed_price: number;
    proposed_quantity: number;
    status: NegotiationStatus;
    expires_at?: string;
    created_at: string;
    updated_at: string;
}

export interface NegotiationMessage {
    id: number;
    negotiation_id: number;
    sender_id: number;
    message_type: MessageType;
    message?: string;
    proposed_price?: number;
    proposed_quantity?: number;
    is_immutable: boolean;
    created_at: string;
}

// Contract types
export type ContractStatus = 'pending' | 'active' | 'in_progress' | 'completed' | 'cancelled' | 'disputed';

export interface Contract {
    id: number;
    contract_number: string;
    negotiation_id: number;
    listing_id: number;
    farmer_id: number;
    buyer_id: number;
    crop_type: string;
    variety?: string;
    quantity: number;
    unit: string;
    agreed_price: number;
    total_value: number;
    delivery_address?: string;
    delivery_date?: string;
    terms_and_conditions?: string;
    farmer_confirmed: boolean;
    farmer_confirmed_at?: string;
    buyer_confirmed: boolean;
    buyer_confirmed_at?: string;
    status: ContractStatus;
    created_at: string;
    updated_at: string;
    changed_by?: number;
}

// Fulfillment types
export type MilestoneType = 'scheduled' | 'dispatched' | 'delivered' | 'completed';
export type MilestoneStatus = 'pending' | 'in_progress' | 'completed' | 'skipped';

export interface FulfillmentMilestone {
    id: number;
    contract_id: number;
    milestone_type: MilestoneType;
    status: MilestoneStatus;
    scheduled_date?: string;
    completed_date?: string;
    notes?: string;
    proof_url?: string;
    proof_type?: 'image' | 'pdf';
    updated_by?: number;
    created_at: string;
    updated_at: string;
}

// Payment types
export type PaymentMethod = 'cash' | 'upi' | 'bank_transfer';
export type PaymentStatus = 'pending' | 'partial' | 'paid' | 'failed';

export interface Payment {
    id: number;
    contract_id: number;
    amount: number;
    payment_method: PaymentMethod;
    payment_status: PaymentStatus;
    transaction_id?: string;
    receipt_url?: string;
    payment_date?: string;
    notes?: string;
    recorded_by?: number;
    created_at: string;
    updated_at: string;
}

// Dispute types
export type DisputeStatus = 'open' | 'under_review' | 'resolved' | 'closed';

export interface Dispute {
    id: number;
    contract_id: number;
    raised_by: number;
    reason: string;
    description: string;
    status: DisputeStatus;
    resolution_notes?: string;
    resolved_by?: number;
    resolved_at?: string;
    created_at: string;
    updated_at: string;
}

export interface DisputeEvidence {
    id: number;
    dispute_id: number;
    uploaded_by: number;
    file_url: string;
    file_type: 'image' | 'pdf' | 'document';
    description?: string;
    created_at: string;
}

// Audit types
export interface AuditLog {
    id: number;
    user_id?: number;
    action: string;
    entity_type: string;
    entity_id?: number;
    old_values?: string;
    new_values?: string;
    ip_address?: string;
    user_agent?: string;
    created_at: string;
}

export interface AdminAction {
    id: number;
    admin_id: number;
    action_type: string;
    target_type: string;
    target_id: number;
    reason?: string;
    details?: string;
    created_at: string;
}

// API types
export interface ApiResponse<T = any> {
    success: boolean;
    message?: string;
    data?: T;
    error?: string;
}

export interface PaginatedResponse<T> {
    items: T[];
    pagination: {
        page: number;
        limit: number;
        total: number;
        pages: number;
    };
}

export interface AuthPayload {
    userId: number;
    email: string;
    role: 'farmer' | 'buyer' | 'admin';
}
