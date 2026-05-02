package com.farmconnect.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.farmconnect.app.data.models.Notification
import com.farmconnect.app.data.repository.NotificationRepository
import com.farmconnect.app.data.repository.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NotificationUiState(
    val isLoading: Boolean = false,
    val notifications: List<Notification> = emptyList(),
    val unreadCount: Int = 0,
    val error: String? = null,
    val selectedFilter: String = "all" // all, negotiation, contract, payment
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState
    
    init {
        loadNotifications()
    }
    
    fun loadNotifications(filter: String = "all") {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, selectedFilter = filter)
            
            val typeFilter = when (filter) {
                "negotiation", "contract", "payment" -> filter
                else -> null
            }
            
            when (val result = notificationRepository.getNotifications(type = typeFilter)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        notifications = result.data.notifications,
                        unreadCount = result.data.unreadCount,
                        error = null
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = result.message
                    )
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            when (notificationRepository.markAsRead(notificationId)) {
                is Result.Success -> {
                    // Update notification in list
                    val updatedNotifications = _uiState.value.notifications.map {
                        if (it.id == notificationId) it.copy(isRead = true) else it
                    }
                    _uiState.value = _uiState.value.copy(
                        notifications = updatedNotifications,
                        unreadCount = maxOf(0, _uiState.value.unreadCount - 1)
                    )
                }
                is Result.Error -> {
                    // Handle error silently or show toast
                }
                is Result.Loading -> {}
            }
        }
    }
    
    fun markAllAsRead() {
        viewModelScope.launch {
            when (notificationRepository.markAllAsRead()) {
                is Result.Success -> {
                    val updatedNotifications = _uiState.value.notifications.map {
                        it.copy(isRead = true)
                    }
                    _uiState.value = _uiState.value.copy(
                        notifications = updatedNotifications,
                        unreadCount = 0
                    )
                }
                is Result.Error -> {}
                is Result.Loading -> {}
            }
        }
    }
    
    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch {
            when (notificationRepository.deleteNotification(notificationId)) {
                is Result.Success -> {
                    val notification = _uiState.value.notifications.find { it.id == notificationId }
                    val updatedNotifications = _uiState.value.notifications.filter { it.id != notificationId }
                    _uiState.value = _uiState.value.copy(
                        notifications = updatedNotifications,
                        unreadCount = if (notification?.isRead == false) 
                            maxOf(0, _uiState.value.unreadCount - 1) 
                        else 
                            _uiState.value.unreadCount
                    )
                }
                is Result.Error -> {}
                is Result.Loading -> {}
            }
        }
    }
}
