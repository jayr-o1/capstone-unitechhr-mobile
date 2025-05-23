package com.capstone.unitechhr.models

import java.util.Date

// Data class to represent the criteria weights
data class CriteriaWeights(
    val education: Int = 0,
    val skills: Int = 0,
    val experience: Int = 0
)

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
    val isDeleted: Boolean = false,
    
    // Added criteria weights field
    val criteriaWeights: CriteriaWeights? = null
) 