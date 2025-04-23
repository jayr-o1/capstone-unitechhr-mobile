package com.capstone.unitechhr.models

import android.util.Log
import java.util.Date

data class JobApplication(
    val id: String = "",
    val jobId: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val location: String = "",
    val status: JobApplicationStatus = JobApplicationStatus.SUBMITTED,
    val appliedDate: Date = Date(),
    val lastUpdated: Date? = null,
    val rawStatus: String? = null
)

enum class JobApplicationStatus {
    SUBMITTED,
    REVIEWING,
    UNDER_REVIEW,
    INTERVIEW_SCHEDULED,
    INTERVIEW_COMPLETED,
    OFFERED,
    HIRED,
    REJECTED,
    PENDING,
    IN_ONBOARDING; // Add onboarding status

    companion object {
        private const val TAG = "JobApplicationStatus"
        
        /**
         * Converts a string to JobApplicationStatus more flexibly
         */
        fun fromString(statusStr: String?): JobApplicationStatus {
            if (statusStr == null || statusStr.isEmpty()) {
                return SUBMITTED
            }
            
            // Log the raw status string for debugging
            Log.d(TAG, "Converting status string: '$statusStr'")
            
            // Try direct conversion with case-insensitivity
            val normalizedStatus = statusStr.uppercase().trim()
            Log.d(TAG, "Normalized status string: '$normalizedStatus'")
            
            // First try exact match ignoring case
            return try {
                // Try direct enum conversion first
                valueOf(normalizedStatus)
            } catch (e: IllegalArgumentException) {
                // If that fails, check if it's a simple lowercase match
                values().firstOrNull { it.name.equals(statusStr, ignoreCase = true) }
                    ?: when {
                        // Match based on containing text
                        normalizedStatus.contains("SUBMIT", ignoreCase = true) -> SUBMITTED
                        normalizedStatus.contains("UNDER REVIEW", ignoreCase = true) -> UNDER_REVIEW
                        normalizedStatus.contains("REVIEW", ignoreCase = true) -> REVIEWING
                        normalizedStatus.contains("PEND", ignoreCase = true) -> PENDING
                        normalizedStatus.contains("WAIT", ignoreCase = true) -> PENDING
                        normalizedStatus.contains("INTERVIEW") && 
                            (normalizedStatus.contains("SCHEDULE") || normalizedStatus.contains("SET")) -> INTERVIEW_SCHEDULED
                        normalizedStatus.contains("INTERVIEW") && 
                            (normalizedStatus.contains("COMPLETE") || normalizedStatus.contains("DONE")) -> INTERVIEW_COMPLETED
                        normalizedStatus.contains("OFFER", ignoreCase = true) -> OFFERED
                        normalizedStatus.contains("HIRE", ignoreCase = true) -> HIRED
                        normalizedStatus.contains("ACCEPT", ignoreCase = true) -> HIRED
                        normalizedStatus.contains("REJECT", ignoreCase = true) -> REJECTED
                        normalizedStatus.contains("DECLINE", ignoreCase = true) -> REJECTED
                        normalizedStatus.contains("ONBOARD", ignoreCase = true) -> IN_ONBOARDING
                        
                        // Check for exact string matches that might be used in Firestore
                        statusStr == "In Onboarding" -> IN_ONBOARDING
                        statusStr == "Onboarding" -> IN_ONBOARDING
                        statusStr == "In Progress" -> IN_ONBOARDING
                        statusStr == "Pending" -> PENDING
                        statusStr == "Reviewing" -> REVIEWING
                        statusStr == "Under Review" -> UNDER_REVIEW
                        statusStr == "Submitted" -> SUBMITTED
                        statusStr == "Interview Scheduled" -> INTERVIEW_SCHEDULED
                        statusStr == "Interview Completed" -> INTERVIEW_COMPLETED
                        statusStr == "Offered" -> OFFERED
                        statusStr == "Hired" -> HIRED
                        statusStr == "Rejected" -> REJECTED
                        
                        // Log and return default if nothing matched
                        else -> {
                            Log.d(TAG, "Could not match status: '$normalizedStatus', defaulting to PENDING")
                            PENDING
                        }
                    }
            }
        }

        /**
         * Gets a friendly display status from a Firestore document
         */
        fun getStatusDisplay(statusString: String?): String {
            if (statusString.isNullOrEmpty()) {
                return "Pending" // Default if no status
            }
            
            // Check if it's already in a reasonable display format (first letter capitalized, no underscores)
            if (statusString.matches(Regex("[A-Z][a-z]+(\\s[A-Z][a-z]+)*"))) {
                return statusString // Already in proper display format
            }
            
            // Otherwise, convert it to a nice format
            return try {
                // Try to convert to enum and then format
                val status = fromString(statusString)
                status.formatForDisplay()
            } catch (e: Exception) {
                Log.e(TAG, "Error formatting status string: $statusString", e)
                // Just do some basic formatting
                statusString.replace("_", " ")
                    .split(" ")
                    .joinToString(" ") { word ->
                        word.lowercase().replaceFirstChar { it.uppercase() }
                    }
            }
        }
    }
}

// Extension function to format status for display
fun JobApplicationStatus.formatForDisplay(): String {
    return when (this) {
        JobApplicationStatus.SUBMITTED -> "Submitted"
        JobApplicationStatus.REVIEWING -> "Reviewing"
        JobApplicationStatus.UNDER_REVIEW -> "Under Review"
        JobApplicationStatus.INTERVIEW_SCHEDULED -> "Interview Scheduled"
        JobApplicationStatus.INTERVIEW_COMPLETED -> "Interview Completed"
        JobApplicationStatus.OFFERED -> "Offered"
        JobApplicationStatus.HIRED -> "Hired"
        JobApplicationStatus.REJECTED -> "Rejected"
        JobApplicationStatus.PENDING -> "Pending"
        JobApplicationStatus.IN_ONBOARDING -> "In Onboarding"
    }
}

// Extension function to get a color based on status
fun JobApplicationStatus.getColorResourceId(): Int {
    return when (this) {
        JobApplicationStatus.SUBMITTED -> com.capstone.unitechhr.R.color.status_pending
        JobApplicationStatus.REVIEWING, 
        JobApplicationStatus.UNDER_REVIEW -> com.capstone.unitechhr.R.color.status_reviewing
        JobApplicationStatus.PENDING -> com.capstone.unitechhr.R.color.status_pending
        JobApplicationStatus.INTERVIEW_SCHEDULED,
        JobApplicationStatus.INTERVIEW_COMPLETED -> com.capstone.unitechhr.R.color.status_interview
        JobApplicationStatus.OFFERED -> com.capstone.unitechhr.R.color.status_hired
        JobApplicationStatus.HIRED -> com.capstone.unitechhr.R.color.status_hired
        JobApplicationStatus.REJECTED -> com.capstone.unitechhr.R.color.status_rejected
        JobApplicationStatus.IN_ONBOARDING -> com.capstone.unitechhr.R.color.status_hired
    }
} 