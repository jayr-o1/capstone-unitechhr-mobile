package com.capstone.unitechhr.repositories

import com.capstone.unitechhr.models.University
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UniversityRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val universitiesCollection = firestore.collection("universities")
    
    suspend fun getUniversities(): List<University> = withContext(Dispatchers.IO) {
        try {
            val snapshot = universitiesCollection
                .orderBy("name")
                .get()
                .await()
            return@withContext snapshot.toObjects(University::class.java)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }
    
    suspend fun getUniversityById(id: String): University? = withContext(Dispatchers.IO) {
        try {
            val document = universitiesCollection.document(id).get().await()
            return@withContext document.toObject(University::class.java)
        } catch (e: Exception) {
            return@withContext null
        }
    }
    
    suspend fun getUniversityByCode(code: String): University? = withContext(Dispatchers.IO) {
        try {
            val snapshot = universitiesCollection
                .whereEqualTo("code", code)
                .get()
                .await()
            
            val universities = snapshot.toObjects(University::class.java)
            return@withContext universities.firstOrNull()
        } catch (e: Exception) {
            return@withContext null
        }
    }
} 