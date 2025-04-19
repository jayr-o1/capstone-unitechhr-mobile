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
    
    private val _registerResult = MutableLiveData<Result<String>>()
    val registerResult: LiveData<Result<String>> = _registerResult
    
    private val _verifyEmailResult = MutableLiveData<Result<FirebaseUser>>()
    val verifyEmailResult: LiveData<Result<FirebaseUser>> = _verifyEmailResult
    
    private val _resendCodeResult = MutableLiveData<Result<String>>()
    val resendCodeResult: LiveData<Result<String>> = _resendCodeResult
    
    private val _resetPasswordResult = MutableLiveData<Result<Unit>>()
    val resetPasswordResult: LiveData<Result<Unit>> = _resetPasswordResult
    
    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser
    
    init {
        // Initialize with current user, if any
        _currentUser.value = authRepository.getCurrentUser()
    }
    
    fun getCurrentUser(): FirebaseUser? {
        return authRepository.getCurrentUser()
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
    
    fun register(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            val result = authRepository.register(email, password, fullName)
            _registerResult.postValue(result)
        }
    }
    
    fun verifyEmail(code: String) {
        viewModelScope.launch {
            val result = authRepository.verifyEmail(code)
            _verifyEmailResult.postValue(result)
            
            if (result.isSuccess) {
                _currentUser.postValue(result.getOrNull())
            }
        }
    }
    
    fun resendVerificationCode() {
        viewModelScope.launch {
            val result = authRepository.resendVerificationCode()
            _resendCodeResult.postValue(result)
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