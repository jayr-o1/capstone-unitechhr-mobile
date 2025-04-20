package com.capstone.unitechhr.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.models.Job
import com.capstone.unitechhr.models.University
import com.capstone.unitechhr.repositories.JobRepository
import kotlinx.coroutines.launch

class JobViewModel : ViewModel() {
    private val repository = JobRepository()
    
    private val _jobs = MutableLiveData<List<Job>>()
    val jobs: LiveData<List<Job>> = _jobs
    
    private val _selectedJob = MutableLiveData<Job?>()
    val selectedJob: LiveData<Job?> = _selectedJob
    
    private val _selectedUniversityId = MutableLiveData<String>()
    val selectedUniversityId: LiveData<String> = _selectedUniversityId
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    init {
        loadJobs()
    }
    
    fun loadJobs() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val selectedUniId = _selectedUniversityId.value
                val jobsList = if (selectedUniId.isNullOrEmpty()) {
                    repository.getJobs()
                } else {
                    repository.getJobsByUniversity(selectedUniId)
                }
                _jobs.value = jobsList
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load jobs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun setSelectedUniversity(universityId: String) {
        if (_selectedUniversityId.value != universityId) {
            _selectedUniversityId.value = universityId
            loadJobs()
        }
    }
    
    fun clearUniversityFilter() {
        _selectedUniversityId.value = ""
        loadJobs()
    }
    
    fun getJobById(universityId: String, jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val job = repository.getJobById(universityId, jobId)
                _selectedJob.value = job
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load job details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun searchJobs(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val selectedUniId = _selectedUniversityId.value
                val searchResults = if (selectedUniId.isNullOrEmpty()) {
                    repository.searchJobs(query)
                } else {
                    repository.searchJobsByUniversity(query, selectedUniId)
                }
                _jobs.value = searchResults
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Search failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectJob(job: Job) {
        _selectedJob.value = job
    }
    
    fun clearSelectedJob() {
        _selectedJob.value = null
    }
} 