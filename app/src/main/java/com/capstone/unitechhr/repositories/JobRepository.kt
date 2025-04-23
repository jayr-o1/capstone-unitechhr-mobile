package com.capstone.unitechhr.repositories

import android.util.Log
import com.capstone.unitechhr.models.Job
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class JobRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val universitiesCollection = firestore.collection("universities")
    
    suspend fun getJobs(): List<Job> = withContext(Dispatchers.IO) {
        try {
            // Get all universities
            val universitiesSnapshot = universitiesCollection.get().await()
            val allJobs = mutableListOf<Job>()
            
            // For each university, get its jobs subcollection
            for (universityDoc in universitiesSnapshot.documents) {
                val universityId = universityDoc.id
                val universityName = universityDoc.getString("name") ?: ""
                
                // Get jobs subcollection for this university
                val jobsSnapshot = universitiesCollection.document(universityId)
                    .collection("jobs")
                    .whereEqualTo("isDeleted", false)
                .get()
                .await()
                
                // Convert documents to Job objects and add university details
                for (jobDoc in jobsSnapshot.documents) {
                    try {
                        val job = convertDocumentToJob(jobDoc, universityId, universityName)
                        if (job != null) {
                            allJobs.add(job)
                        }
                    } catch (e: Exception) {
                        Log.e("JobRepository", "Error parsing job document: ${e.message}")
                    }
                }
            }
            
            return@withContext allJobs.sortedByDescending { it.postedDate }
        } catch (e: Exception) {
            Log.e("JobRepository", "Error getting all jobs: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    suspend fun getJobsByUniversity(universityId: String): List<Job> = withContext(Dispatchers.IO) {
        try {
            // Get university name
            val universityDoc = universitiesCollection.document(universityId).get().await()
            val universityName = universityDoc.getString("name") ?: ""
            
            // Get jobs subcollection for this university
            val jobsSnapshot = universitiesCollection.document(universityId)
                .collection("jobs")
                .whereEqualTo("isDeleted", false)
                .get()
                .await()
            
            // Convert documents to Job objects
            val jobs = mutableListOf<Job>()
            for (jobDoc in jobsSnapshot.documents) {
                try {
                    val job = convertDocumentToJob(jobDoc, universityId, universityName)
                    if (job != null) {
                        jobs.add(job)
                    }
                } catch (e: Exception) {
                    Log.e("JobRepository", "Error parsing job document: ${e.message}")
                }
            }
            
            return@withContext jobs.sortedByDescending { it.postedDate }
        } catch (e: Exception) {
            Log.e("JobRepository", "Error getting jobs for university $universityId: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    suspend fun getJobById(universityId: String, jobId: String): Job? = withContext(Dispatchers.IO) {
        try {
            // Get university name
            val universityDoc = universitiesCollection.document(universityId).get().await()
            val universityName = universityDoc.getString("name") ?: ""
            
            // Get job document
            val jobDoc = universitiesCollection.document(universityId)
                .collection("jobs")
                .document(jobId)
                .get()
                .await()
            
            return@withContext convertDocumentToJob(jobDoc, universityId, universityName)
        } catch (e: Exception) {
            Log.e("JobRepository", "Error getting job $jobId: ${e.message}")
            return@withContext null
        }
    }
    
    suspend fun getJobById(jobId: String): Job? = withContext(Dispatchers.IO) {
        try {
            // Get all universities to search for this job
            val universitiesSnapshot = universitiesCollection.get().await()
            
            // Search through all universities for this job
            for (universityDoc in universitiesSnapshot.documents) {
                val universityId = universityDoc.id
                val universityName = universityDoc.getString("name") ?: ""
                
                // Check if this job exists in this university
                val jobDoc = universitiesCollection.document(universityId)
                    .collection("jobs")
                    .document(jobId)
                    .get()
                    .await()
                
                if (jobDoc.exists()) {
                    return@withContext convertDocumentToJob(jobDoc, universityId, universityName)
                }
            }
            
            // Job not found in any university
            Log.e("JobRepository", "Job $jobId not found in any university")
            return@withContext null
        } catch (e: Exception) {
            Log.e("JobRepository", "Error finding job $jobId across universities: ${e.message}")
            return@withContext null
        }
    }
    
    suspend fun searchJobs(query: String): List<Job> = withContext(Dispatchers.IO) {
        try {
            // Get all jobs first
            val allJobs = getJobs()
            
            // Filter by search query
            return@withContext allJobs.filter { job ->
                job.title.contains(query, ignoreCase = true) ||
                job.department?.contains(query, ignoreCase = true) ?: false ||
                job.summary?.contains(query, ignoreCase = true) ?: false ||
                job.universityName.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e("JobRepository", "Error searching jobs: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    suspend fun searchJobsByUniversity(query: String, universityId: String): List<Job> = withContext(Dispatchers.IO) {
        try {
            // Get jobs for the specified university
            val universityJobs = getJobsByUniversity(universityId)
            
            // Filter by search query
            return@withContext universityJobs.filter { job ->
                job.title.contains(query, ignoreCase = true) ||
                job.department?.contains(query, ignoreCase = true) ?: false ||
                job.summary?.contains(query, ignoreCase = true) ?: false
            }
        } catch (e: Exception) {
            Log.e("JobRepository", "Error searching jobs by university: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    // Helper function to convert Firestore document to Job model
    private fun convertDocumentToJob(document: com.google.firebase.firestore.DocumentSnapshot, universityId: String, universityName: String): Job? {
        if (!document.exists()) return null
        
        try {
            // Extract job data from document
            val id = document.id
            val title = document.getString("title") ?: ""
            val department = document.getString("department") ?: ""
            val summary = document.getString("summary") ?: ""
            val salary = document.getString("salary") ?: ""
            val status = document.getString("status") ?: ""
            val workSetup = document.getString("workSetup") ?: ""
            val postedDate = document.getTimestamp("datePosted")?.toDate() ?: Date()
            val availableSlots = document.getLong("availableSlots")?.toInt()
            
            // Get arrays
            val essentialSkills = document.get("essentialSkills") as? List<String> ?: emptyList()
            val keyDuties = document.get("keyDuties") as? List<String> ?: emptyList()
            val qualifications = document.get("qualifications") as? List<String> ?: emptyList()
            
            // Format duties and qualifications for display
            val dutiesText = keyDuties.joinToString("\n• ", "• ")
            val qualificationsText = qualifications.joinToString("\n• ", "• ")
            
            // Create Job object with all fields populated
            return Job(
                id = id,
                title = title,
                company = department, // Using department as company
                location = workSetup, // Using workSetup as location
                salary = salary,
                jobType = status, // Using status as jobType
                description = summary, // Just using summary as description
                requirements = qualificationsText, // Still keeping the formatted requirements
                postedDate = postedDate,
                universityId = universityId,
                universityName = universityName,
                department = department,
                summary = summary,
                status = status,
                workSetup = workSetup,
                availableSlots = availableSlots,
                essentialSkills = essentialSkills,
                keyDuties = keyDuties,
                qualifications = qualifications,
                isDeleted = document.getBoolean("isDeleted") ?: false
            )
        } catch (e: Exception) {
            Log.e("JobRepository", "Error converting document to Job: ${e.message}")
            return null
        }
    }
} 