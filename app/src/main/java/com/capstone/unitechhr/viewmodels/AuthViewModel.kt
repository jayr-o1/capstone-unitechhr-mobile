package com.capstone.unitechhr.viewmodels

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
    
    private val _loginResult = MutableLiveData<Result<FirebaseUser>>()
    val loginResult: LiveData<Result<FirebaseUser>> = _loginResult
    
    private val _registerResult = MutableLiveData<Result<Unit>>()
    val registerResult: LiveData<Result<Unit>> = _registerResult
    
    private val _verifyEmailResult = MutableLiveData<Result<FirebaseUser>>()
    val verifyEmailResult: LiveData<Result<FirebaseUser>> = _verifyEmailResult
    
    private val _resendVerificationEmailResult = MutableLiveData<Result<Unit>>()
    val resendVerificationEmailResult: LiveData<Result<Unit>> = _resendVerificationEmailResult
    
    private val _resetPasswordResult = MutableLiveData<Result<Unit>>()
    val resetPasswordResult: LiveData<Result<Unit>> = _resetPasswordResult
    
    private val _currentUser = MutableLiveData<FirebaseUser?>()
    val currentUser: LiveData<FirebaseUser?> = _currentUser
    
    init {
        // Initialize with current user, if any
        _currentUser.value = authRepository.getCurrentUser()
    }
    
    // Check if user is logged in
    fun isUserLoggedIn(): Boolean {
        return authRepository.isUserLoggedIn()
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
    
    fun getCurrentUser(): FirebaseUser? {
        return authRepository.getCurrentUser()
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
    
    // Call this after user clicks the verification link
    fun verifyUserAfterEmailClick() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            viewModelScope.launch {
                val result = authRepository.verifyUserAfterEmailClick(user)
                _verifyEmailResult.postValue(result)
                
                if (result.isSuccess) {
                    _currentUser.postValue(result.getOrNull())
                }
            }
        }
    }
    
    // Resend verification email
    fun resendVerificationEmail() {
        val user = authRepository.getCurrentUser()
        if (user != null) {
            viewModelScope.launch {
                val result = authRepository.resendVerificationCode()
                _resendVerificationEmailResult.postValue(result)
            }
        } else {
            _resendVerificationEmailResult.postValue(Result.failure(Exception("User not logged in")))
        }
    }
    
    // Verify with code
    fun verifyWithCode(email: String, code: String) {
        viewModelScope.launch {
            try {
                // Call Firebase Function to verify code
                val functions = com.google.firebase.functions.FirebaseFunctions.getInstance()
                val data = hashMapOf(
                    "email" to email,
                    "code" to code
                )
                
                val result = functions
                    .getHttpsCallable("verifyEmail")
                    .call(data)
                    .await()
                
                val success = (result.data as? Map<String, Any>)?.get("success") as? Boolean ?: false
                
                if (success) {
                    // Try to sign in with saved credentials to get the verified user
                    val user = authRepository.getCurrentUser()
                    if (user != null) {
                        // Update user's verification status
                        _verifyEmailResult.postValue(Result.success(user))
                    } else {
                        _verifyEmailResult.postValue(Result.failure(Exception("Verification successful, please sign in again.")))
                    }
                } else {
                    _verifyEmailResult.postValue(Result.failure(Exception("Verification failed. Please try again.")))
                }
            } catch (e: Exception) {
                _verifyEmailResult.postValue(Result.failure(e))
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