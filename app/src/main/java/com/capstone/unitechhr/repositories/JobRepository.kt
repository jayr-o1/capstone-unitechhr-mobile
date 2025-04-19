package com.capstone.unitechhr.repositories

import com.capstone.unitechhr.models.Job
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class JobRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val jobsCollection = firestore.collection("jobs")
    
    suspend fun getJobs(): List<Job> = withContext(Dispatchers.IO) {
        try {
            val snapshot = jobsCollection
                .orderBy("postedDate", Query.Direction.DESCENDING)
                .get()
                .await()
            return@withContext snapshot.toObjects(Job::class.java)
        } catch (e: Exception) {
            // Handle errors
            return@withContext emptyList()
        }
    }
    
    suspend fun getJobById(id: String): Job? = withContext(Dispatchers.IO) {
        try {
            val document = jobsCollection.document(id).get().await()
            return@withContext document.toObject(Job::class.java)
        } catch (e: Exception) {
            // Handle errors
            return@withContext null
        }
    }
    
    suspend fun searchJobs(query: String): List<Job> = withContext(Dispatchers.IO) {
        try {
            // Simple search implementation - in a real app, you might use Firestore's
            // where clauses or a more sophisticated search strategy
            val allJobs = getJobs()
            return@withContext allJobs.filter { job ->
                job.title.contains(query, ignoreCase = true) ||
                job.company.contains(query, ignoreCase = true) ||
                job.description.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
    
    suspend fun addJob(job: Job): Boolean = withContext(Dispatchers.IO) {
        try {
            val documentRef = if (job.id.isEmpty()) {
                jobsCollection.document()
            } else {
                jobsCollection.document(job.id)
            }
            documentRef.set(job).await()
            return@withContext true
        } catch (e: Exception) {
            // Handle errors
            return@withContext false
        }
    }
} 