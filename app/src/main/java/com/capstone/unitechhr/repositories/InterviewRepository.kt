package com.capstone.unitechhr.repositories

import com.capstone.unitechhr.models.Interview
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import android.util.Log
import com.capstone.unitechhr.models.InterviewStatus
import java.util.Date

class InterviewRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val interviewsCollection = firestore.collection("interviews")

    suspend fun getInterviews(): List<Interview> = withContext(Dispatchers.IO) {
        try {
            val snapshot = interviewsCollection
                .orderBy("scheduledDate", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val interviews = snapshot.toObjects(Interview::class.java)
            Log.d("InterviewRepository", "getInterviews found ${interviews.size} total interviews")
            if (interviews.isNotEmpty()) {
                Log.d("InterviewRepository", "Sample applicantIds: ${interviews.take(3).map { it.applicantId }}")
            }
            
            return@withContext interviews
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Error in getInterviews: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun getInterviewsByApplicant(applicantId: String): List<Interview> = withContext(Dispatchers.IO) {
        try {
            Log.d("InterviewRepository", "Fetching interviews for applicantId: $applicantId")
            
            // Try with the provided applicant ID first
            val snapshot = interviewsCollection
                .whereEqualTo("applicantId", applicantId)
                .orderBy("scheduledDate", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val interviews = snapshot.toObjects(Interview::class.java)
            Log.d("InterviewRepository", "Found ${interviews.size} interviews with exact applicantId match")
            
            // If no results and it looks like a sanitized email, try with the original format
            if (interviews.isEmpty() && applicantId.contains("-") && !applicantId.contains("@")) {
                try {
                    // Try to convert from sanitized to original format
                    val parts = applicantId.split("-")
                    if (parts.size >= 2) {
                        val possibleEmail = parts[0] + "@" + parts.subList(1, parts.size).joinToString(".")
                        Log.d("InterviewRepository", "Trying alternative email format: $possibleEmail")
                        
                        val alternativeSnapshot = interviewsCollection
                            .whereEqualTo("applicantId", possibleEmail)
                            .orderBy("scheduledDate", Query.Direction.ASCENDING)
                            .get()
                            .await()
                        
                        val altInterviews = alternativeSnapshot.toObjects(Interview::class.java)
                        Log.d("InterviewRepository", "Found ${altInterviews.size} interviews with original email format")
                        return@withContext altInterviews
                    }
                } catch (e: Exception) {
                    Log.e("InterviewRepository", "Error trying alternative email format: ${e.message}")
                }
            }
            
            // If no results and it looks like an email, try with the sanitized format
            if (interviews.isEmpty() && applicantId.contains("@")) {
                try {
                    val sanitizedEmail = applicantId.replace("@", "-").replace(".", "-")
                    Log.d("InterviewRepository", "Trying sanitized email format: $sanitizedEmail")
                    
                    val alternativeSnapshot = interviewsCollection
                        .whereEqualTo("applicantId", sanitizedEmail)
                        .orderBy("scheduledDate", Query.Direction.ASCENDING)
                        .get()
                        .await()
                    
                    val altInterviews = alternativeSnapshot.toObjects(Interview::class.java)
                    Log.d("InterviewRepository", "Found ${altInterviews.size} interviews with sanitized email format")
                    return@withContext altInterviews
                } catch (e: Exception) {
                    Log.e("InterviewRepository", "Error trying sanitized email format: ${e.message}")
                }
            }
            
            // Try a more direct approach - just get all interviews and filter client-side
            if (interviews.isEmpty()) {
                Log.d("InterviewRepository", "No interviews found with standard formats, trying to get all interviews")
                val allInterviewsSnapshot = interviewsCollection
                    .orderBy("scheduledDate", Query.Direction.ASCENDING)
                    .get()
                    .await()
                
                val allInterviews = allInterviewsSnapshot.toObjects(Interview::class.java)
                Log.d("InterviewRepository", "Retrieved ${allInterviews.size} total interviews")
                
                // Look for any potential matches with variations of the applicant ID
                val emailPattern = if (applicantId.contains("@")) {
                    applicantId
                } else if (applicantId.contains("-")) {
                    val parts = applicantId.split("-")
                    parts.getOrNull(0)?.let { username ->
                        "$username@"
                    } ?: ""
                } else ""
                
                if (emailPattern.isNotEmpty()) {
                    val matchingInterviews = allInterviews.filter { 
                        it.applicantId.contains(emailPattern, ignoreCase = true) ||
                        it.applicantId.contains(applicantId, ignoreCase = true)
                    }
                    
                    Log.d("InterviewRepository", "Found ${matchingInterviews.size} interviews with partial matching: $emailPattern or $applicantId")
                    if (matchingInterviews.isNotEmpty()) {
                        return@withContext matchingInterviews
                    }
                }
            }
            
            return@withContext interviews
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Error getting interviews by applicant: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun getInterviewById(id: String): Interview? = withContext(Dispatchers.IO) {
        try {
            val document = interviewsCollection.document(id).get().await()
            return@withContext document.toObject(Interview::class.java)
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun scheduleInterview(interview: Interview): Boolean = withContext(Dispatchers.IO) {
        try {
            val documentRef = if (interview.id.isEmpty()) {
                interviewsCollection.document()
            } else {
                interviewsCollection.document(interview.id)
            }
            documentRef.set(interview).await()
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun updateInterviewStatus(id: String, status: com.capstone.unitechhr.models.InterviewStatus): Boolean = 
        withContext(Dispatchers.IO) {
            try {
                interviewsCollection.document(id)
                    .update("status", status)
                    .await()
                return@withContext true
            } catch (e: Exception) {
                return@withContext false
            }
        }

    suspend fun deleteInterview(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            interviewsCollection.document(id).delete().await()
            return@withContext true
        } catch (e: Exception) {
            return@withContext false
        }
    }

    suspend fun getAllInterviewsForUser(email: String): List<Interview> = withContext(Dispatchers.IO) {
        try {
            Log.d("InterviewRepository", "Fetching ALL interviews for email: $email")
            val sanitizedEmail = email.replace("@", "-").replace(".", "-")
            val username = email.split("@").firstOrNull() ?: ""
            
            // Get all interviews
            val snapshot = interviewsCollection
                .orderBy("scheduledDate", Query.Direction.ASCENDING)
                .get()
                .await()
            
            val allInterviews = snapshot.toObjects(Interview::class.java)
            Log.d("InterviewRepository", "Found ${allInterviews.size} total interviews")
            
            // Try to find any that might match our user
            val matchingInterviews = allInterviews.filter { interview ->
                val applicantId = interview.applicantId
                applicantId == email ||
                applicantId == sanitizedEmail ||
                (applicantId.contains("@") && applicantId.startsWith(username)) ||
                (applicantId.contains("-") && applicantId.startsWith(username))
            }
            
            Log.d("InterviewRepository", "Found ${matchingInterviews.size} interviews matching user email in any format")
            if (matchingInterviews.isNotEmpty()) {
                Log.d("InterviewRepository", "Found interviews with applicantIds: ${matchingInterviews.map { it.applicantId }}")
            }
            
            return@withContext matchingInterviews
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Error fetching all interviews for user: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun getInterviewsFromApplicantSubcollection(email: String): List<Interview> = withContext(Dispatchers.IO) {
        try {
            Log.d("InterviewRepository", "Fetching interviews from applicant subcollection for: $email")
            val sanitizedEmail = email.replace("@", "-").replace(".", "-")
            val results = mutableListOf<Interview>()
            
            // First, we need to query all universities
            val universities = firestore.collection("universities").get().await()
            
            Log.d("InterviewRepository", "Found ${universities.documents.size} universities to check")
            
            // For each university, check all jobs
            for (universityDoc in universities.documents) {
                val universityId = universityDoc.id
                
                // Query all jobs for this university
                val jobs = firestore.collection("universities")
                    .document(universityId)
                    .collection("jobs")
                    .get()
                    .await()
                
                Log.d("InterviewRepository", "University $universityId has ${jobs.documents.size} jobs")
                
                // For each job, check if our applicant exists
                for (jobDoc in jobs.documents) {
                    val jobId = jobDoc.id
                    
                    // Look for the applicant by sanitized email
                    val applicantDocRef = firestore.collection("universities")
                        .document(universityId)
                        .collection("jobs")
                        .document(jobId)
                        .collection("applicants")
                        .document(sanitizedEmail)
                    
                    val applicantDoc = applicantDocRef.get().await()
                    
                    // If applicant exists, check for interviews
                    if (applicantDoc.exists()) {
                        Log.d("InterviewRepository", "Found applicant $sanitizedEmail for job $jobId in university $universityId")
                        
                        // Check for an interviews subcollection
                        val interviews = applicantDocRef
                            .collection("interviews")
                            .get()
                            .await()
                        
                        Log.d("InterviewRepository", "Found ${interviews.documents.size} interviews for this applicant")
                        
                        // Convert each document to an Interview object and add to results
                        for (interviewDoc in interviews.documents) {
                            try {
                                // Manual conversion instead of using toObject() since there's a case mismatch
                                val id = interviewDoc.id
                                val data = interviewDoc.data
                                
                                if (data != null) {
                                    // Extract fields from the document data
                                    val interviewerId = data["interviewer"] as? String ?: ""
                                    val dateTimeStr = data["dateTime"] as? String ?: ""
                                    val title = data["title"] as? String ?: ""
                                    val statusString = data["status"] as? String ?: "Scheduled"
                                    
                                    // Convert dateTime string to Date object
                                    val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm", java.util.Locale.getDefault())
                                    val scheduledDate = try {
                                        if (dateTimeStr.isNotEmpty()) {
                                            dateFormat.parse(dateTimeStr)
                                        } else {
                                            null
                                        }
                                    } catch (e: Exception) {
                                        Log.e("InterviewRepository", "Error parsing dateTime: $dateTimeStr", e)
                                        null
                                    } ?: java.util.Date()
                                    
                                    // Convert status string to enum
                                    val status = when(statusString.lowercase()) {
                                        "scheduled" -> InterviewStatus.SCHEDULED
                                        "completed" -> InterviewStatus.COMPLETED
                                        "cancelled" -> InterviewStatus.CANCELLED
                                        "rescheduled" -> InterviewStatus.RESCHEDULED
                                        else -> InterviewStatus.SCHEDULED
                                    }
                                    
                                    // Create interview object with available data
                                    val interview = Interview(
                                        id = id,
                                        applicantId = sanitizedEmail,
                                        jobId = jobId,
                                        interviewerIds = listOf(interviewerId),
                                        scheduledDate = scheduledDate,
                                        status = status,
                                        // Set other fields to defaults or extract if available
                                        duration = 60,
                                        location = "Virtual",
                                        meetingLink = "",
                                        notes = title // Store the title in the notes field since Interview model doesn't have title
                                    )
                                    
                                    results.add(interview)
                                    Log.d("InterviewRepository", "Successfully mapped interview: $id with status $statusString to $status")
                                }
                            } catch (e: Exception) {
                                Log.e("InterviewRepository", "Error mapping interview document: ${e.message}")
                            }
                        }
                    }
                }
            }
            
            Log.d("InterviewRepository", "Total interviews found in subcollections: ${results.size}")
            if (results.isNotEmpty()) {
                Log.d("InterviewRepository", "Interview IDs: ${results.map { it.id }}")
            }
            
            return@withContext results
        } catch (e: Exception) {
            Log.e("InterviewRepository", "Error getting interviews from subcollections: ${e.message}", e)
            return@withContext emptyList()
        }
    }
} 