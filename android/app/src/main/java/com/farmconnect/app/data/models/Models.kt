package com.farmconnect.app.data.models

import com.google.gson.annotations.SerializedName

// ===== Authentication Models =====
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val email: String,
    val password: String,
    val role: String,
    val fullName: String,
    val phone: String? = null
)

data class UpdateProfileRequest(
    val fullName: String,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null
)

data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

data class FcmTokenRequest(
    val token: String
)

data class AuthResponse(
    val success: Boolean,
    val message: String? = null,
    val data: AuthData? = null,
    val error: String? = null
)

data class AuthData(
    val userId: Int,
    val email: String,
    val role: String,
    val status: String? = null,
    val token: String,
    val expiresIn: Long? = null,
    val profile: UserProfile? = null
)

data class UserProfile(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("full_name") val fullName: String,
    val phone: String? = null,
    val address: String? = null,
    val city: String? = null,
    val state: String? = null,
    val pincode: String? = null,
    @SerializedName("profile_photo") val profilePhoto: String? = null,
    @SerializedName("business_name") val businessName: String? = null,
    @SerializedName("business_type") val businessType: String? = null
)

data class UploadResponse(
    val success: Boolean,
    val message: String,
    val urls: List<String>
)

// ===== Listing Models =====
data class Listing(
    val id: Int,
    @SerializedName("farmer_id") val farmerId: Int,
    @SerializedName("crop_type") val cropType: String,
    val variety: String? = null,
    val quantity: Double,
    val unit: String,
    @SerializedName("quality_grade") val qualityGrade: String? = null,
    @SerializedName("min_price") val minPrice: Double,
    @SerializedName("max_price") val maxPrice: Double,
    @SerializedName("harvest_start_date") val harvestStartDate: String,
    @SerializedName("harvest_end_date") val harvestEndDate: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("location_address") val locationAddress: String? = null,
    val description: String? = null,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("farmer_name") val farmerName: String? = null,
    @SerializedName("primary_photo") val primaryPhoto: String? = null,
    val photos: List<ListingPhoto>? = null,
    val farmer: FarmerInfo? = null
)

data class ListingPhoto(
    val id: Int,
    @SerializedName("listing_id") val listingId: Int,
    @SerializedName("photo_url") val photoUrl: String,
    @SerializedName("is_primary") val isPrimary: Boolean
)

data class FarmerInfo(
    val id: Int,
    @SerializedName("full_name") val fullName: String,
    val phone: String? = null,
    val city: String? = null,
    val state: String? = null
)

data class CreateListingRequest(
    val cropType: String,
    val variety: String? = null,
    val quantity: Double,
    val unit: String,
    val qualityGrade: String? = null,
    val minPrice: Double,
    val maxPrice: Double,
    val harvestStartDate: String,
    val harvestEndDate: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationAddress: String? = null,
    val description: String? = null,
    val photos: List<String> = emptyList()
)

data class UpdateStatusRequest(
    val status: String
)

// ===== Negotiation Models =====
data class Negotiation(
    val id: Int,
    @SerializedName("listing_id") val listingId: Int,
    @SerializedName("buyer_id") val buyerId: Int,
    @SerializedName("farmer_id") val farmerId: Int,
    @SerializedName("proposed_price") val proposedPrice: Double,
    @SerializedName("proposed_quantity") val proposedQuantity: Double,
    val status: String,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("crop_type") val cropType: String? = null,
    val variety: String? = null,
    @SerializedName("buyer_name") val buyerName: String? = null,
    @SerializedName("farmer_name") val farmerName: String? = null,
    val messages: List<NegotiationMessage>? = null
)

data class NegotiationMessage(
    val id: Int,
    @SerializedName("negotiation_id") val negotiationId: Int,
    @SerializedName("sender_id") val senderId: Int,
    @SerializedName("message_type") val messageType: String,
    val message: String? = null,
    @SerializedName("proposed_price") val proposedPrice: Double? = null,
    @SerializedName("proposed_quantity") val proposedQuantity: Double? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("sender_name") val senderName: String? = null
)

data class CreateNegotiationRequest(
    val listingId: Int,
    val proposedPrice: Double,
    val proposedQuantity: Double,
    val message: String? = null
)

data class SendMessageRequest(
    val messageType: String,
    val message: String? = null,
    val proposedPrice: Double? = null,
    val proposedQuantity: Double? = null
)

data class AcceptNegotiationRequest(
    val finalPrice: Double,
    val finalQuantity: Double,
    val qualityGrade: String? = null,
    val paymentTerms: String? = null,
    val transportResponsibility: String? = null,
    val additionalTerms: String? = null
)

