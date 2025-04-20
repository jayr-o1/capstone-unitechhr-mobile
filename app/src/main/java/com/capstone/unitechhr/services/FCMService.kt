package com.capstone.unitechhr.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.capstone.unitechhr.MainActivity
import com.capstone.unitechhr.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class FCMService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FCMService"
        private const val CHANNEL_ID = "unitech_notifications"
        private const val CHANNEL_NAME = "UniTech Notifications"
        private const val CHANNEL_DESCRIPTION = "General notifications from UniTech HR"
    }
    
    /**
     * Called when a new token is generated
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        
        // Store the token in SharedPreferences or send to your server
        getSharedPreferences("fcm_prefs", Context.MODE_PRIVATE).edit()
            .putString("fcm_token", token)
            .apply()
            
        // TODO: Send the token to your server for targeting specific devices
    }
    
    /**
     * Called when a message is received
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            sendNotification(it.title, it.body)
        }
        
        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message Data: ${remoteMessage.data}")
            
            val title = remoteMessage.data["title"] ?: "UniTech HR"
            val message = remoteMessage.data["message"] ?: "You have a new notification"
            
            sendNotification(title, message)
        }
    }
    
    /**
     * Create and show a notification
     */
    private fun sendNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )
        
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_assessment) // Use an appropriate icon from your app
            .setContentTitle(title ?: "UniTech HR")
            .setContentText(messageBody ?: "You have a new notification")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Show the notification with a unique ID
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
} 