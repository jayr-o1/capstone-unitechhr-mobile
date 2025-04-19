package com.capstone.unitechhr.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.repositories.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    
    private val _loginResult = MutableLiveData<Result<FirebaseUser>>()
    val loginResult: LiveData<Result<FirebaseUser>> = _loginResult
    
    private val _registerResult = MutableLiveData<Result<FirebaseUser>>()
    val registerResult: LiveData<Result<FirebaseUser>> = _registerResult
    
    private val _resetPasswordResult = MutableLiveData<Result<Unit>>()
    val resetPasswordResult: LiveData<Result<Unit>> = _resetPasswordResult
    
    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser
    
    init {
        // Initialize with current user, if any
        _currentUser.value = authRepository.getCurrentUser()
    }
    
    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
    }
    
    fun login(email: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.signIn(email, password)
            _loginResult.postValue(result)
            
            if (result.isSuccess) {
                _currentUser.postValue(result.getOrNull())
            }
        }
    }
    
    fun register(email: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.register(email, password)
            _registerResult.postValue(result)
            
            if (result.isSuccess) {
                _currentUser.postValue(result.getOrNull())
            }
        }
    }
    
    fun sendPasswordResetEmail(email: String) {
        viewModelScope.launch {
            val result = authRepository.sendPasswordResetEmail(email)
            _resetPasswordResult.postValue(result)
        }
    }
    
    fun logout() {
        authRepository.signOut()
        _currentUser.postValue(null)
    }
} 