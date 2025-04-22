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
                        _errorMessage.value = "Onboarding checklist not found"
                        _tasks.value = emptyList()
                    }
                } else {
                    Log.e(TAG, "Document not found at path: $cleanPath")
                    _errorMessage.value = "Document not found"
                    _tasks.value = emptyList()
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