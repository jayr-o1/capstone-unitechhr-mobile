package com.capstone.unitechhr.repositories

import android.util.Log
import com.capstone.unitechhr.models.Applicant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ApplicantRepository {
    private val TAG = "ApplicantRepository"
    private val firestore = FirebaseFirestore.getInstance()
    private val applicantsCollection = firestore.collection("applicants")

    suspend fun getApplicants(): List<Applicant> = withContext(Dispatchers.IO) {
        try {
            val snapshot = applicantsCollection
                .orderBy("applicationDate", Query.Direction.DESCENDING)
                .get()
                .await()
            return@withContext snapshot.toObjects(Applicant::class.java)
        } catch (e: Exception) {
            // Handle errors
            Log.e(TAG, "Error fetching applicants: ${e.message}")
            return@withContext emptyList()
        }
    }

    suspend fun getApplicantById(id: String): Applicant? = withContext(Dispatchers.IO) {
        try {
            val document = applicantsCollection.document(id).get().await()
            return@withContext document.toObject(Applicant::class.java)
        } catch (e: Exception) {
            // Handle errors
            Log.e(TAG, "Error fetching applicant by ID: ${e.message}")
            return@withContext null
        }
    }

    suspend fun addApplicant(applicant: Applicant): Boolean = withContext(Dispatchers.IO) {
        try {
            val documentRef = if (applicant.id.isEmpty()) {
                applicantsCollection.document()
            } else {
                applicantsCollection.document(applicant.id)
            }
            documentRef.set(applicant).await()
            return@withContext true
        } catch (e: Exception) {
            // Handle errors
            Log.e(TAG, "Error adding applicant: ${e.message}")
            return@withContext false
        }
    }

    suspend fun updateApplicant(applicant: Applicant): Boolean = withContext(Dispatchers.IO) {
        try {
            applicantsCollection.document(applicant.id).set(applicant).await()
            return@withContext true
        } catch (e: Exception) {
            // Handle errors
            Log.e(TAG, "Error updating applicant: ${e.message}")
            return@withContext false
        }
    }

    suspend fun deleteApplicant(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            applicantsCollection.document(id).delete().await()
            return@withContext true
        } catch (e: Exception) {
            // Handle errors
            Log.e(TAG, "Error deleting applicant: ${e.message}")
            return@withContext false
        }
    }

    suspend fun searchApplicants(query: String): List<Applicant> = withContext(Dispatchers.IO) {
        try {
            val snapshot = applicantsCollection.get().await()
            val applicants = snapshot.toObjects(Applicant::class.java)
            
            // Filter applicants based on search query
            return@withContext applicants.filter {
                it.firstName.contains(query, ignoreCase = true) ||
                it.lastName.contains(query, ignoreCase = true) ||
                it.email.contains(query, ignoreCase = true) ||
                it.appliedPosition.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            // Handle errors
            Log.e(TAG, "Error searching applicants: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    /**
     * Update FCM token for an applicant
     */
    suspend fun updateFcmToken(applicantId: String, fcmToken: String): Boolean = withContext(Dispatchers.IO) {
        try {
            applicantsCollection.document(applicantId)
                .update("fcmToken", fcmToken)
                .await()
            Log.d(TAG, "FCM token updated for applicant: $applicantId")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token for applicant: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Check if notifications are enabled for an applicant
     */
    suspend fun isNotificationsEnabled(applicantId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = applicantsCollection.document(applicantId).get().await()
            return@withContext doc.getBoolean("notificationsEnabled") ?: true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking notification settings: ${e.message}")
            return@withContext true  // Default to enabled if error
        }
    }
    
    /**
     * Enable or disable notifications for an applicant
     */
    suspend fun setNotificationsEnabled(applicantId: String, enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            applicantsCollection.document(applicantId)
                .update("notificationsEnabled", enabled)
                .await()
            Log.d(TAG, "Notification settings updated for applicant: $applicantId to $enabled")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating notification settings: ${e.message}")
            return@withContext false
        }
    }
} 