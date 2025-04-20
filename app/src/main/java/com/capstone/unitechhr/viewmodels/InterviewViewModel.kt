package com.capstone.unitechhr.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.models.Applicant
import com.capstone.unitechhr.models.Interview
import com.capstone.unitechhr.models.InterviewStatus
import com.capstone.unitechhr.models.Job
import com.capstone.unitechhr.repositories.ApplicantRepository
import com.capstone.unitechhr.repositories.InterviewRepository
import com.capstone.unitechhr.repositories.JobRepository
import kotlinx.coroutines.launch
import java.util.Date

class InterviewViewModel : ViewModel() {
    private val interviewRepository = InterviewRepository()
    private val applicantRepository = ApplicantRepository()
    private val jobRepository = JobRepository()

    private val _interviews = MutableLiveData<List<Interview>>()
    val interviews: LiveData<List<Interview>> = _interviews
    
    private val _interviewApplicants = MutableLiveData<Map<String, Applicant>>()
    val interviewApplicants: LiveData<Map<String, Applicant>> = _interviewApplicants
    
    private val _interviewJobs = MutableLiveData<Map<String, Job>>()
    val interviewJobs: LiveData<Map<String, Job>> = _interviewJobs
    
    private val _selectedInterview = MutableLiveData<Interview?>()
    val selectedInterview: LiveData<Interview?> = _selectedInterview
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _operationStatus = MutableLiveData<Boolean?>()
    val operationStatus: LiveData<Boolean?> = _operationStatus
    
    fun loadInterviews() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val interviewsList = interviewRepository.getInterviews()
                _interviews.value = interviewsList
                
                // Load related applicants and jobs
                loadRelatedData(interviewsList)
                
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load interviews: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadRelatedData(interviewsList: List<Interview>) {
        // Load applicant data
        val applicantIds = interviewsList.map { it.applicantId }.distinct()
        val applicantsMap = mutableMapOf<String, Applicant>()
        
        for (id in applicantIds) {
            applicantRepository.getApplicantById(id)?.let { applicant ->
                applicantsMap[id] = applicant
            }
        }
        _interviewApplicants.value = applicantsMap
        
        // Load job data
        val jobIds = interviewsList.map { it.jobId }.distinct()
        val jobsMap = mutableMapOf<String, Job>()
        
        for (id in jobIds) {
            try {
                // First extract universityId and jobId if possible (format: "universityId:jobId")
                val parts = id.split(":")
                if (parts.size == 2) {
                    // If the jobId contains universityId
                    val universityId = parts[0]
                    val jobId = parts[1]
                    jobRepository.getJobById(universityId, jobId)?.let { job ->
                        jobsMap[id] = job
                    }
                } else {
                    // For backwards compatibility, try getting job from all universities
                    val allUniversities = getUniversitiesWithJob(id)
                    if (allUniversities.isNotEmpty()) {
                        val (universityId, job) = allUniversities.first()
                        jobsMap[id] = job
                    }
                }
            } catch (e: Exception) {
                // Log error but continue processing other jobs
                // Log.e("InterviewViewModel", "Error loading job $id: ${e.message}")
            }
        }
        _interviewJobs.value = jobsMap
    }
    
    // Helper method to find a job across all universities
    private suspend fun getUniversitiesWithJob(jobId: String): List<Pair<String, Job>> {
        val result = mutableListOf<Pair<String, Job>>()
        try {
            // Get all jobs
            val allJobs = jobRepository.getJobs()
            
            // Find matching job by ID
            val matchingJob = allJobs.find { it.id == jobId }
            if (matchingJob != null) {
                result.add(Pair(matchingJob.universityId, matchingJob))
            }
        } catch (e: Exception) {
            // Handle any errors
        }
        return result
    }
    
    fun loadInterviewsByApplicant(applicantId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val interviewsList = interviewRepository.getInterviewsByApplicant(applicantId)
                _interviews.value = interviewsList
                
                // Load related jobs
                val jobIds = interviewsList.map { it.jobId }.distinct()
                val jobsMap = mutableMapOf<String, Job>()
                
                for (id in jobIds) {
                    try {
                        // First extract universityId and jobId if possible (format: "universityId:jobId")
                        val parts = id.split(":")
                        if (parts.size == 2) {
                            // If the jobId contains universityId
                            val universityId = parts[0]
                            val jobId = parts[1]
                            jobRepository.getJobById(universityId, jobId)?.let { job ->
                                jobsMap[id] = job
                            }
                        } else {
                            // For backwards compatibility, try getting job from all universities
                            val allUniversities = getUniversitiesWithJob(id)
                            if (allUniversities.isNotEmpty()) {
                                val (universityId, job) = allUniversities.first()
                                jobsMap[id] = job
                            }
                        }
                    } catch (e: Exception) {
                        // Log error but continue processing other jobs
                        // Log.e("InterviewViewModel", "Error loading job $id: ${e.message}")
                    }
                }
                _interviewJobs.value = jobsMap
                
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load interviews: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getInterviewById(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val interview = interviewRepository.getInterviewById(id)
                _selectedInterview.value = interview
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load interview details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun scheduleInterview(
        applicantId: String,
        jobId: String,
        interviewerIds: List<String>,
        scheduledDate: Date,
        duration: Int,
        location: String,
        meetingLink: String,
        notes: String
    ) {
        val newInterview = Interview(
            applicantId = applicantId,
            jobId = jobId,
            interviewerIds = interviewerIds,
            scheduledDate = scheduledDate,
            duration = duration,
            location = location,
            meetingLink = meetingLink,
            notes = notes
        )
        
        viewModelScope.launch {
            _isLoading.value = true
            _operationStatus.value = null
            
            try {
                val success = interviewRepository.scheduleInterview(newInterview)
                _operationStatus.value = success
                
                if (success) {
                    loadInterviews()
                } else {
                    _errorMessage.value = "Failed to schedule interview"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error scheduling interview: ${e.message}"
                _operationStatus.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun updateInterviewStatus(interviewId: String, status: InterviewStatus) {
        viewModelScope.launch {
            _isLoading.value = true
            _operationStatus.value = null
            
            try {
                val success = interviewRepository.updateInterviewStatus(interviewId, status)
                _operationStatus.value = success
                
                if (success) {
                    // Update local data
                    _selectedInterview.value?.let { interview ->
                        if (interview.id == interviewId) {
                            _selectedInterview.value = interview.copy(status = status)
                        }
                    }
                    
                    _interviews.value?.let { list ->
                        _interviews.value = list.map { 
                            if (it.id == interviewId) it.copy(status = status) else it 
                        }
                    }
                } else {
                    _errorMessage.value = "Failed to update interview status"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating interview: ${e.message}"
                _operationStatus.value = false
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun cancelInterview(interviewId: String) {
        updateInterviewStatus(interviewId, InterviewStatus.CANCELLED)
    }
    
    fun completeInterview(interviewId: String) {
        updateInterviewStatus(interviewId, InterviewStatus.COMPLETED)
    }
    
    fun selectInterview(interview: Interview) {
        _selectedInterview.value = interview
    }
    
    fun clearSelectedInterview() {
        _selectedInterview.value = null
    }
    
    fun clearOperationStatus() {
        _operationStatus.value = null
    }
} 