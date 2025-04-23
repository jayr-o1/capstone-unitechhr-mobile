package com.capstone.unitechhr.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.models.OnboardingChecklistTask
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EmployeeOnboardingViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val TAG = "EmployeeOnboardingVM"
    
    private val _tasks = MutableLiveData<List<OnboardingChecklistTask>>()
    val tasks: LiveData<List<OnboardingChecklistTask>> = _tasks
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _progress = MutableLiveData<Int>()
    val progress: LiveData<Int> = _progress
    
    // Function to load onboarding tasks for a specific employee
    fun loadOnboardingTasks(employeeId: String, collectionPath: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d(TAG, "Loading onboarding tasks from path: $collectionPath")
                
                // Remove leading slash if present to avoid double slashes
                val cleanPath = if (collectionPath.startsWith("/")) {
                    collectionPath.substring(1)
                } else {
                    collectionPath
                }
                
                // Check if path matches the university/jobs/applicants pattern
                val isUniversityJobPath = cleanPath.contains("/universities/") && 
                                        cleanPath.contains("/jobs/") && 
                                        cleanPath.contains("/applicants/")
                
                Log.d(TAG, "Is university job applicant path: $isUniversityJobPath")
                
                Log.d(TAG, "Using document path: $cleanPath")
                val document = firestore.document(cleanPath).get().await()
                Log.d(TAG, "Document exists: ${document.exists()}")
                
                if (document.exists()) {
                    // Get onboardingChecklist array from document
                    @Suppress("UNCHECKED_CAST")
                    val onboardingChecklist = document.get("onboardingChecklist") as? List<Map<String, Any>>
                    
                    Log.d(TAG, "Onboarding checklist retrieved: ${onboardingChecklist != null}, size: ${onboardingChecklist?.size ?: 0}")
                    
                    if (onboardingChecklist != null) {
                        // Convert to our model
                        val tasksList = onboardingChecklist.map { taskMap ->
                            val id = (taskMap["id"] as? Number)?.toLong() ?: 0
                            val taskText = taskMap["task"] as? String ?: ""
                            val completed = taskMap["completed"] as? Boolean ?: false
                            
                            Log.d(TAG, "Task found: $id, $taskText, completed: $completed")
                            
                            OnboardingChecklistTask(
                                id = id,
                                task = taskText,
                                completed = completed
                            )
                        }
                        
                        _tasks.value = tasksList
                        updateProgress(tasksList)
                        _errorMessage.value = null
                    } else {
                        Log.e(TAG, "Onboarding checklist array not found in document")
                        
                        if (isUniversityJobPath) {
                            // For university/job/applicant paths, create a default onboarding checklist
                            Log.d(TAG, "Creating default onboarding checklist for university job applicant")
                            
                            val defaultChecklist = createDefaultChecklist()
                            _tasks.value = defaultChecklist
                            updateProgress(defaultChecklist)
                            
                            // Save the default checklist to Firestore
                            try {
                                val checklistData = defaultChecklist.map { task ->
                                    mapOf(
                                        "id" to task.id,
                                        "task" to task.task,
                                        "completed" to task.completed
                                    )
                                }
                                
                                // Get the current document data to determine if we should use set or update
                                val documentData = document.data
                                
                                if (documentData != null) {
                                    // If document exists, use update to add the onboardingChecklist field
                                    firestore.document(cleanPath)
                                        .update("onboardingChecklist", checklistData)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "Default checklist added to existing document via update")
                                        }
                                        .addOnFailureListener { e ->
                                            // If update fails (e.g., permission denied), try using set with merge
                                            Log.e(TAG, "Error updating document with checklist: ${e.message}")
                                            Log.d(TAG, "Attempting to set with merge instead...")
                                            
                                            val updatedData = hashMapOf<String, Any>(
                                                "onboardingChecklist" to checklistData
                                            )
                                            
                                            firestore.document(cleanPath)
                                                .set(updatedData, com.google.firebase.firestore.SetOptions.merge())
                                                .addOnSuccessListener {
                                                    Log.d(TAG, "Default checklist added via set with merge")
                                                }
                                                .addOnFailureListener { e2 ->
                                                    Log.e(TAG, "Error setting document with merge: ${e2.message}")
                                                }
                                        }
                                } else {
                                    // If document doesn't exist, create it with the checklist
                                    val newDocData = hashMapOf<String, Any>(
                                        "onboardingChecklist" to checklistData,
                                        "createdAt" to com.google.firebase.Timestamp.now(),
                                        "userId" to employeeId
                                    )
                                    
                                    firestore.document(cleanPath)
                                        .set(newDocData)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "New document created with default checklist")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error creating document with checklist: ${e.message}")
                                        }
                                }
                                
                            } catch (e: Exception) {
                                Log.e(TAG, "Error creating default checklist: ${e.message}")
                            }
                        } else {
                            _errorMessage.value = "Onboarding checklist not found"
                            _tasks.value = emptyList()
                        }
                    }
                } else {
                    Log.e(TAG, "Document not found at path: $cleanPath")
                    
                    if (isUniversityJobPath) {
                        // For university/job/applicant paths, create a new document with a default checklist
                        Log.d(TAG, "Creating new document with default checklist at path: $cleanPath")
                        
                        val defaultChecklist = createDefaultChecklist()
                        _tasks.value = defaultChecklist
                        updateProgress(defaultChecklist)
                        
                        // Create a new document with the default checklist
                        try {
                            val checklistData = defaultChecklist.map { task ->
                                mapOf(
                                    "id" to task.id,
                                    "task" to task.task,
                                    "completed" to task.completed
                                )
                            }
                            
                            val newDocData = hashMapOf<String, Any>(
                                "onboardingChecklist" to checklistData,
                                "createdAt" to com.google.firebase.Timestamp.now(),
                                "userId" to employeeId
                            )
                            
                            firestore.document(cleanPath)
                                .set(newDocData)
                                .addOnSuccessListener {
                                    Log.d(TAG, "New document created with default checklist")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error creating document with checklist: ${e.message}")
                                }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error creating new document with checklist: ${e.message}")
                        }
                    } else {
                        _errorMessage.value = "Document not found"
                        _tasks.value = emptyList()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading tasks: ${e.message}", e)
                _errorMessage.value = "Failed to load onboarding tasks: ${e.message}"
                _tasks.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun createDefaultChecklist(): List<OnboardingChecklistTask> {
        return listOf(
            OnboardingChecklistTask(id = 1, task = "Complete employment paperwork", completed = false),
            OnboardingChecklistTask(id = 2, task = "Set up company email account", completed = false),
            OnboardingChecklistTask(id = 3, task = "Tour of office facilities", completed = false),
            OnboardingChecklistTask(id = 4, task = "IT systems access setup", completed = false),
            OnboardingChecklistTask(id = 5, task = "Meet with HR for benefits enrollment", completed = false),
            OnboardingChecklistTask(id = 6, task = "Attend company orientation", completed = false),
            OnboardingChecklistTask(id = 7, task = "Introduction to department team", completed = false),
            OnboardingChecklistTask(id = 8, task = "Review company policies", completed = false),
            OnboardingChecklistTask(id = 9, task = "Set up workstation", completed = false),
            OnboardingChecklistTask(id = 10, task = "First project assignment", completed = false)
        )
    }
    
    // Function to update a task status
    fun updateTaskStatus(
        employeeId: String,
        taskId: Long,
        isCompleted: Boolean,
        collectionPath: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Remove leading slash if present
                val cleanPath = if (collectionPath.startsWith("/")) {
                    collectionPath.substring(1)
                } else {
                    collectionPath
                }
                
                Log.d(TAG, "Updating task status in document: $cleanPath")
                val docRef = firestore.document(cleanPath)
                
                // Get current tasks
                val document = docRef.get().await()
                
                if (document.exists()) {
                    @Suppress("UNCHECKED_CAST")
                    val onboardingChecklist = document.get("onboardingChecklist") as? List<Map<String, Any>> ?: emptyList()
                    
                    Log.d(TAG, "Updating task $taskId to completed: $isCompleted")
                    
                    // Update the specific task
                    val updatedChecklist = onboardingChecklist.map { taskMap ->
                        val currentId = (taskMap["id"] as? Number)?.toLong() ?: 0
                        if (currentId == taskId) {
                            // Create a new map with updated completed status
                            mapOf(
                                "id" to currentId,
                                "task" to (taskMap["task"] as? String ?: ""),
                                "completed" to isCompleted
                            )
                        } else {
                            taskMap
                        }
                    }
                    
                    // Update Firestore
                    docRef.update("onboardingChecklist", updatedChecklist).await()
                    Log.d(TAG, "Successfully updated task in Firestore")
                    
                    // Update local data
                    val currentTasks = _tasks.value?.toMutableList() ?: mutableListOf()
                    val updatedTasks = currentTasks.map { task ->
                        if (task.id == taskId) {
                            task.copy(completed = isCompleted)
                        } else {
                            task
                        }
                    }
                    
                    _tasks.value = updatedTasks
                    updateProgress(updatedTasks)
                    _errorMessage.value = null
                } else {
                    Log.e(TAG, "Document not found at: $cleanPath")
                    _errorMessage.value = "Document not found"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating task: ${e.message}", e)
                _errorMessage.value = "Failed to update task: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private fun updateProgress(tasks: List<OnboardingChecklistTask>) {
        if (tasks.isEmpty()) {
            _progress.value = 0
            return
        }
        
        val completedTasks = tasks.count { it.completed }
        val progressPercentage = (completedTasks * 100) / tasks.size
        _progress.value = progressPercentage
        Log.d(TAG, "Progress updated to $progressPercentage%")
    }
} 