package com.capstone.unitechhr.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.models.University
import com.capstone.unitechhr.repositories.UniversityRepository
import kotlinx.coroutines.launch

class UniversityViewModel : ViewModel() {
    private val repository = UniversityRepository()
    
    private val _universities = MutableLiveData<List<University>>()
    val universities: LiveData<List<University>> = _universities
    
    private val _selectedUniversity = MutableLiveData<University?>()
    val selectedUniversity: LiveData<University?> = _selectedUniversity
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    init {
        loadUniversities()
    }
    
    fun loadUniversities() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val list = repository.getUniversities()
                _universities.value = list
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load universities: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getUniversityById(id: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val university = repository.getUniversityById(id)
                _selectedUniversity.value = university
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load university details: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun getUniversityByCode(code: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val university = repository.getUniversityByCode(code)
                _selectedUniversity.value = university
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load university by code: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun selectUniversity(university: University) {
        _selectedUniversity.value = university
    }
    
    fun clearSelectedUniversity() {
        _selectedUniversity.value = null
    }
} 