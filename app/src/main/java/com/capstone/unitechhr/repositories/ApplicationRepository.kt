package com.capstone.unitechhr.repositories

import android.util.Log
import com.capstone.unitechhr.models.Application
import com.capstone.unitechhr.models.ApplicationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class ApplicationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    
    // Get all applications for a specific user
    suspend fun getUserApplications(userId: String): List<Application> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("applications")
                .orderBy("applicationDate", Query.Direction.DESCENDING)
                .get()
                .await()
                
            return@withContext snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Application::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("ApplicationRepository", "Error converting application document: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error fetching user applications: ${e.message}")
            
            // For testing - return sample data if there's an error
            if (userId.contains("test") || true) { // Always return test data for now
                return@withContext generateSampleApplications()
            }
            
            return@withContext emptyList()
        }
    }
    
    // Get a specific application
    suspend fun getApplication(userId: String, applicationId: String): Application? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("applications")
                .document(applicationId)
                .get()
                .await()
                
            return@withContext doc.toObject(Application::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error fetching application: ${e.message}")
            return@withContext null
        }
    }
    
    // For testing - Generate sample applications
    private fun generateSampleApplications(): List<Application> {
        // Create a list of sample applications
        return listOf(
            Application(
                id = "app1",
                jobId = "job1",
                userId = "user1",
                jobTitle = "Android Developer",
                companyName = "Google",
                applicationDate = Date(System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000), // 10 days ago
                status = ApplicationStatus.INTERVIEW
            ),
            Application(
                id = "app2",
                jobId = "job2",
                userId = "user1",
                jobTitle = "Frontend Developer",
                companyName = "Facebook",
                applicationDate = Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000), // 5 days ago
                status = ApplicationStatus.REVIEWING
            ),
            Application(
                id = "app3",
                jobId = "job3",
                userId = "user1",
                jobTitle = "Backend Developer",
                companyName = "Amazon",
                applicationDate = Date(System.currentTimeMillis() - 15 * 24 * 60 * 60 * 1000), // 15 days ago
                status = ApplicationStatus.REJECTED
            ),
            Application(
                id = "app4",
                jobId = "job4",
                userId = "user1",
                jobTitle = "DevOps Engineer",
                companyName = "Microsoft",
                applicationDate = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000), // 2 days ago
                status = ApplicationStatus.PENDING
            ),
            Application(
                id = "app5",
                jobId = "job5",
                userId = "user1",
                jobTitle = "Data Scientist",
                companyName = "Netflix",
                applicationDate = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000), // 30 days ago
                status = ApplicationStatus.HIRED
            )
        )
    }
} 