package com.capstone.unitechhr

import android.app.Application
import android.util.Log
import com.capstone.unitechhr.utils.NetworkUtils
import com.capstone.unitechhr.utils.NotificationUtils
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.Timestamp

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
        
        // First, check if user is logged out
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val isLoggedOut = sharedPreferences.getBoolean("is_logged_out", false)
        
        if (isLoggedOut) {
            Log.d(TAG, "User is logged out, skipping FCM token update")
            return
        }
        
        // Get FCM token only if user is not logged out
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d(TAG, "FCM Token: $token")
                
                // Save token to local storage
                NotificationUtils.saveFcmToken(applicationContext, token)
                
                // Subscribe to default topics
                NotificationUtils.subscribeToDefaultTopics()
                
                // Send the token to server only if user is not logged out
                if (!isLoggedOut) {
                    sendFcmTokenToServer(token)
                }
            } else {
                Log.e(TAG, "Failed to get FCM token", task.exception)
            }
        }
    }
    
    /**
     * Send FCM token to the current user's document in Firestore
     */
    private fun sendFcmTokenToServer(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        currentUser?.let { user ->
            val firestore = FirebaseFirestore.getInstance()
            
            // Update the user's FCM token in Firestore
            firestore.collection("users").document(user.uid)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token updated in Firestore for user: ${user.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error updating FCM token in Firestore", e)
                    
                    // If the user document doesn't exist yet, create it
                    val userData = hashMapOf(
                        "uid" to user.uid,
                        "email" to user.email,
                        "fcmToken" to token,
                        "lastUpdated" to Timestamp.now()
                    )
                    
                    firestore.collection("users").document(user.uid)
                        .set(userData)
                        .addOnSuccessListener {
                            Log.d(TAG, "Created user document with FCM token")
                        }
                        .addOnFailureListener { innerE ->
                            Log.e(TAG, "Error creating user document", innerE)
                        }
                }
        }
    }
} 