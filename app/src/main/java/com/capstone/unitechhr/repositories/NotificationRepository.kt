package com.capstone.unitechhr.repositories

import android.util.Log
import com.capstone.unitechhr.models.Notification
import com.capstone.unitechhr.models.NotificationType
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "NotificationRepository"
    
    // Get notifications for a specific applicant
    suspend fun getNotificationsForApplicant(applicantId: String): List<Notification> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching notifications for applicantId: $applicantId")
            val notifications = mutableListOf<Notification>()
            
            // Get personal notifications - get all and filter in-memory to avoid index issues
            try {
                Log.d(TAG, "Fetching personal notifications...")
                val personalSnapshot = firestore.collection("applicants")
                    .document(applicantId)
                    .collection("notifications")
                    .get()
                    .await()
                
                Log.d(TAG, "Found ${personalSnapshot.size()} personal notifications")
                personalSnapshot.documents.forEach { doc ->
                    try {
                        val notification = convertDocToNotification(doc)
                        // Filter out dismissed notifications in memory
                        if (notification != null && !notification.isDismissed) {
                            notifications.add(notification)
                            Log.d(TAG, "Added personal notification: ${notification.title}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting notification document: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching personal notifications: ${e.message}")
            }
            
            // Get general notifications - get all and filter in-memory to avoid index issues
            try {
                Log.d(TAG, "Fetching general notifications...")
                val generalSnapshot = firestore.collection("applicants_general_notifications")
                    .get()
                    .await()
                
                Log.d(TAG, "Found ${generalSnapshot.size()} general notifications")
                generalSnapshot.documents.forEach { doc ->
                    try {
                        val notification = convertDocToNotification(doc)
                        // Filter out dismissed notifications in memory
                        if (notification != null && !notification.isDismissed) {
                            notifications.add(notification)
                            Log.d(TAG, "Added general notification: ${notification.title}")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting general notification document: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching general notifications: ${e.message}")
            }
            
            // Sort the combined list by timestamp (newest first)
            val sortedNotifications = notifications.sortedByDescending { it.timestamp }
            Log.d(TAG, "Returning total of ${sortedNotifications.size} notifications")
            
            return@withContext sortedNotifications
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching notifications: ${e.message}", e)
            return@withContext emptyList()
        }
    }
    
    // Custom conversion function to handle type mismatches
    private fun convertDocToNotification(doc: DocumentSnapshot): Notification? {
        try {
            // Extract basic fields
            val id = doc.id
            val title = doc.getString("title") ?: ""
            val message = doc.getString("message") ?: ""
            val timestamp = doc.getDate("timestamp") ?: Date()
            val isRead = doc.getBoolean("read") ?: false
            val isDismissed = doc.getBoolean("dismissed") ?: false
            
            // Handle enum conversion
            val typeString = doc.getString("type") ?: "GENERAL"
            val type = try {
                NotificationType.fromString(typeString)
            } catch (e: Exception) {
                Log.w(TAG, "Unknown notification type: $typeString, defaulting to GENERAL")
                NotificationType.GENERAL
            }
            
            // Extract additional fields
            val relatedItemId = doc.getString("relatedItemId") ?: ""
            val universityId = doc.getString("universityId")
            val universityName = doc.getString("universityName")
            val jobId = doc.getString("jobId")
            val jobTitle = doc.getString("jobTitle")
            
            // Create notification object
            return Notification(
                id = id,
                title = title,
                message = message,
                timestamp = timestamp,
                isRead = isRead,
                isDismissed = isDismissed,
                type = type,
                relatedItemId = relatedItemId,
                universityId = universityId,
                universityName = universityName,
                jobId = jobId,
                jobTitle = jobTitle
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error manually parsing notification: ${e.message}", e)
            return null
        }
    }
    
    // Check if there are any unread notifications
    suspend fun hasUnreadNotifications(applicantId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Checking for unread notifications for applicantId: $applicantId")
            
            // Approach 1: Use simple queries without composite indexes
            // Check personal notifications with just one condition at a time
            val personalSnapshot = firestore.collection("applicants")
                .document(applicantId)
                .collection("notifications")
                .whereEqualTo("read", false)  // Just check for unread
                .get()
                .await()
            
            // Filter in memory for non-dismissed notifications
            val hasUnreadPersonal = personalSnapshot.documents.any { doc ->
                val isDismissed = doc.getBoolean("dismissed") ?: false
                !isDismissed  // Return true if not dismissed
            }
            
            // If we found unread personal notifications, no need to check general
            if (hasUnreadPersonal) {
                Log.d(TAG, "Found unread personal notification")
                return@withContext true
            }
            
            // Check general notifications with just one condition
            val generalSnapshot = firestore.collection("applicants_general_notifications")
                .whereEqualTo("read", false)  // Just check for unread
                .get()
                .await()
                
            // Filter in memory for non-dismissed notifications
            val hasUnreadGeneral = generalSnapshot.documents.any { doc ->
                val isDismissed = doc.getBoolean("dismissed") ?: false
                !isDismissed  // Return true if not dismissed
            }
            
            Log.d(TAG, "Has unread general notifications: $hasUnreadGeneral")
            return@withContext hasUnreadGeneral
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking unread notifications: ${e.message}", e)
            return@withContext false
        }
    }
    
    // Mark a notification as read
    suspend fun markNotificationAsRead(applicantId: String, notificationId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Marking notification as read: $notificationId for applicant: $applicantId")
            // First try to find and mark in personal notifications
            val personalDoc = firestore.collection("applicants")
                .document(applicantId)
                .collection("notifications")
                .document(notificationId)
                .get()
                .await()
                
            if (personalDoc.exists()) {
                firestore.collection("applicants")
                    .document(applicantId)
                    .collection("notifications")
                    .document(notificationId)
                    .update("read", true)  // Use 'read' field, not 'isRead'
                    .await()
                Log.d(TAG, "Marked personal notification as read: $notificationId")
                return@withContext true
            }
            
            // If not in personal, try general notifications
            val generalDoc = firestore.collection("applicants_general_notifications")
                .document(notificationId)
                .get()
                .await()
                
            if (generalDoc.exists()) {
                firestore.collection("applicants_general_notifications")
                    .document(notificationId)
                    .update("read", true)  // Use 'read' field, not 'isRead'
                    .await()
                Log.d(TAG, "Marked general notification as read: $notificationId")
                return@withContext true
            }
            
            Log.d(TAG, "Notification not found in either collection: $notificationId")
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read: ${e.message}", e)
            return@withContext false
        }
    }
    
    // Mark all notifications as read
    suspend fun markAllNotificationsAsRead(applicantId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Marking all notifications as read for applicant: $applicantId")
            val batch = firestore.batch()
            
            // Mark personal notifications as read
            val personalSnapshot = firestore.collection("applicants")
                .document(applicantId)
                .collection("notifications")
                .whereEqualTo("read", false)  // Use 'read' field, not 'isRead'
                .get()
                .await()
                
            Log.d(TAG, "Found ${personalSnapshot.size()} unread personal notifications to mark as read")
            for (doc in personalSnapshot.documents) {
                batch.update(doc.reference, "read", true)  // Use 'read' field, not 'isRead'
            }
            
            // Mark general notifications as read
            val generalSnapshot = firestore.collection("applicants_general_notifications")
                .whereEqualTo("read", false)  // Use 'read' field, not 'isRead'
                .get()
                .await()
                
            Log.d(TAG, "Found ${generalSnapshot.size()} unread general notifications to mark as read")
            for (doc in generalSnapshot.documents) {
                batch.update(doc.reference, "read", true)  // Use 'read' field, not 'isRead'
            }
            
            batch.commit().await()
            Log.d(TAG, "Successfully marked all notifications as read")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error marking all notifications as read: ${e.message}", e)
            return@withContext false
        }
    }
    
    // For testing - Add a sample notification to an applicant
    suspend fun addSampleNotification(applicantId: String, notification: Notification): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Adding sample notification for applicant: $applicantId, title: ${notification.title}")
            // Convert the notification to a map to handle field name differences
            val notificationData = mapOf(
                "title" to notification.title,
                "message" to notification.message,
                "timestamp" to notification.timestamp,
                "read" to notification.isRead,  // Convert isRead to read for Firestore
                "dismissed" to notification.isDismissed, // Include dismissed field
                "type" to notification.type.name,
                "relatedItemId" to notification.relatedItemId
            )
            
            firestore.collection("applicants")
                .document(applicantId)
                .collection("notifications")
                .add(notificationData)
                .await()
            
            Log.d(TAG, "Successfully added sample notification for applicant: $applicantId")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding sample notification: ${e.message}", e)
            return@withContext false
        }
    }
    
    // For testing - Add a sample notification to the general notifications collection
    suspend fun addSampleGeneralNotification(notification: Notification): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Adding sample general notification: ${notification.title}")
            // Convert the notification to a map to handle field name differences
            val notificationData = mapOf(
                "title" to notification.title,
                "message" to notification.message,
                "timestamp" to notification.timestamp,
                "read" to notification.isRead,  // Convert isRead to read for Firestore
                "dismissed" to notification.isDismissed, // Include dismissed field
                "type" to notification.type.name,
                "relatedItemId" to notification.relatedItemId
            )
            
            firestore.collection("applicants_general_notifications")
                .add(notificationData)
                .await()
            
            Log.d(TAG, "Successfully added general notification")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding sample general notification: ${e.message}", e)
            return@withContext false
        }
    }
    
    // Dismiss a notification (hide but don't delete)
    suspend fun dismissNotification(applicantId: String, notificationId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Dismissing notification: $notificationId for applicant: $applicantId")
            // First try to find and mark in personal notifications
            val personalDoc = firestore.collection("applicants")
                .document(applicantId)
                .collection("notifications")
                .document(notificationId)
                .get()
                .await()
                
            if (personalDoc.exists()) {
                firestore.collection("applicants")
                    .document(applicantId)
                    .collection("notifications")
                    .document(notificationId)
                    .update("dismissed", true)
                    .await()
                Log.d(TAG, "Dismissed personal notification: $notificationId")
                return@withContext true
            }
            
            // If not in personal, try general notifications
            val generalDoc = firestore.collection("applicants_general_notifications")
                .document(notificationId)
                .get()
                .await()
                
            if (generalDoc.exists()) {
                firestore.collection("applicants_general_notifications")
                    .document(notificationId)
                    .update("dismissed", true)
                    .await()
                Log.d(TAG, "Dismissed general notification: $notificationId")
                return@withContext true
            }
            
            Log.d(TAG, "Notification not found in either collection: $notificationId")
            return@withContext false
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing notification: ${e.message}", e)
            return@withContext false
        }
    }
} 