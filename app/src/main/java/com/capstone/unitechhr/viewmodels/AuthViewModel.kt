package com.capstone.unitechhr.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.repositories.AuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    
    private val _loginResult = MutableLiveData<Result<String>>()
    val loginResult: LiveData<Result<String>> = _loginResult
    
    private val _registerResult = MutableLiveData<Result<String>>()
    val registerResult: LiveData<Result<String>> = _registerResult
    
    private val _verifyEmailResult = MutableLiveData<Result<String>>()
    val verifyEmailResult: LiveData<Result<String>> = _verifyEmailResult
    
    private val _resendVerificationCodeResult = MutableLiveData<Result<String>>()
    val resendVerificationCodeResult: LiveData<Result<String>> = _resendVerificationCodeResult
    
    private val _currentUserEmail = MutableLiveData<String?>()
    val currentUserEmail: LiveData<String?> = _currentUserEmail
    
    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser
    
    init {
        // Initialize with current user, if any
        _currentUser.value = authRepository.getCurrentUser()
    }
    
    // Check if user is logged in
    fun isUserLoggedIn(context: Context): Boolean {
        return authRepository.isUserLoggedIn(context)
    }
    
    // Check if user is verified
    suspend fun isUserVerified(): Boolean {
        val user = authRepository.getCurrentUser() ?: return false
        return authRepository.isEmailVerified(user.uid)
    }
    
    // Check if user is logged in and verified
    suspend fun isUserLoggedInAndVerified(): Boolean {
        return authRepository.isUserLoggedInAndVerified()
    }
    
    // Get current user email
    fun getCurrentUserEmail(context: Context): String? {
        val email = authRepository.getCurrentUserEmail(context)
        _currentUserEmail.value = email
        return email
    }
    
    fun login(context: Context, email: String, password: String) {
        viewModelScope.launch {
            val result = authRepository.signIn(context, email, password)
            _loginResult.postValue(result)
            
            if (result.isSuccess) {
                _currentUserEmail.postValue(result.getOrNull())
            }
        }
    }
    
    fun register(email: String, password: String, fullName: String) {
        viewModelScope.launch {
            val result = authRepository.register(email, password, fullName)
            _registerResult.postValue(result)
        }
    }
    
    fun verifyEmail(email: String, code: String) {
        viewModelScope.launch {
            val result = authRepository.verifyEmail(email, code)
            _verifyEmailResult.postValue(result)
        }
    }
    
    fun resendVerificationCode(email: String) {
        viewModelScope.launch {
            val result = authRepository.resendVerificationCode(email)
            _resendVerificationCodeResult.postValue(result)
        }
    }
    
    fun logout(context: Context) {
        authRepository.signOut(context)
        _currentUserEmail.postValue(null)
    }
} 