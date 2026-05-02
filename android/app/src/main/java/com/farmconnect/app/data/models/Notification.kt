package com.farmconnect.app.data.models

import com.google.gson.annotations.SerializedName

data class Notification(
    val id: Int,
    val type: String, // "negotiation", "contract", "payment", "general"
    val title: String,
    val message: String,
    @SerializedName("is_read") val isRead: Boolean,
    val data: NotificationData? = null,
    @SerializedName("created_at") val createdAt: String
)

data class NotificationData(
    @SerializedName("negotiation_id") val negotiationId: Int? = null,
    @SerializedName("contract_id") val contractId: Int? = null,
    @SerializedName("listing_id") val listingId: Int? = null,
    val amount: Double? = null
)

data class NotificationListResponse(
    val notifications: List<Notification>,
    val unreadCount: Int
)
