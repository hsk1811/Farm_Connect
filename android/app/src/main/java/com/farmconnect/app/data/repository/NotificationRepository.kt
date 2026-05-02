package com.farmconnect.app.data.repository

import com.farmconnect.app.data.api.ApiService
import com.farmconnect.app.data.models.Notification
import com.farmconnect.app.data.models.NotificationListResponse
import com.farmconnect.app.data.repository.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getNotifications(
        type: String? = null,
        isRead: Boolean? = null,
        page: Int = 1
    ): Result<NotificationListResponse> {
        return try {
            val response = apiService.getNotifications(type, isRead, page, 50)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to load notifications")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
    
    suspend fun markAsRead(notificationId: Int): Result<Notification> {
        return try {
            val response = apiService.markNotificationAsRead(notificationId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success(response.body()?.data!!)
            } else {
                Result.Error(response.body()?.error ?: "Failed to mark as read")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
    
    suspend fun markAllAsRead(): Result<String> {
        return try {
            val response = apiService.markAllNotificationsAsRead()
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success("All marked as read")
            } else {
                Result.Error(response.body()?.error ?: "Failed to mark all as read")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
    
    suspend fun deleteNotification(notificationId: Int): Result<String> {
        return try {
            val response = apiService.deleteNotification(notificationId)
            if (response.isSuccessful && response.body()?.success == true) {
                Result.Success("Notification deleted")
            } else {
                Result.Error(response.body()?.error ?: "Failed to delete notification")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}
