package com.capstone.unitechhr.repositories

import com.capstone.unitechhr.models.Applicant
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ApplicantRepository {
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
            return@withContext emptyList()
        }
    }

    suspend fun getApplicantById(id: String): Applicant? = withContext(Dispatchers.IO) {
        try {
            val document = applicantsCollection.document(id).get().await()
            return@withContext document.toObject(Applicant::class.java)
        } catch (e: Exception) {
            // Handle errors
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
            return@withContext false
        }
    }

    suspend fun updateApplicant(applicant: Applicant): Boolean = withContext(Dispatchers.IO) {
        try {
            applicantsCollection.document(applicant.id).set(applicant).await()
            return@withContext true
        } catch (e: Exception) {
            // Handle errors
            return@withContext false
        }
    }

    suspend fun deleteApplicant(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            applicantsCollection.document(id).delete().await()
            return@withContext true
        } catch (e: Exception) {
            // Handle errors
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
            return@withContext emptyList()
        }
    }
} 