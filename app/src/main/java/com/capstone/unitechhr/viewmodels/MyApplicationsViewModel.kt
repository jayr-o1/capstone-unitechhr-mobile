package com.capstone.unitechhr.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.models.JobApplication
import com.capstone.unitechhr.repositories.ApplicationRepository
import kotlinx.coroutines.launch

class MyApplicationsViewModel : ViewModel() {
    private val repository = ApplicationRepository()
    private val TAG = "MyApplicationsVM"

    private val _applications = MutableLiveData<List<JobApplication>>()
    val applications: LiveData<List<JobApplication>> = _applications

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _selectedApplication = MutableLiveData<JobApplication?>()
    val selectedApplication: LiveData<JobApplication?> = _selectedApplication

    fun loadApplicationsForUser(userId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading applications for user: $userId")
                val applicationsList = repository.getJobApplicationsForUser(userId)
                _applications.value = applicationsList
                _errorMessage.value = null

                Log.d(TAG, "Loaded ${applicationsList.size} applications")
                if (applicationsList.isEmpty()) {
                    _errorMessage.value = "No job applications found"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading applications: ${e.message}", e)
                _errorMessage.value = "Error loading applications: ${e.message}"
                _applications.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectApplication(application: JobApplication) {
        _selectedApplication.value = application
    }

    fun clearSelection() {
        _selectedApplication.value = null
    }
} 