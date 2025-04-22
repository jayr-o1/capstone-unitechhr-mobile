package com.capstone.unitechhr.models

import com.google.firebase.firestore.PropertyName
import java.util.Date

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Date = Date(),
    
    @get:PropertyName("read")
    @set:PropertyName("read")
    var isRead: Boolean = false,
    
    @get:PropertyName("dismissed")
    @set:PropertyName("dismissed")
    var isDismissed: Boolean = false,
    
    val type: NotificationType = NotificationType.GENERAL,
    val relatedItemId: String = "",
    
    // Additional fields from Firestore
    val universityId: String? = null,
    val universityName: String? = null,
    val jobId: String? = null,
    val jobTitle: String? = null,
    val data: Map<String, Any>? = null
)

enum class NotificationType {
    GENERAL,
    JOB_POSTED,
    INTERVIEW_SCHEDULED,
    APPLICATION_STATUS_CHANGE,
    NEW_JOB;
    
    companion object {
        @JvmStatic
        fun fromString(value: String): NotificationType {
            return when (value.lowercase()) {
                "new_job" -> NEW_JOB
                "job_posted" -> JOB_POSTED
                "interview_scheduled" -> INTERVIEW_SCHEDULED
                "application_status_change" -> APPLICATION_STATUS_CHANGE
                else -> GENERAL
            }
        }
    }
} 