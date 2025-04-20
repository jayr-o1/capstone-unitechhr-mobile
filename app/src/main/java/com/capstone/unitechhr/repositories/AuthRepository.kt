package com.capstone.unitechhr.repositories

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val applicantsCollection = firestore.collection("applicants")
    
    // Check if the user is logged in
    fun isUserLoggedIn(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("current_user_email", null) != null
    }
    
    // Get the current logged in user email
    fun getCurrentUserEmail(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getString("current_user_email", null)
    }
    
    // Process Google Sign-In result
    suspend fun handleGoogleSignInResult(context: Context, account: GoogleSignInAccount?): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (account == null) {
                return@withContext Result.failure(Exception("Google sign-in failed"))
            }
            
            val email = account.email ?: return@withContext Result.failure(Exception("Email not available"))
            val fullName = account.displayName ?: "User"
            val googleId = account.id ?: return@withContext Result.failure(Exception("Google ID not available"))
            
            // Create a document ID from the email
            val emailSlug = emailToCollectionId(email)
            
            // Check if user already exists
            val userDoc = applicantsCollection.document(emailSlug).get().await()
            
            if (!userDoc.exists()) {
                // New user - create a profile
                val userData = hashMapOf(
                    "email" to email,
                    "fullName" to fullName,
                    "googleId" to googleId,
                    "createdAt" to System.currentTimeMillis()
                )
                
                applicantsCollection.document(emailSlug).set(userData).await()
                Log.d("AuthRepository", "New user created: $email")
            } else {
                // Existing user - update Google ID if needed
                if (userDoc.getString("googleId") != googleId) {
                    applicantsCollection.document(emailSlug)
                        .update("googleId", googleId)
                        .await()
                }
            }
            
            // Save user session
            val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().apply {
                putString("current_user_email", email)
                putString("current_user_name", fullName)
                putString("current_user_id", googleId)
                putBoolean("is_logged_out", false)
                apply()
            }
            
            return@withContext Result.success(email)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Google sign-in error", e)
            return@withContext Result.failure(e)
        }
    }
    
    // Check if Google account is currently signed in
    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }
    
    // Sign out
    fun signOut(context: Context) {
        // Get shared preferences
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        
        // Set the logged out flag and timestamp before clearing
        sharedPreferences.edit()
            .putBoolean("is_logged_out", true)
            .putLong("logout_timestamp", System.currentTimeMillis())
            .apply()
        
        // Then clear other user data
        sharedPreferences.edit()
            .remove("current_user_email")
            .remove("current_user_name")
            .remove("current_user_id")
            .apply()
        
        try {
            // Try to clear any Google Sign-In account caches manually
            val googleSignIn = GoogleSignIn.getClient(context, 
                GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build())
            googleSignIn.signOut() // This is a backup to ensure Google sign-out
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error during manual Google sign-out", e)
        }
        
        Log.d("AuthRepository", "User data cleared from local storage and logged_out flag set")
    }
    
    // Check if user was recently logged out (within the last minute)
    fun wasRecentlyLoggedOut(context: Context): Boolean {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val isLoggedOut = sharedPreferences.getBoolean("is_logged_out", false)
        
        if (!isLoggedOut) {
            return false
        }
        
        // Check if logout happened within the last minute
        val logoutTime = sharedPreferences.getLong("logout_timestamp", 0)
        val currentTime = System.currentTimeMillis()
        val timeDifference = currentTime - logoutTime
        
        // Block auto-login for 60 seconds after logout
        val blockPeriod = 60 * 1000L // 60 seconds in milliseconds
        
        return timeDifference < blockPeriod
    }
    
    // Helper function to convert email to a valid collection ID
    private fun emailToCollectionId(email: String): String {
        // Replace invalid characters with dash
        return email.replace("@", "-").replace(".", "-")
    }
} 