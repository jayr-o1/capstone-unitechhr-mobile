package com.capstone.unitechhr.services

import android.util.Log
import com.capstone.unitechhr.repositories.ApplicantRepository
import com.capstone.unitechhr.utils.NotificationUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Service that extends FirebaseMessagingService to handle incoming FCM messages
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {
    private val TAG = "FCMService"
    private val firestore = FirebaseFirestore.getInstance()

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
        
        // Send the token to server for current user
        sendRegistrationToServer(token)
        
        // Update token for applicant if exists
        updateApplicantToken(token)
    }
    
    /**
     * Update FCM token for applicant in Firestore
     */
    private fun updateApplicantToken(token: String) {
        // Check if there's a current user
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // First, check if user exists as an applicant
                val applicantRepository = ApplicantRepository()
                
                // Try to find an applicant with the user's email
                val applicants = applicantRepository.searchApplicants(currentUser.email ?: "")
                
                // If found, update FCM token
                applicants.firstOrNull()?.let { applicant ->
                    Log.d(TAG, "Found applicant with ID: ${applicant.id}")
                    val success = applicantRepository.updateFcmToken(applicant.id, token)
                    if (success) {
                        Log.d(TAG, "Updated FCM token for applicant: ${applicant.id}")
                    } else {
                        Log.e(TAG, "Failed to update FCM token for applicant: ${applicant.id}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating applicant token: ${e.message}")
            }
        }
    }

    /**
     * Handle incoming data message
     */
    private fun handleDataMessage(data: Map<String, String>) {
        try {
            val title = data["title"] ?: "UniTech HR"
            val message = data["message"] ?: "You have a new notification"
            val type = data["type"] ?: "general"
            
            // We can handle different types of notifications here based on type
            when (type) {
                "new_job" -> {
                    // Handle new job notification
                    val jobId = data["jobId"]
                    val universityId = data["universityId"]
                    Log.d(TAG, "New job notification: $jobId from university: $universityId")
                    
                    // Store notification in local database if needed
                    storeNotification(title, message, type, data)
                }
                "job_application" -> {
                    // Handle job application related notification
                    val applicationId = data["applicationId"]
                    val status = data["status"]
                    Log.d(TAG, "Job application notification: $applicationId, status: $status")
                    
                    // Store notification in local database if needed
                    storeNotification(title, message, type, data)
                }
                "interview_scheduled" -> {
                    // Handle interview scheduling notification
                    val interviewId = data["interviewId"]
                    val jobId = data["jobId"]
                    val applicantId = data["applicantId"]
                    Log.d(TAG, "Interview scheduled notification: $interviewId for job: $jobId, applicant: $applicantId")
                    
                    // Store notification in local database if needed
                    storeNotification(title, message, type, data)
                }
                "applicant_hired" -> {
                    // Handle hired notification
                    val jobId = data["jobId"]
                    val applicantId = data["applicantId"]
                    Log.d(TAG, "Applicant hired notification for job: $jobId, applicant: $applicantId")
                    
                    // Store notification in local database if needed
                    storeNotification(title, message, type, data)
                }
                "onboarding_tasks" -> {
                    // Handle onboarding tasks notification
                    val onboardingId = data["onboardingId"]
                    Log.d(TAG, "Onboarding tasks notification: $onboardingId")
                    
                    // Store notification in local database if needed
                    storeNotification(title, message, type, data)
                }
                "general" -> {
                    // Handle general notification
                    Log.d(TAG, "General notification")
                    
                    // Store notification in local database if needed
                    storeNotification(title, message, type, data)
                }
            }
            
            // Display the notification
            showNotification(title, message)
            
        } catch (e: Exception) {
            Log.e(TAG, "Exception: ${e.message}")
        }
    }

    /**
     * Store notification in Firestore for the current user
     */
    private fun storeNotification(title: String, message: String, type: String, data: Map<String, String>) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        currentUser?.let { user ->
            // Create notification object
            val notification = hashMapOf(
                "title" to title,
                "message" to message,
                "type" to type,
                "data" to data,
                "timestamp" to Timestamp.now(),
                "read" to false
            )
            
            // Store in Firestore
            firestore.collection("users").document(user.uid)
                .collection("notifications")
                .add(notification)
                .addOnSuccessListener {
                    Log.d(TAG, "Notification stored with ID: ${it.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error storing notification", e)
                }
        }
    }

    /**
     * Send FCM token to server for targeting specific device
     */
    private fun sendRegistrationToServer(token: String) {
        // If user is logged in, save token to their document
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        currentUser?.let { user ->
            firestore.collection("users").document(user.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token updated successfully for user: ${user.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating FCM token", e)
                    
                    // If user document doesn't exist yet, create it
                    val userData = hashMapOf(
                        "fcmToken" to token,
                        "lastTokenUpdate" to Timestamp.now()
                    )
                    
                    firestore.collection("users").document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d(TAG, "User document created with FCM token for: ${user.uid}")
                        }
                        .addOnFailureListener { innerE ->
                            Log.e(TAG, "Error creating user document", innerE)
                        }
                }
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