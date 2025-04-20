package com.capstone.unitechhr.viewmodels

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.models.Job
import com.capstone.unitechhr.models.University
import com.capstone.unitechhr.repositories.JobRepository
import kotlinx.coroutines.launch

class JobViewModel : ViewModel() {
    private val TAG = "JobViewModel"
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
                
                // Filter out jobs with "Closed" status
                val openJobs = jobsList.filter { job ->
                    job.status?.equals("Closed", ignoreCase = true) != true
                }
                
                _jobs.value = openJobs
                Log.d(TAG, "Loaded ${openJobs.size} open jobs out of ${jobsList.size} total jobs")
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load jobs: ${e.message}", e)
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
                Log.d(TAG, "Fetching job with ID: $jobId from university: $universityId")
                val job = repository.getJobById(universityId, jobId)
                if (job != null) {
                    Log.d(TAG, "Successfully loaded job: ${job.title}")
                    _selectedJob.value = job
                } else {
                    Log.e(TAG, "Job with ID $jobId not found or returned null")
                    _errorMessage.value = "Job not found"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load job details: ${e.message}", e)
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
                val searchResults = repository.searchJobs(query)
                
                // Filter out jobs with "Closed" status
                val openJobs = searchResults.filter { job ->
                    job.status?.equals("Closed", ignoreCase = true) != true
                }
                
                _jobs.value = openJobs
                Log.d(TAG, "Search found ${openJobs.size} open jobs out of ${searchResults.size} total jobs matching '$query'")
                _errorMessage.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to search jobs: ${e.message}", e)
                _errorMessage.value = "Failed to search jobs: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectJob(job: Job) {
        Log.d(TAG, "Selecting job: ${job.id} - ${job.title}")
        // Check if we have all the necessary details in the job object
        val hasKeyDuties = !job.keyDuties.isNullOrEmpty()
        val hasEssentialSkills = !job.essentialSkills.isNullOrEmpty() 
        val hasQualifications = !job.qualifications.isNullOrEmpty()
        
        Log.d(TAG, "Job has key duties: $hasKeyDuties")
        Log.d(TAG, "Job has essential skills: $hasEssentialSkills")
        Log.d(TAG, "Job has qualifications: $hasQualifications")
        
        // If we have everything we need, just set it directly
        if (hasKeyDuties && hasEssentialSkills && hasQualifications) {
            Log.d(TAG, "Job already has all details, setting directly")
            _selectedJob.value = job
        } 
        // Otherwise, reload from the repository to get full details
        else {
            Log.d(TAG, "Job missing some details, loading from repository")
            getJobById(job.universityId, job.id)
        }
    }
    
    fun clearSelectedJob() {
        Log.d(TAG, "Clearing selected job")
        _selectedJob.value = null
    }
} 