package com.capstone.unitechhr.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.models.Onboarding
import com.capstone.unitechhr.models.OnboardingStatus
import com.capstone.unitechhr.models.OnboardingTask
import com.capstone.unitechhr.models.TaskCategory
import com.capstone.unitechhr.repositories.OnboardingRepository
import kotlinx.coroutines.launch
import java.util.Date

class OnboardingViewModel : ViewModel() {
    private val repository = OnboardingRepository()
    
    private val _onboardingProcesses = MutableLiveData<List<Onboarding>>()
    val onboardingProcesses: LiveData<List<Onboarding>> = _onboardingProcesses
    
    private val _selectedOnboarding = MutableLiveData<Onboarding?>()
    val selectedOnboarding: LiveData<Onboarding?> = _selectedOnboarding
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _operationSuccessful = MutableLiveData<Boolean?>()
    val operationSuccessful: LiveData<Boolean?> = _operationSuccessful
    
    fun loadOnboardingProcesses() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val processes = repository.getOnboardingProcesses()
                _onboardingProcesses.value = processes
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load onboarding processes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getOnboardingById(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val onboarding = repository.getOnboardingById(id)
                _selectedOnboarding.value = onboarding
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load onboarding details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getOnboardingByEmployee(employeeId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val onboarding = repository.getOnboardingByEmployee(employeeId)
                _selectedOnboarding.value = onboarding
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load onboarding details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun createOnboarding(
        employeeId: String,
        startDate: Date,
        completionDeadline: Date? = null,
        mentor: String,
        tasks: List<OnboardingTask> = emptyList(),
        notes: String = ""
    ) {
        val newOnboarding = Onboarding(
            employeeId = employeeId,
            startDate = startDate,
            completionDeadline = completionDeadline,
            mentor = mentor,
            status = OnboardingStatus.NOT_STARTED,
            tasks = tasks,
            notes = notes
        )
        
        viewModelScope.launch {
            _isLoading.value = true
            _operationSuccessful.value = null
            
            try {
                val onboardingId = repository.createOnboarding(newOnboarding)
                if (onboardingId != null) {
                    _operationSuccessful.value = true
                    loadOnboardingProcesses()
                    getOnboardingById(onboardingId)
                } else {
                    _operationSuccessful.value = false
                    _errorMessage.value = "Failed to create onboarding process"
                }
            } catch (e: Exception) {
                _operationSuccessful.value = false
                _errorMessage.value = "Error creating onboarding: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateOnboardingStatus(id: String, status: OnboardingStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            _operationSuccessful.value = null
            
            try {
                val success = repository.updateOnboardingStatus(id, status)
                _operationSuccessful.value = success
                
                if (success) {
                    // Update local data
                    _selectedOnboarding.value?.let { onboarding ->
                        if (onboarding.id == id) {
                            _selectedOnboarding.value = onboarding.copy(status = status)
                        }
                    }
                    
                    _onboardingProcesses.value?.let { list ->
                        _onboardingProcesses.value = list.map {
                            if (it.id == id) it.copy(status = status) else it
                        }
                    }
                } else {
                    _errorMessage.value = "Failed to update onboarding status"
                }
            } catch (e: Exception) {
                _operationSuccessful.value = false
                _errorMessage.value = "Error updating status: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun addTask(
        onboardingId: String,
        title: String,
        description: String,
        category: TaskCategory,
        dueDate: Date? = null,
        assignedTo: String = ""
    ) {
        val task = OnboardingTask(
            title = title,
            description = description,
            category = category,
            dueDate = dueDate,
            assignedTo = assignedTo
        )
        
        viewModelScope.launch {
            _isLoading.value = true
            _operationSuccessful.value = null
            
            try {
                val success = repository.addTask(onboardingId, task)
                _operationSuccessful.value = success
                
                if (success) {
                    // Refresh data
                    getOnboardingById(onboardingId)
                } else {
                    _errorMessage.value = "Failed to add task"
                }
            } catch (e: Exception) {
                _operationSuccessful.value = false
                _errorMessage.value = "Error adding task: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun completeTask(onboardingId: String, taskId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _operationSuccessful.value = null
            
            try {
                val success = repository.completeTask(onboardingId, taskId)
                _operationSuccessful.value = success
                
                if (success) {
                    // Refresh data
                    getOnboardingById(onboardingId)
                } else {
                    _errorMessage.value = "Failed to complete task"
                }
            } catch (e: Exception) {
                _operationSuccessful.value = false
                _errorMessage.value = "Error completing task: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectOnboarding(onboarding: Onboarding) {
        _selectedOnboarding.value = onboarding
    }
    
    fun clearSelectedOnboarding() {
        _selectedOnboarding.value = null
    }
    
    fun clearOperationStatus() {
        _operationSuccessful.value = null
    }
} 