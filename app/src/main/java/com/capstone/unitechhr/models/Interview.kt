package com.capstone.unitechhr.models

import java.util.Date

data class Interview(
    val id: String = "",
    val applicantId: String = "",
    val jobId: String = "",
    val interviewerIds: List<String> = emptyList(),
    val scheduledDate: Date = Date(),
    val duration: Int = 60, // in minutes
    val location: String = "", // can be physical address or "Virtual"
    val meetingLink: String = "", // for virtual interviews
    val notes: String = "",
    val status: InterviewStatus = InterviewStatus.SCHEDULED
)

enum class InterviewStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
    RESCHEDULED
} 