package com.capstone.unitechhr.models

import java.util.Date

data class Job(
    val id: String = "",
    val title: String = "",
    val company: String = "",
    val location: String = "",
    val salary: String = "",
    val jobType: String = "",
    val description: String = "",
    val requirements: String = "",
    val postedDate: Date = Date(),
    val isFavorite: Boolean = false,
    val universityId: String = "",
    val universityName: String = ""
) 