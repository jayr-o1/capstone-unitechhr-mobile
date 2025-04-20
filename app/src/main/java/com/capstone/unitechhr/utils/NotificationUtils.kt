package com.capstone.unitechhr.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.capstone.unitechhr.MainActivity
import com.capstone.unitechhr.R
import com.google.firebase.messaging.FirebaseMessaging

/**
 * Utility class for managing and displaying notifications
 */
object NotificationUtils {
    private const val TAG = "NotificationUtils"
    private const val CHANNEL_ID = "unitech_notifications"
    private const val CHANNEL_NAME = "UniTech HR Notifications"
    private const val CHANNEL_DESCRIPTION = "Notifications from UniTech HR including job applications and updates"
    private const val NOTIFICATION_ID = 100
    private const val PREFS_NAME = "notification_prefs"
    private const val FCM_TOKEN_KEY = "fcm_token"
    
    // Define notification topics
    private const val TOPIC_ALL_USERS = "all_users"
    private const val TOPIC_JOB_SEEKERS = "job_seekers"
    private const val TOPIC_PREFIX_UNIVERSITY = "university_"
    private const val TOPIC_PREFIX_JOB = "job_"

    /**
     * Create notification channel for Android O and above
     */
    fun createNotificationChannel(context: Context) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
                vibrationPattern = longArrayOf(100, 200, 300, 400, 500)
            }
            
            // Register the channel with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created")
        }
    }

    /**
     * Subscribe to all general and job seeker topics
     */
    fun subscribeToDefaultTopics() {
        // Subscribe to all users topic
        subscribeToTopic(TOPIC_ALL_USERS)
        
        // Subscribe to job seekers topic
        subscribeToTopic(TOPIC_JOB_SEEKERS)
        
        Log.d(TAG, "Subscribed to default topics")
    }

    /**
     * Subscribe to university-specific topics
     */
    fun subscribeToUniversityTopic(universityId: String) {
        subscribeToTopic("$TOPIC_PREFIX_UNIVERSITY$universityId")
        Log.d(TAG, "Subscribed to university topic for ID: $universityId")
    }

    /**
     * Subscribe to a job-specific topic
     */
    fun subscribeToJobTopic(jobId: String) {
        subscribeToTopic("$TOPIC_PREFIX_JOB$jobId")
        Log.d(TAG, "Subscribed to job topic for ID: $jobId")
    }

    /**
     * Unsubscribe from a university topic
     */
    fun unsubscribeFromUniversityTopic(universityId: String) {
        unsubscribeFromTopic("$TOPIC_PREFIX_UNIVERSITY$universityId")
        Log.d(TAG, "Unsubscribed from university topic for ID: $universityId")
    }

    /**
     * Unsubscribe from a job topic
     */
    fun unsubscribeFromJobTopic(jobId: String) {
        unsubscribeFromTopic("$TOPIC_PREFIX_JOB$jobId")
        Log.d(TAG, "Unsubscribed from job topic for ID: $jobId")
    }

    /**
     * Show notification with title and message
     */
    fun showNotification(context: Context, title: String, message: String) {
        showNotification(context, title, message, System.currentTimeMillis().toInt())
    }

    /**
     * Show notification with a specific ID
     */
    fun showNotification(context: Context, title: String, message: String, notificationId: Int) {
        // Create a PendingIntent for when the notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // Build the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_assessment) // Use existing assessment icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        
        // Show the notification
        try {
            with(NotificationManagerCompat.from(context)) {
                // Check permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notify(notificationId, builder.build())
                        Log.d(TAG, "Notification sent: $title - $message")
                    } else {
                        Log.d(TAG, "Notification permission not granted")
                    }
                } else {
                    // For Android 12 and below
                    notify(notificationId, builder.build())
                    Log.d(TAG, "Notification sent: $title - $message")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error sending notification: ${e.message}")
        }
    }

    /**
     * Save FCM token to SharedPreferences and log it
     */
    fun saveFcmToken(context: Context, token: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(FCM_TOKEN_KEY, token)
            apply()
        }
        Log.d(TAG, "FCM Token: $token")
    }

    /**
     * Get the saved FCM token
     */
    fun getFcmToken(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(FCM_TOKEN_KEY, null)
    }

    /**
     * Subscribe to a specific FCM topic
     */
    fun subscribeToTopic(topic: String) {
        FirebaseMessaging.getInstance().subscribeToTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Subscribed to topic: $topic")
                } else {
                    Log.e(TAG, "Failed to subscribe to topic: $topic")
                }
            }
    }

    /**
     * Unsubscribe from a specific FCM topic
     */
    fun unsubscribeFromTopic(topic: String) {
        FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Unsubscribed from topic: $topic")
                } else {
                    Log.e(TAG, "Failed to unsubscribe from topic: $topic")
                }
            }
    }
} 