// ===== Contract Models =====
data class Contract(
    val id: Int,
    @SerializedName("contract_number") val contractNumber: String,
    @SerializedName("negotiation_id") val negotiationId: Int,
    @SerializedName("listing_id") val listingId: Int,
    @SerializedName("farmer_id") val farmerId: Int,
    @SerializedName("buyer_id") val buyerId: Int,
    @SerializedName("crop_type") val cropType: String,
    val variety: String? = null,
    val quantity: Double,
    val unit: String,
    @SerializedName("agreed_price") val agreedPrice: Double,
    @SerializedName("total_value") val totalValue: Double,
    @SerializedName("delivery_address") val deliveryAddress: String? = null,
    @SerializedName("delivery_date") val deliveryDate: String? = null,
    @SerializedName("quality_grade") val qualityGrade: String? = null,
    @SerializedName("payment_terms") val paymentTerms: String? = null,
    @SerializedName("transport_responsibility") val transportResponsibility: String? = null,
    @SerializedName("additional_terms") val additionalTerms: String? = null,
    @SerializedName("farmer_confirmed") val farmerConfirmed: Boolean,
    @SerializedName("buyer_confirmed") val buyerConfirmed: Boolean,
    val status: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("farmer_name") val farmerName: String? = null,
    @SerializedName("buyer_name") val buyerName: String? = null,
    val milestones: List<Milestone>? = null,
    val payments: List<Payment>? = null
)

// ===== Fulfillment Models =====
data class Milestone(
    val id: Int,
    @SerializedName("contract_id") val contractId: Int,
    @SerializedName("milestone_type") val milestoneType: String,
    val status: String,
    @SerializedName("scheduled_date") val scheduledDate: String? = null,
    @SerializedName("completed_date") val completedDate: String? = null,
    val notes: String? = null,
    @SerializedName("proof_url") val proofUrl: String? = null,
    @SerializedName("proof_type") val proofType: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class CreateMilestoneRequest(
    val milestoneType: String,
    val scheduledDate: String? = null,
    val notes: String? = null
)

data class UpdateMilestoneRequest(
    val status: String,
    val completedDate: String? = null,
    val notes: String? = null
)

// ===== Payment Models =====
data class Payment(
    val id: Int,
    @SerializedName("contract_id") val contractId: Int,
    val amount: Double,
    @SerializedName("payment_method") val paymentMethod: String,
    @SerializedName("payment_status") val paymentStatus: String,
    @SerializedName("transaction_id") val transactionId: String? = null,
    @SerializedName("receipt_url") val receiptUrl: String? = null,
    @SerializedName("payment_date") val paymentDate: String? = null,
    val notes: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class RecordPaymentRequest(
    val amount: Double,
    val paymentMethod: String,
    val paymentStatus: String,
    val transactionId: String? = null,
    val paymentDate: String? = null,
    val notes: String? = null
)

data class PaymentSummary(
    val totalPaid: Double,
    val totalValue: Double,
    val remaining: Double,
    val status: String
)

// ===== Dispute Models =====
data class Dispute(
    val id: Int,
    @SerializedName("contract_id") val contractId: Int,
    @SerializedName("raised_by") val raisedBy: Int,
    val reason: String,
    val description: String,
    val status: String,
    @SerializedName("resolution_notes") val resolutionNotes: String? = null,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("contract_number") val contractNumber: String? = null,
    @SerializedName("crop_type") val cropType: String? = null,
    @SerializedName("raised_by_name") val raisedByName: String? = null,
    val evidence: List<DisputeEvidence>? = null
)

data class DisputeEvidence(
    val id: Int,
    @SerializedName("dispute_id") val disputeId: Int,
    @SerializedName("uploaded_by") val uploadedBy: Int,
    @SerializedName("file_url") val fileUrl: String,
    @SerializedName("file_type") val fileType: String,
    val description: String? = null,
    @SerializedName("created_at") val createdAt: String
)

data class RaiseDisputeRequest(
    val contractId: Int,
    val reason: String,
    val description: String
)

// ===== API Response Wrappers =====
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null
)

data class PaginatedData<T>(
    val items: List<T>? = null,
    val listings: List<T>? = null,
    val negotiations: List<T>? = null,
    val contracts: List<T>? = null,
    val disputes: List<T>? = null,
    val favorites: List<T>? = null,
    val pagination: Pagination
)

data class Pagination(
    val page: Int,
    val limit: Int,
    val total: Int,
    val pages: Int
)

// ===== Favorite Models =====
data class Favorite(
    val id: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("listing_id") val listingId: Int,
    @SerializedName("crop_type") val cropType: String? = null,
    val variety: String? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    @SerializedName("min_price") val minPrice: Double? = null,
    @SerializedName("max_price") val maxPrice: Double? = null,
    val status: String? = null,
    @SerializedName("farmer_name") val farmerName: String? = null,
    @SerializedName("primary_photo") val primaryPhoto: String? = null
)

data class AddFavoriteRequest(
    val listingId: Int
)
