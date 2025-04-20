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
    val universityName: String = "",
    
    // Additional fields from Firestore structure
    val department: String? = null,
    val summary: String? = null,
    val status: String? = null,
    val workSetup: String? = null,
    val availableSlots: Int? = null,
    val essentialSkills: List<String>? = null,
    val keyDuties: List<String>? = null,
    val qualifications: List<String>? = null,
    val isDeleted: Boolean = false
) 