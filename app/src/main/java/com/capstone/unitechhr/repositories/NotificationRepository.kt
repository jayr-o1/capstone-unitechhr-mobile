package com.capstone.unitechhr.repositories

import android.util.Log
import com.capstone.unitechhr.models.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    // Get notifications for a specific applicant
    suspend fun getNotificationsForApplicant(applicantId: String): List<Notification> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("applicants")
                .document(applicantId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                
            return@withContext snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Notification::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("NotificationRepository", "Error converting notification document: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error fetching notifications: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    // Check if there are any unread notifications
    suspend fun hasUnreadNotifications(applicantId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("applicants")
                .document(applicantId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .limit(1)
                .get()
                .await()
                
            return@withContext !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error checking unread notifications: ${e.message}")
            return@withContext false
        }
    }
    
    // Mark a notification as read
    suspend fun markNotificationAsRead(applicantId: String, notificationId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("applicants")
                .document(applicantId)
                .collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
            
            return@withContext true
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error marking notification as read: ${e.message}")
            return@withContext false
        }
    }
    
    // Mark all notifications as read
    suspend fun markAllNotificationsAsRead(applicantId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val batch = firestore.batch()
            val snapshot = firestore.collection("applicants")
                .document(applicantId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .await()
                
            for (doc in snapshot.documents) {
                batch.update(doc.reference, "isRead", true)
            }
            
            batch.commit().await()
            return@withContext true
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error marking all notifications as read: ${e.message}")
            return@withContext false
        }
    }
    
    // For testing - Add a sample notification to an applicant
    suspend fun addSampleNotification(applicantId: String, notification: Notification): Boolean = withContext(Dispatchers.IO) {
        try {
            firestore.collection("applicants")
                .document(applicantId)
                .collection("notifications")
                .add(notification)
                .await()
            
            return@withContext true
        } catch (e: Exception) {
            Log.e("NotificationRepository", "Error adding sample notification: ${e.message}")
            return@withContext false
        }
    }
} 