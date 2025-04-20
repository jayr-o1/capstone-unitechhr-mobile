package com.capstone.unitechhr.services

import android.util.Log
import com.capstone.unitechhr.utils.NotificationUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Service that extends FirebaseMessagingService to handle incoming FCM messages
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCMService"

    /**
     * Called when a new FCM message arrives
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if message contains notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it.title, it.body)
        }
    }

    /**
     * Called when a new FCM token is generated
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // Save the token
        NotificationUtils.saveFcmToken(applicationContext, token)
        
        // If you need to send the token to your server, do it here
        // sendRegistrationToServer(token)
    }

    /**
     * Handle incoming data message
     */
    private fun handleDataMessage(data: Map<String, String>) {
        try {
            val title = data["title"] ?: "UniTech HR"
            val message = data["message"] ?: "You have a new notification"
            val type = data["type"] ?: "general"
            
            // We can handle different types of notifications here
            when (type) {
                "job_application" -> {
                    // Handle job application related notification
                    val applicationId = data["applicationId"]
                    val status = data["status"]
                    Log.d(TAG, "Job application notification: $applicationId, status: $status")
                }
                "interview" -> {
                    // Handle interview related notification
                    val interviewId = data["interviewId"]
                    val time = data["time"]
                    Log.d(TAG, "Interview notification: $interviewId, time: $time")
                }
                "general" -> {
                    // Handle general notification
                    Log.d(TAG, "General notification")
                }
            }
            
            // Display the notification
            showNotification(title, message)
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
        }
    }

    /**
     * Show notification with given title and message
     */
    private fun showNotification(title: String?, message: String?) {
        val notificationTitle = title ?: "UniTech HR"
        val notificationMessage = message ?: "You have a new notification"
        
        NotificationUtils.showNotification(
            applicationContext,
            notificationTitle,
            notificationMessage
        )
    }
} 