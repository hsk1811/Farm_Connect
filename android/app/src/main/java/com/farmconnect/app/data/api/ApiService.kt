package com.farmconnect.app.data.api

import com.farmconnect.app.data.models.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // ===== Upload =====
    @Multipart
    @POST("upload")
    suspend fun uploadImages(@Part files: List<MultipartBody.Part>): Response<ApiResponse<UploadResponse>>

    // ===== Authentication =====
    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<AuthResponse>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @GET("auth/me")
    suspend fun getCurrentUser(): Response<ApiResponse<Map<String, Any>>>

    @PUT("auth/profile")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<UserProfile>>
    
    @POST("auth/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<ApiResponse<Map<String, Any>>>
    
    @POST("auth/change-password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Map<String, Any>>>

    // ===== Listings =====
    @GET("listings")
    suspend fun getListings(
        @Query("cropType") cropType: String? = null,
        @Query("variety") variety: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("minQuantity") minQuantity: Double? = null,
        @Query("maxQuantity") maxQuantity: Double? = null,
        @Query("qualityGrade") qualityGrade: String? = null,
        @Query("city") city: String? = null,
        @Query("state") state: String? = null,
        @Query("harvestStartAfter") harvestStartAfter: String? = null,
        @Query("harvestEndBefore") harvestEndBefore: String? = null,
        @Query("status") status: String? = null,
        @Query("sortBy") sortBy: String? = null,
        @Query("sortOrder") sortOrder: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<PaginatedData<Listing>>>

    @GET("listings/my")
    suspend fun getMyListings(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<PaginatedData<Listing>>>

    @GET("listings/{id}")
    suspend fun getListingById(@Path("id") id: Int): Response<ApiResponse<Listing>>

    @POST("listings")
    suspend fun createListing(@Body request: CreateListingRequest): Response<ApiResponse<Listing>>

    @PATCH("listings/{id}/status")
    suspend fun updateListingStatus(
        @Path("id") id: Int,
        @Body request: UpdateStatusRequest
    ): Response<ApiResponse<Listing>>

    @DELETE("listings/{id}")
    suspend fun deleteListing(@Path("id") id: Int): Response<ApiResponse<Unit>>

    @PUT("listings/{id}")
    suspend fun updateListing(
        @Path("id") id: Int,
        @Body request: CreateListingRequest
    ): Response<ApiResponse<Listing>>

    @PATCH("listings/{id}/status")
    suspend fun updateListingStatus(
        @Path("id") id: Int,
        @Body status: Map<String, String>
    ): Response<ApiResponse<Listing>>

    // ===== Buyer =====
    @GET("buyer/listings")
    suspend fun browseListings(
        @Query("cropType") cropType: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("minQuantity") minQuantity: Double? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<PaginatedData<Listing>>>

    @GET("buyer/favorites")
    suspend fun getFavorites(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<PaginatedData<Favorite>>>

    @POST("buyer/favorites")
    suspend fun addFavorite(@Body request: AddFavoriteRequest): Response<ApiResponse<Favorite>>

    @DELETE("buyer/favorites/{listingId}")
    suspend fun removeFavorite(@Path("listingId") listingId: Int): Response<ApiResponse<Unit>>

    @GET("buyer/favorites/{listingId}/check")
    suspend fun checkFavorite(@Path("listingId") listingId: Int): Response<ApiResponse<Map<String, Boolean>>>

    // ===== Negotiations =====
    @GET("negotiations")
    suspend fun getNegotiations(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<PaginatedData<Negotiation>>>

    @GET("negotiations/{id}")
    suspend fun getNegotiationById(@Path("id") id: Int): Response<ApiResponse<Negotiation>>

    @POST("negotiations")
    suspend fun createNegotiation(@Body request: CreateNegotiationRequest): Response<ApiResponse<Negotiation>>

    @POST("negotiations/{id}/messages")
    suspend fun sendMessage(
        @Path("id") id: Int,
        @Body request: SendMessageRequest
    ): Response<ApiResponse<NegotiationMessage>>

    @POST("negotiations/{id}/accept")
    suspend fun acceptNegotiation(
        @Path("id") id: Int,
        @Body request: AcceptNegotiationRequest
    ): Response<ApiResponse<Map<String, Any>>>

    @POST("negotiations/{id}/reject")
    suspend fun rejectNegotiation(
        @Path("id") id: Int,
        @Body reason: Map<String, String>
    ): Response<ApiResponse<Negotiation>>

    // ===== Contracts =====
    @GET("contracts")
    suspend fun getContracts(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<PaginatedData<Contract>>>

    @GET("contracts/{id}")
    suspend fun getContractById(@Path("id") id: Int): Response<ApiResponse<Contract>>

    @POST("contracts/{id}/confirm")
    suspend fun confirmContract(
        @Path("id") id: Int,
        @Body confirmed: Map<String, Boolean>
    ): Response<ApiResponse<Contract>>

    @PATCH("contracts/{id}/status")
    suspend fun updateContractStatus(
        @Path("id") id: Int,
        @Body status: Map<String, String>
    ): Response<ApiResponse<Contract>>

    // ===== Fulfillment =====
    @GET("contracts/{id}/fulfillment")
    suspend fun getMilestones(@Path("id") contractId: Int): Response<ApiResponse<List<Milestone>>>

    @POST("contracts/{id}/fulfillment/milestones")
    suspend fun createMilestone(
        @Path("id") contractId: Int,
        @Body request: CreateMilestoneRequest
    ): Response<ApiResponse<Milestone>>

    @PUT("contracts/fulfillment/milestones/{milestoneId}")
    suspend fun updateMilestone(
        @Path("milestoneId") milestoneId: Int,
        @Body request: UpdateMilestoneRequest
    ): Response<ApiResponse<Milestone>>

    // ===== Payments =====
    @GET("contracts/{id}/payments")
    suspend fun getPayments(@Path("id") contractId: Int): Response<ApiResponse<Map<String, Any>>>

    @POST("contracts/{id}/payments")
    suspend fun recordPayment(
        @Path("id") contractId: Int,
        @Body request: RecordPaymentRequest
    ): Response<ApiResponse<Payment>>

    // ===== Disputes =====
    @GET("disputes")
    suspend fun getDisputes(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20
    ): Response<ApiResponse<PaginatedData<Dispute>>>

    @GET("disputes/{id}")
    suspend fun getDisputeById(@Path("id") id: Int): Response<ApiResponse<Dispute>>

    @POST("disputes")
    suspend fun raiseDispute(@Body request: RaiseDisputeRequest): Response<ApiResponse<Dispute>>

    @POST("disputes/{id}/evidence")
    suspend fun addEvidence(
        @Path("id") id: Int,
        @Body evidence: Map<String, String>
    ): Response<ApiResponse<DisputeEvidence>>
    
    // ===== Notifications =====
    @GET("notifications")
    suspend fun getNotifications(
        @Query("type") type: String? = null,
        @Query("isRead") isRead: Boolean? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 50
    ): Response<ApiResponse<NotificationListResponse>>
    
    @PUT("notifications/{id}/read")
    suspend fun markNotificationAsRead(@Path("id") id: Int): Response<ApiResponse<Notification>>
    
    @PUT("notifications/read-all")
    suspend fun markAllNotificationsAsRead(): Response<ApiResponse<Map<String, Any>>>
    
    @DELETE("notifications/{id}")
    suspend fun deleteNotification(@Path("id") id: Int): Response<ApiResponse<Map<String, Any>>>
}
