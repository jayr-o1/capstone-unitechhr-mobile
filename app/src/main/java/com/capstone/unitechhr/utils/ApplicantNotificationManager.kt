package com.capstone.unitechhr.utils

import android.content.Context
import android.util.Log
import com.capstone.unitechhr.models.Applicant
import com.capstone.unitechhr.repositories.ApplicantRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Utility class for managing applicant notifications and FCM topics
 */
object ApplicantNotificationManager {
    private const val TAG = "ApplicantNotificationMgr"
    
    // Define applicant-specific topics
    private const val TOPIC_ALL_APPLICANTS = "all_applicants"
    private const val TOPIC_PREFIX_APPLICANT = "applicant_"
    private const val TOPIC_PREFIX_POSITION = "position_"
    private const val TOPIC_PREFIX_STATUS = "status_"
    
    /**
     * Initialize FCM for an applicant
     * - Updates FCM token
     * - Subscribes to relevant topics
     * - Checks notification permissions
     */
    fun initializeApplicantFcm(context: Context, applicant: Applicant) {
        // Get current FCM token and update
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }
            
            val token = task.result
            Log.d(TAG, "FCM Token: $token")
            
            // If token is different from stored token, update it
            if (token != applicant.fcmToken) {
                updateApplicantFcmToken(applicant.id, token)
            }
            
            // Subscribe to topics
            subscribeApplicantToTopics(applicant)
        }
        
        // Check notification permissions and request if needed
        checkAndRequestNotificationPermission(context)
    }
    
    /**
     * Update an applicant's FCM token in Firestore
     */
    private fun updateApplicantFcmToken(applicantId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ApplicantRepository()
                val success = repository.updateFcmToken(applicantId, token)
                if (success) {
                    Log.d(TAG, "FCM token updated for applicant: $applicantId")
                } else {
                    Log.e(TAG, "Failed to update FCM token for applicant: $applicantId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating FCM token: ${e.message}")
            }
        }
    }
    
    /**
     * Subscribe an applicant to relevant FCM topics
     */
    private fun subscribeApplicantToTopics(applicant: Applicant) {
        if (!applicant.notificationsEnabled) {
            Log.d(TAG, "Notifications disabled for applicant: ${applicant.id}")
            return
        }
        
        // Subscribe to all applicants topic
        subscribeToTopic(TOPIC_ALL_APPLICANTS)
        
        // Subscribe to applicant-specific topic
        subscribeToTopic("$TOPIC_PREFIX_APPLICANT${applicant.id}")
        
        // Subscribe to applied position topic
        val positionTopic = applicant.appliedPosition.replace(" ", "_").lowercase()
        subscribeToTopic("$TOPIC_PREFIX_POSITION$positionTopic")
        
        // Subscribe to application status topic
        subscribeToTopic("$TOPIC_PREFIX_STATUS${applicant.status.name.lowercase()}")
        
        Log.d(TAG, "Applicant subscribed to topics: ${applicant.id}")
    }
    
    /**
     * Unsubscribe an applicant from FCM topics
     */
    fun unsubscribeApplicantFromTopics(applicant: Applicant) {
        // Unsubscribe from all applicants topic
        unsubscribeFromTopic(TOPIC_ALL_APPLICANTS)
        
        // Unsubscribe from applicant-specific topic
        unsubscribeFromTopic("$TOPIC_PREFIX_APPLICANT${applicant.id}")
        
        // Unsubscribe from applied position topic
        val positionTopic = applicant.appliedPosition.replace(" ", "_").lowercase()
        unsubscribeFromTopic("$TOPIC_PREFIX_POSITION$positionTopic")
        
        // Unsubscribe from application status topic
        unsubscribeFromTopic("$TOPIC_PREFIX_STATUS${applicant.status.name.lowercase()}")
        
        Log.d(TAG, "Applicant unsubscribed from topics: ${applicant.id}")
    }
    
    /**
     * Enable or disable notifications for an applicant
     */
    fun setApplicantNotificationsEnabled(applicantId: String, enabled: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = ApplicantRepository()
                val success = repository.setNotificationsEnabled(applicantId, enabled)
                
                if (success) {
                    Log.d(TAG, "Notification settings updated for applicant: $applicantId to $enabled")
                    
                    // Fetch updated applicant data
                    val applicant = repository.getApplicantById(applicantId)
                    applicant?.let {
                        // Subscribe or unsubscribe based on new setting
                        if (enabled) {
                            subscribeApplicantToTopics(it)
                        } else {
                            unsubscribeApplicantFromTopics(it)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating notification settings: ${e.message}")
            }
        }
    }
    
    /**
     * Subscribe to a specific FCM topic
     */
    private fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to topic: $topic", task.exception)
                }
            }
    }
    
    /**
     * Unsubscribe from a specific FCM topic
     */
    private fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                } else {
                    Log.e(TAG, "Failed to unsubscribe from topic: $topic", task.exception)
                }
            }
    }
    
    /**
     * Check and request notification permissions if needed
     */
    private fun checkAndRequestNotificationPermission(context: Context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionStatus = context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            
            if (permissionStatus != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Notification permission not granted")
                // Note: Actual permission request would need to be done from an Activity
                // This utility class just logs the status
            } else {
                Log.d(TAG, "Notification permission already granted")
            }
        }
    }
} 