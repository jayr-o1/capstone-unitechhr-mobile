package com.capstone.unitechhr.models

import java.util.Date

data class Applicant(
    val id: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val resume: String = "", // URL to resume file
    val appliedPosition: String = "",
    val applicationDate: Date = Date(),
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val skills: List<String> = emptyList(),
    val education: List<Education> = emptyList(),
    val experience: List<WorkExperience> = emptyList()
)

data class Education(
    val institution: String = "",
    val degree: String = "",
    val fieldOfStudy: String = "",
    val startDate: Date = Date(),
    val endDate: Date? = null,
    val isCurrentlyStudying: Boolean = false
)

data class WorkExperience(
    val company: String = "",
    val position: String = "",
    val description: String = "",
    val startDate: Date = Date(),
    val endDate: Date? = null,
    val isCurrentlyWorking: Boolean = false
) 