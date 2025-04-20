package com.capstone.unitechhr.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.models.Application
import com.capstone.unitechhr.repositories.ApplicationRepository
import kotlinx.coroutines.launch

class ApplicationViewModel : ViewModel() {
    private val repository = ApplicationRepository()
    
    private val _myApplications = MutableLiveData<List<Application>>()
    val myApplications: LiveData<List<Application>> = _myApplications
    
    private val _selectedApplication = MutableLiveData<Application?>()
    val selectedApplication: LiveData<Application?> = _selectedApplication
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    fun loadUserApplications(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val applications = repository.getUserApplications(userId)
                _myApplications.value = applications
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load applications: ${e.message}"
                _myApplications.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectApplication(application: Application) {
        _selectedApplication.value = application
    }
    
    fun clearSelectedApplication() {
        _selectedApplication.value = null
    }
    
    // Add other application-related functions here
} 