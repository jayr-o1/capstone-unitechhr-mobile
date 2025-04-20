package com.capstone.unitechhr.models

import java.util.Date

data class Notification(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Date = Date(),
    val isRead: Boolean = false,
    val type: NotificationType = NotificationType.GENERAL,
    val relatedItemId: String = "" // ID of related job/interview/application
)

enum class NotificationType {
    GENERAL,
    JOB_POSTED,
    INTERVIEW_SCHEDULED,
    APPLICATION_STATUS_CHANGE
} 