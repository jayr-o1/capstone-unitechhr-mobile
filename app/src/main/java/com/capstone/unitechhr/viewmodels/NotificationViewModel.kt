package com.capstone.unitechhr.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.models.Notification
import com.capstone.unitechhr.repositories.NotificationRepository
import kotlinx.coroutines.launch

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()
    
    private val _notifications = MutableLiveData<List<Notification>>()
    val notifications: LiveData<List<Notification>> = _notifications
    
    private val _hasUnreadNotifications = MutableLiveData<Boolean>()
    val hasUnreadNotifications: LiveData<Boolean> = _hasUnreadNotifications
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _operationResult = MutableLiveData<Boolean?>()
    val operationResult: LiveData<Boolean?> = _operationResult
    
    // Load notifications for the current applicant
    fun loadNotifications(applicantId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val notificationList = repository.getNotificationsForApplicant(applicantId)
                _notifications.value = notificationList
                
                // Check if there are any unread notifications
                _hasUnreadNotifications.value = notificationList.any { !it.isRead }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Check for unread notifications
    fun checkUnreadNotifications(applicantId: String) {
        viewModelScope.launch {
            try {
                _hasUnreadNotifications.value = repository.hasUnreadNotifications(applicantId)
            } catch (e: Exception) {
                // Handle error
                _hasUnreadNotifications.value = false
            }
        }
    }
    
    // Mark a notification as read
    fun markAsRead(applicantId: String, notificationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val success = repository.markNotificationAsRead(applicantId, notificationId)
                _operationResult.value = success
                
                // Update the local list to reflect changes
                if (success) {
                    _notifications.value = _notifications.value?.map { 
                        if (it.id == notificationId) it.copy(isRead = true) else it 
                    }
                    
                    // Check if all notifications are now read
                    _hasUnreadNotifications.value = _notifications.value?.any { !it.isRead } ?: false
                }
            } catch (e: Exception) {
                _operationResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Dismiss a notification (hide but don't delete it)
    fun dismissNotification(applicantId: String, notificationId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val success = repository.dismissNotification(applicantId, notificationId)
                _operationResult.value = success
                
                // Update the local list to reflect changes by removing the notification
                if (success) {
                    val updatedList = _notifications.value?.filter { it.id != notificationId } ?: emptyList()
                    _notifications.value = updatedList
                    
                    // Check if all remaining notifications are read
                    _hasUnreadNotifications.value = updatedList.any { !it.isRead }
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Error dismissing notification: ${e.message}")
                _operationResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // Mark all notifications as read
    fun markAllAsRead(applicantId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val success = repository.markAllNotificationsAsRead(applicantId)
                _operationResult.value = success
                
                // Update the local list to reflect changes
                if (success) {
                    _notifications.value = _notifications.value?.map { it.copy(isRead = true) }
                    _hasUnreadNotifications.value = false
                }
            } catch (e: Exception) {
                _operationResult.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    // For testing - Add a sample notification
    fun addSampleNotification(applicantId: String, notification: Notification) {
        viewModelScope.launch {
            try {
                repository.addSampleNotification(applicantId, notification)
                // Reload notifications to get the latest
                loadNotifications(applicantId)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // For testing - Add a sample general notification
    fun addSampleGeneralNotification(notification: Notification) {
        viewModelScope.launch {
            try {
                repository.addSampleGeneralNotification(notification)
                // Check for notifications again to update the UI
                _hasUnreadNotifications.value = true
            } catch (e: Exception) {
                // Handle error
                Log.e("NotificationViewModel", "Error adding general notification: ${e.message}")
            }
        }
    }
    
    fun clearOperationResult() {
        _operationResult.value = null
    }
} 