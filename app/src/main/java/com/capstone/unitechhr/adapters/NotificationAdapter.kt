package com.capstone.unitechhr.adapters

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.Notification
import com.capstone.unitechhr.models.NotificationType
import java.util.Date

class NotificationAdapter(
    private val onItemClick: (Notification) -> Unit,
    private val onDismissClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {
    
    private var notifications: List<Notification> = emptyList()
    
    fun updateNotifications(newNotifications: List<Notification>) {
        notifications = newNotifications
        notifyDataSetChanged()
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view, onItemClick, onDismissClick)
    }
    
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notifications[position])
    }
    
    override fun getItemCount() = notifications.size
    
    class NotificationViewHolder(
        itemView: View,
        private val onItemClick: (Notification) -> Unit,
        private val onDismissClick: (Notification) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val typeIcon: ImageView = itemView.findViewById(R.id.notificationTypeIcon)
        private val unreadIndicator: View = itemView.findViewById(R.id.unreadIndicator)
        private val title: TextView = itemView.findViewById(R.id.notificationTitle)
        private val message: TextView = itemView.findViewById(R.id.notificationMessage)
        private val time: TextView = itemView.findViewById(R.id.notificationTime)
        private val dismissButton: ImageButton = itemView.findViewById(R.id.dismissButton)
        
        fun bind(notification: Notification) {
            // Set notification data
            title.text = notification.title
            message.text = notification.message
            time.text = getTimeAgo(notification.timestamp)
            
            // Set read/unread indicator
            unreadIndicator.visibility = if (notification.isRead) View.GONE else View.VISIBLE
            
            // Set icon based on notification type
            when (notification.type) {
                NotificationType.JOB_POSTED, NotificationType.NEW_JOB -> typeIcon.setImageResource(R.drawable.ic_work)
                NotificationType.INTERVIEW_SCHEDULED -> typeIcon.setImageResource(R.drawable.ic_schedule)
                NotificationType.APPLICATION_STATUS_CHANGE -> typeIcon.setImageResource(R.drawable.ic_assessment)
                else -> typeIcon.setImageResource(R.drawable.ic_notifications)
            }
            
            // Set click listener for the notification item
            itemView.setOnClickListener {
                onItemClick(notification)
            }
            
            // Set click listener for the dismiss button
            dismissButton.setOnClickListener {
                onDismissClick(notification)
            }
        }
        
        private fun getTimeAgo(date: Date): String {
            val now = System.currentTimeMillis()
            val timeAgo = DateUtils.getRelativeTimeSpanString(
                date.time,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            )
            return timeAgo.toString()
        }
    }
} 