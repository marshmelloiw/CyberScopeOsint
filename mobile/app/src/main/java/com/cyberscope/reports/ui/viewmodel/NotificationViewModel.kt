package com.cyberscope.reports.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cyberscope.reports.data.api.ApiClient
import com.cyberscope.reports.data.api.CyberScopeApi
import com.cyberscope.reports.data.model.Notification
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    
    private val api: CyberScopeApi = ApiClient.api
    
    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()
    
    private val _unreadCount = MutableStateFlow<Int>(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()
    
    private var userId: Long? = null
    private var isPolling = false
    
    fun setUserId(userId: Long?) {
        this.userId = userId
        if (userId != null && !isPolling) {
            startPolling()
        }
    }
    
    fun loadNotifications() {
        viewModelScope.launch {
            if (userId == null) return@launch
            
            try {
                val response = api.getNotifications(userId)
                if (response.isSuccessful && response.body() != null) {
                    val notificationsResponse = response.body()!!
                    _notifications.value = notificationsResponse.notifications
                    _unreadCount.value = notificationsResponse.unreadCount
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
    
    private fun startPolling() {
        if (isPolling) return
        isPolling = true
        
        viewModelScope.launch {
            while (isPolling && userId != null) {
                loadNotifications()
                delay(30000) // Poll every 30 seconds
            }
        }
    }
    
    fun stopPolling() {
        isPolling = false
    }
    
    fun markAsRead(notificationId: Int) {
        viewModelScope.launch {
            try {
                val response = api.markNotificationAsRead(notificationId)
                if (response.isSuccessful) {
                    // Reload notifications to update read status
                    loadNotifications()
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }
}

