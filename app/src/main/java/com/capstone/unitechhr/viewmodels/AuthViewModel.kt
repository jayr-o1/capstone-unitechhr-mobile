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
    val displayName: String,
    val resumeUrl: String? = null,
    val hasResume: Boolean = false,
    val jobTitle: String? = null,
    val universityId: String? = null,
    val jobId: String? = null
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
                
                if (result.isSuccess) {
                    // Immediately load current user after successful sign-in
                    loadCurrentUser(context)
                    
                    // For debugging: Log the currently loaded user data
                    val email = result.getOrNull()
                    val userName = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        .getString("current_user_name", "User")
                    Log.d("AuthViewModel", "Successfully signed in user: $userName ($email)")
                }
                
                _signInResult.postValue(result)
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
        try {
            val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            val email = authRepository.getCurrentUserEmail(context)
            
            if (email != null) {
                // Try to get displayName from SharedPreferences
                val displayName = sharedPreferences.getString("current_user_name", null)
                    ?: email.substringBefore("@") // Fallback to email username if no name stored
                
                // Try to get resumeUrl from SharedPreferences
                val resumeUrl = sharedPreferences.getString("resume_url", null)
                val hasResume = !resumeUrl.isNullOrEmpty()
                
                // Load job-related information
                val jobTitle = sharedPreferences.getString("job_title", null)
                val universityId = sharedPreferences.getString("university_id", null)
                val jobId = sharedPreferences.getString("job_id", null)
                
                // Update the current user value and email
                val userData = UserData(
                    email = email, 
                    displayName = displayName, 
                    resumeUrl = resumeUrl, 
                    hasResume = hasResume,
                    jobTitle = jobTitle,
                    universityId = universityId,
                    jobId = jobId
                )
                _currentUser.value = userData
                _currentUserEmail.value = email
                
                Log.d("AuthViewModel", "Successfully loaded user: $displayName ($email), resume: ${if (hasResume) "Yes" else "No"}")
                Log.d("AuthViewModel", "User job info - title: $jobTitle, universityId: $universityId, jobId: $jobId")
                
                // If we somehow didn't have the name stored properly, update it
                if (!sharedPreferences.contains("current_user_name")) {
                    sharedPreferences.edit()
                        .putString("current_user_name", displayName)
                        .apply()
                    Log.d("AuthViewModel", "Updated missing user name in preferences: $displayName")
                }
            } else {
                _currentUser.value = null
                _currentUserEmail.value = null
                Log.d("AuthViewModel", "No user email found, user data set to null")
            }
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error loading current user data", e)
            // Don't reset the user data on error to avoid potential data loss
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
        try {
            // Clear all shared preferences first
            val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            
            // First remove any user-related data
            sharedPreferences.edit().clear().apply()
            
            // Then specifically set logged out flags - do this in a separate edit for reliability
            sharedPreferences.edit()
                .putBoolean("is_logged_out", true)
                .putLong("logout_timestamp", System.currentTimeMillis())
                .apply()
                
            // Set a system property to force logout across app restarts
            System.setProperty("com.capstone.unitechhr.user.logged_out", "true")
                
            // Sign out from Google with completion listener
            googleSignInClient?.signOut()?.addOnCompleteListener {
                // Clear local storage after Google sign-out completes
                authRepository.signOut(context)
                _currentUserEmail.postValue(null)
                _currentUser.postValue(null)
                
                // Log the sign-out for debugging
                Log.d("AuthViewModel", "User signed out successfully via Google client")
            } ?: run {
                // If no googleSignInClient provided, just clear local storage
                authRepository.signOut(context)
                _currentUserEmail.postValue(null)
                _currentUser.postValue(null)
                
                Log.d("AuthViewModel", "User signed out successfully without Google client")
            }
            
            Log.d("AuthViewModel", "Logout process completed, is_logged_out flag is set to true")
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error during logout", e)
        }
    }
    
    /**
     * Update the current user's resume information in SharedPreferences and Firestore
     */
    fun updateUserResume(context: Context, resumeUrl: String?) {
        viewModelScope.launch {
            try {
                // Update SharedPreferences
                val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                sharedPreferences.edit()
                    .putString("resume_url", resumeUrl)
                    .apply()
                
                // Get current user email
                val email = getCurrentUserEmail(context)
                
                // Update Firestore document if we have an email
                if (email != null) {
                    authRepository.updateUserResumeInFirestore(email, resumeUrl)
                    
                    // Update current user data in view model
                    val currentUserData = _currentUser.value
                    if (currentUserData != null) {
                        _currentUser.value = currentUserData.copy(
                            resumeUrl = resumeUrl,
                            hasResume = !resumeUrl.isNullOrEmpty()
                        )
                    }
                    
                    Log.d("AuthViewModel", "Updated user resume: $resumeUrl")
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error updating user resume", e)
            }
        }
    }
} 