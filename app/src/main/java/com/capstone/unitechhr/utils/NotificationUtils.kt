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

    /**
     * Creates the notification channel for API 26+
     * This method should be called when the app starts (in Application class or MainActivity's onCreate)
     */
    fun createNotificationChannel(context: Context) {
        // Create the notification channel only on API 26+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
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
     * Sends a test notification for debugging purposes
     */
    fun sendTestNotification(context: Context, title: String, message: String) {
        // Create the notification channel first
        createNotificationChannel(context)
        
        // Create an explicit intent for the MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // Create a PendingIntent
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
        
        try {
            with(NotificationManagerCompat.from(context)) {
                // Check permission on Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == 
                        android.content.pm.PackageManager.PERMISSION_GRANTED) {
                        notify(NOTIFICATION_ID, builder.build())
                        Log.d(TAG, "Test notification sent")
                    } else {
                        Log.d(TAG, "Notification permission not granted")
                    }
                } else {
                    // For Android 12 and below
                    notify(NOTIFICATION_ID, builder.build())
                    Log.d(TAG, "Test notification sent")
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Error sending notification: ${e.message}")
        }
    }

    /**
     * Display a notification with the given title and message
     */
    fun showNotification(context: Context, title: String, message: String, notificationId: Int = NOTIFICATION_ID) {
        // Create the notification channel first (does nothing if already created)
        createNotificationChannel(context)
        
        // Create an explicit intent for the MainActivity
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        // Create a PendingIntent
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