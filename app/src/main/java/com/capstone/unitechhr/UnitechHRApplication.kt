package com.capstone.unitechhr

import android.app.Application
import android.util.Log
import com.capstone.unitechhr.utils.NetworkUtils
import com.capstone.unitechhr.utils.NotificationUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

class UnitechHRApplication : Application() {
    companion object {
        private const val TAG = "UnitechHRApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "Initializing application")
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Create notification channel
        NotificationUtils.createNotificationChannel(this)
        
        // Configure network policy for email operations
        NetworkUtils.configureNetworkPolicy()
        
        // Initialize Firebase Cloud Messaging
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        
        // Get FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "FCM Token: $token")
                
                // Subscribe to topics
                NotificationUtils.subscribeToTopic("all_users")
                NotificationUtils.subscribeToTopic("job_seekers")
            } else {
                Log.e(TAG, "Failed to get FCM token", task.exception)
            }
        }
    }
} 