package com.capstone.unitechhr.models

import java.util.Date

data class Onboarding(
    val id: String = "",
    val employeeId: String = "",
    val startDate: Date = Date(),
    val completionDeadline: Date? = null,
    val mentor: String = "",
    val status: OnboardingStatus = OnboardingStatus.NOT_STARTED,
    val tasks: List<OnboardingTask> = emptyList(),
    val notes: String = ""
)

data class OnboardingTask(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val category: TaskCategory = TaskCategory.GENERAL,
    val dueDate: Date? = null,
    val isCompleted: Boolean = false,
    val completedDate: Date? = null,
    val assignedTo: String = "" // Can be HR, manager, or employee themselves
)

enum class OnboardingStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    EXTENDED
}

enum class TaskCategory {
    GENERAL,
    PAPERWORK,
    TRAINING,
    IT_SETUP,
    INTRODUCTIONS,
    BENEFITS
} 