package com.capstone.unitechhr.models

import java.util.Date

data class Application(
    val id: String = "",
    val jobId: String = "",
    val userId: String = "",
    val jobTitle: String = "",
    val companyName: String = "",
    val applicationDate: Date = Date(),
    val status: ApplicationStatus = ApplicationStatus.PENDING,
    val resume: String = "", // Document reference or URL
    val coverLetter: String = "", // Document reference or URL
    val notes: String = ""
) 