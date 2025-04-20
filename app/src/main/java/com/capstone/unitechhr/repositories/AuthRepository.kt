package com.capstone.unitechhr.repositories

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.util.Log
import java.util.Random

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
    
    // Sign in with email and password
    suspend fun signIn(context: Context, email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Query the applicants collection for the user
            val emailSlug = emailToCollectionId(email)
            val userDoc = applicantsCollection.document(emailSlug).get().await()
            
            if (!userDoc.exists()) {
                return@withContext Result.failure(Exception("No account found with this email"))
            }
            
            val storedPassword = userDoc.getString("password")
            if (storedPassword != password) {
                return@withContext Result.failure(Exception("Incorrect password"))
            }
            
            val isVerified = userDoc.getBoolean("isVerified") ?: false
            if (!isVerified) {
                return@withContext Result.failure(Exception("Email not verified. Please verify your account first."))
            }
            
            // Save user session
            val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            sharedPreferences.edit().apply {
                putString("current_user_email", email)
                putString("current_user_name", userDoc.getString("fullName"))
                apply()
            }
            
            return@withContext Result.success(email)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    // Register a new user
    suspend fun register(email: String, password: String, fullName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Create a collection ID from the email
            val emailSlug = emailToCollectionId(email)
            
            // Check if user already exists
            val existingUser = applicantsCollection.document(emailSlug).get().await()
            if (existingUser.exists()) {
                return@withContext Result.failure(Exception("An account with this email already exists"))
            }
            
            // Generate verification code
            val verificationCode = generateVerificationCode()
            
            // Store user info in Firestore with isVerified flag
            val userData = hashMapOf(
                "email" to email,
                "fullName" to fullName,
                "password" to password,  // In a real app, you should hash this
                "isVerified" to false,
                "verificationCode" to verificationCode,
                "createdAt" to System.currentTimeMillis()
            )
            
            applicantsCollection.document(emailSlug).set(userData).await()
            
            // In a real app, you would send the verification code via email here
            Log.d("AuthRepository", "Registration successful. Verification code: $verificationCode")
            
            return@withContext Result.success(email)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    // Verify email with 6-digit code
    suspend fun verifyEmail(email: String, code: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val emailSlug = emailToCollectionId(email)
            val userDoc = applicantsCollection.document(emailSlug).get().await()
            
            if (!userDoc.exists()) {
                return@withContext Result.failure(Exception("No account found with this email"))
            }
            
            val storedCode = userDoc.getString("verificationCode")
            if (storedCode != code) {
                return@withContext Result.failure(Exception("Invalid verification code"))
            }
            
            // Mark the user as verified
            applicantsCollection.document(emailSlug)
                .update("isVerified", true)
                .await()
            
            return@withContext Result.success(email)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    // Generate a new verification code and save it
    suspend fun resendVerificationCode(email: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val emailSlug = emailToCollectionId(email)
            val userDoc = applicantsCollection.document(emailSlug).get().await()
            
            if (!userDoc.exists()) {
                return@withContext Result.failure(Exception("No account found with this email"))
            }
            
            // Generate a new verification code
            val newCode = generateVerificationCode()
            
            // Update the verification code in Firestore
            applicantsCollection.document(emailSlug)
                .update("verificationCode", newCode)
                .await()
            
            // In a real app, you would send this code via email
            Log.d("AuthRepository", "New verification code for $email: $newCode")
            
            return@withContext Result.success(newCode)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    // Sign out
    fun signOut(context: Context) {
        val sharedPreferences = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().clear().apply()
    }
    
    // Helper function to generate a 6-digit verification code
    private fun generateVerificationCode(): String {
        return String.format("%06d", Random().nextInt(999999))
    }
    
    // Helper function to convert email to a valid collection ID
    private fun emailToCollectionId(email: String): String {
        // Replace invalid characters with dash
        return email.replace("@", "-").replace(".", "-")
    }
} 