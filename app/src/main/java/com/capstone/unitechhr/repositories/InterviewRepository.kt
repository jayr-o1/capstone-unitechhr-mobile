package com.capstone.unitechhr.repositories

import com.capstone.unitechhr.models.Interview
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class InterviewRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val interviewsCollection = firestore.collection("interviews")

    suspend fun getInterviews(): List<Interview> = withContext(Dispatchers.IO) {
        try {
            val snapshot = interviewsCollection
                .orderBy("scheduledDate", Query.Direction.ASCENDING)
                .get()
                .await()
            return@withContext snapshot.toObjects(Interview::class.java)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun getInterviewsByApplicant(applicantId: String): List<Interview> = withContext(Dispatchers.IO) {
        try {
            val snapshot = interviewsCollection
                .whereEqualTo("applicantId", applicantId)
                .orderBy("scheduledDate", Query.Direction.ASCENDING)
                .get()
                .await()
            return@withContext snapshot.toObjects(Interview::class.java)
        } catch (e: Exception) {
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
} 