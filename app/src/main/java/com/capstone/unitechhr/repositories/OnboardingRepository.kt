package com.capstone.unitechhr.repositories

import com.capstone.unitechhr.models.Onboarding
import com.capstone.unitechhr.models.OnboardingStatus
import com.capstone.unitechhr.models.OnboardingTask
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OnboardingRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val onboardingCollection = firestore.collection("onboarding")
    private val tasksCollection = firestore.collection("onboardingTasks")

    suspend fun getOnboardingProcesses(): List<Onboarding> = withContext(Dispatchers.IO) {
        try {
            val snapshot = onboardingCollection.get().await()
            return@withContext snapshot.toObjects(Onboarding::class.java)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    suspend fun getOnboardingById(id: String): Onboarding? = withContext(Dispatchers.IO) {
        try {
            val document = onboardingCollection.document(id).get().await()
            return@withContext document.toObject(Onboarding::class.java)
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun getOnboardingByEmployee(employeeId: String): Onboarding? = withContext(Dispatchers.IO) {
        try {
            val snapshot = onboardingCollection
                .whereEqualTo("employeeId", employeeId)
                .get()
                .await()
            
            val onboardings = snapshot.toObjects(Onboarding::class.java)
            return@withContext if (onboardings.isNotEmpty()) onboardings.first() else null
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun createOnboarding(onboarding: Onboarding): String? = withContext(Dispatchers.IO) {
        try {
            val documentRef = if (onboarding.id.isEmpty()) {
                onboardingCollection.document()
            } else {
                onboardingCollection.document(onboarding.id)
            }
            
            val onboardingWithId = if (onboarding.id.isEmpty()) {
                onboarding.copy(id = documentRef.id)
            } else {
                onboarding
            }
            
            documentRef.set(onboardingWithId).await()
            return@withContext documentRef.id
        } catch (e: Exception) {
            return@withContext null
        }
    }

    suspend fun updateOnboardingStatus(id: String, status: OnboardingStatus): Boolean = 
        withContext(Dispatchers.IO) {
            try {
                onboardingCollection.document(id)
                    .update("status", status)
                    .await()
                return@withContext true
            } catch (e: Exception) {
                return@withContext false
            }
        }

    suspend fun addTask(onboardingId: String, task: OnboardingTask): Boolean = 
        withContext(Dispatchers.IO) {
            try {
                val onboarding = getOnboardingById(onboardingId) ?: return@withContext false
                
                val taskRef = if (task.id.isEmpty()) {
                    tasksCollection.document()
                } else {
                    tasksCollection.document(task.id)
                }
                
                val taskWithId = if (task.id.isEmpty()) {
                    task.copy(id = taskRef.id)
                } else {
                    task
                }
                
                taskRef.set(taskWithId).await()
                
                val updatedTasks = onboarding.tasks.toMutableList()
                updatedTasks.add(taskWithId)
                
                onboardingCollection.document(onboardingId)
                    .update("tasks", updatedTasks)
                    .await()
                
                return@withContext true
            } catch (e: Exception) {
                return@withContext false
            }
        }

    suspend fun completeTask(onboardingId: String, taskId: String, completionDate: java.util.Date = java.util.Date()): Boolean = 
        withContext(Dispatchers.IO) {
            try {
                val onboarding = getOnboardingById(onboardingId) ?: return@withContext false
                
                val updatedTasks = onboarding.tasks.map { task ->
                    if (task.id == taskId) {
                        task.copy(isCompleted = true, completedDate = completionDate)
                    } else {
                        task
                    }
                }
                
                onboardingCollection.document(onboardingId)
                    .update("tasks", updatedTasks)
                    .await()
                
                // If all tasks are completed, update onboarding status
                if (updatedTasks.all { it.isCompleted }) {
                    updateOnboardingStatus(onboardingId, OnboardingStatus.COMPLETED)
                } else if (onboarding.status == OnboardingStatus.NOT_STARTED) {
                    updateOnboardingStatus(onboardingId, OnboardingStatus.IN_PROGRESS)
                }
                
                return@withContext true
            } catch (e: Exception) {
                return@withContext false
            }
        }
} 