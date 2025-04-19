package com.capstone.unitechhr.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.models.Applicant
import com.capstone.unitechhr.models.ApplicationStatus
import com.capstone.unitechhr.repositories.ApplicantRepository
import kotlinx.coroutines.launch

class ApplicantViewModel : ViewModel() {
    private val repository = ApplicantRepository()

    private val _applicants = MutableLiveData<List<Applicant>>()
    val applicants: LiveData<List<Applicant>> = _applicants

    private val _selectedApplicant = MutableLiveData<Applicant?>()
    val selectedApplicant: LiveData<Applicant?> = _selectedApplicant

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    init {
        loadApplicants()
    }

    fun loadApplicants() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val applicantsList = repository.getApplicants()
                _applicants.value = applicantsList
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load applicants: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectApplicant(applicant: Applicant) {
        _selectedApplicant.value = applicant
    }

    fun clearSelectedApplicant() {
        _selectedApplicant.value = null
    }

    fun addApplicant(applicant: Applicant) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.addApplicant(applicant)
                if (success) {
                    loadApplicants()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Failed to add applicant"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error adding applicant: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateApplicant(applicant: Applicant) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.updateApplicant(applicant)
                if (success) {
                    loadApplicants()
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Failed to update applicant"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error updating applicant: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteApplicant(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.deleteApplicant(id)
                if (success) {
                    loadApplicants()
                    if (_selectedApplicant.value?.id == id) {
                        clearSelectedApplicant()
                    }
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Failed to delete applicant"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting applicant: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateApplicantStatus(applicant: Applicant, newStatus: ApplicationStatus) {
        val updatedApplicant = applicant.copy(status = newStatus)
        updateApplicant(updatedApplicant)
    }

    fun searchApplicants(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (query.isEmpty()) {
                    loadApplicants()
                } else {
                    val results = repository.searchApplicants(query)
                    _applicants.value = results
                }
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error searching applicants: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
} 