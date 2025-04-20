package com.capstone.unitechhr.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.repositories.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.launch

data class UserData(
    val email: String,
    val displayName: String
)

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    
    private val _signInResult = MutableLiveData<Result<String>>()
    val signInResult: LiveData<Result<String>> = _signInResult
    
    private val _currentUserEmail = MutableLiveData<String?>()
    val currentUserEmail: LiveData<String?> = _currentUserEmail
    
    private val _currentUser = MutableLiveData<UserData?>()
    val currentUser: LiveData<UserData?> = _currentUser
    
    // Get Google Sign In client
    fun getGoogleSignInClient(context: Context, webClientId: String): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .requestProfile()
            .build()
        
        return com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso)
    }
    
    // Handle Google Sign In result
    fun handleSignInResult(context: Context, completedTask: Task<GoogleSignInAccount>) {
        viewModelScope.launch {
            try {
                val account = completedTask.getResult(ApiException::class.java)
                val result = authRepository.handleGoogleSignInResult(context, account)
                _signInResult.postValue(result)
                
                if (result.isSuccess) {
                    loadCurrentUser(context)
                }
            } catch (e: ApiException) {
                // Provide more detailed error message based on status code
                val errorMessage = when (e.statusCode) {
                    // Common error codes from GoogleSignInStatusCodes
                    12500 -> "This device doesn't have Google Play Services installed"
                    12501 -> "User cancelled the sign-in flow"
                    12502 -> "Sign-in attempt currently in progress"
                    7 -> "Network error occurred. Check your connection"
                    8 -> "Internal error occurred"
                    10 -> "Developer error: Check your Google Sign-In configuration"
                    16 -> "API key not valid. Check your API key configuration"
                    else -> "Google sign-in failed: ${e.statusCode}"
                }
                
                Log.e("AuthViewModel", "Google sign-in error: ${e.statusCode}", e)
                _signInResult.postValue(Result.failure(Exception(errorMessage)))
            }
        }
    }
    
    // Load current user data from shared preferences
    fun loadCurrentUser(context: Context) {
        val email = authRepository.getCurrentUserEmail(context)
        if (email != null) {
            val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val displayName = sharedPreferences.getString("current_user_name", "User") ?: "User"
            _currentUser.value = UserData(email, displayName)
            _currentUserEmail.value = email
        } else {
            _currentUser.value = null
            _currentUserEmail.value = null
        }
    }
    
    // Check if user is already signed in
    fun checkSignInStatus(context: Context): Boolean {
        // First and foremost, check if the is_logged_out flag is set
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val isLoggedOut = sharedPreferences.getBoolean("is_logged_out", false)
        
        // If logged out flag is set, always return false
        if (isLoggedOut) {
            Log.d("AuthViewModel", "User is explicitly logged out, preventing auto-login")
            return false
        }
        
        // Check if the user was recently logged out
        if (authRepository.wasRecentlyLoggedOut(context)) {
            Log.d("AuthViewModel", "User was recently logged out, blocking auto-login for safety")
            return false
        }
        
        // Check if we have a stored email - this is more reliable than Google's cache
        val storedEmail = sharedPreferences.getString("current_user_email", null)
        if (storedEmail == null) {
            Log.d("AuthViewModel", "No stored email, user is not logged in")
            return false
        }
        
        // Double-check against Google account
        try {
            val account = authRepository.getLastSignedInAccount(context)
            if (account != null && account.email != null) {
                // Make sure the emails match
                if (account.email == storedEmail) {
                    _currentUserEmail.value = account.email
                    loadCurrentUser(context)
                    return true
                } else {
                    // Emails don't match, this is suspicious
                    Log.w("AuthViewModel", "Stored email doesn't match Google account, logging out")
                    authRepository.signOut(context)
                    return false
                }
            } else {
                // No Google account found but we have a stored email
                Log.w("AuthViewModel", "Stored email exists but no Google account found, logging out")
                authRepository.signOut(context)
                return false
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error checking sign in status", e)
            return false
        }
    }
    
    // Check if user is logged in
    fun isUserLoggedIn(context: Context): Boolean {
        return authRepository.isUserLoggedIn(context)
    }
    
    // Get current user email
    fun getCurrentUserEmail(context: Context): String? {
        val email = authRepository.getCurrentUserEmail(context)
        _currentUserEmail.value = email
        return email
    }
    
    // Sign out
    fun logout(context: Context, googleSignInClient: GoogleSignInClient? = null) {
        // Clear all shared preferences first
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
        
        // Then specifically set logged out flags
        sharedPreferences.edit()
            .putBoolean("is_logged_out", true)
            .putLong("logout_timestamp", System.currentTimeMillis())
            .apply()
            
        // Sign out from Google with completion listener
        googleSignInClient?.signOut()?.addOnCompleteListener {
            // Clear local storage after Google sign-out completes
            authRepository.signOut(context)
            _currentUserEmail.postValue(null)
            _currentUser.postValue(null)
            
            // Log the sign-out for debugging
            Log.d("AuthViewModel", "User signed out successfully")
        } ?: run {
            // If no googleSignInClient provided, just clear local storage
            authRepository.signOut(context)
            _currentUserEmail.postValue(null)
            _currentUser.postValue(null)
        }
    }
} 