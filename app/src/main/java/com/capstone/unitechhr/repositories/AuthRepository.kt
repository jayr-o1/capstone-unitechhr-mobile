package com.capstone.unitechhr.repositories

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Random
import java.util.concurrent.TimeUnit

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val functions = FirebaseFunctions.getInstance()
    private val verificationCodesCollection = firestore.collection("verification_codes")
    
    // Check if the user is logged in
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null && firebaseAuth.currentUser!!.isEmailVerified
    }
    
    // Get the current logged in user
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
    
    // Sign in with email and password
    suspend fun signIn(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            return@withContext if (authResult.user != null) {
                // Reload user to get the latest email verification status
                authResult.user!!.reload().await()
                
                if (authResult.user!!.isEmailVerified) {
                    Result.success(authResult.user!!)
                } else {
                    Result.failure(Exception("Email not verified. Please verify your email before logging in."))
                }
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    // Register a new user
    suspend fun register(email: String, password: String, fullName: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            return@withContext if (authResult.user != null) {
                // Generate a 6-digit verification code
                val verificationCode = generateVerificationCode()
                
                // Store verification code in Firestore with timestamp
                val verificationData = hashMapOf(
                    "email" to email,
                    "code" to verificationCode,
                    "timestamp" to System.currentTimeMillis(),
                    "verified" to false,
                    "fullName" to fullName
                )
                
                verificationCodesCollection
                    .document(authResult.user!!.uid)
                    .set(verificationData)
                    .await()
                    
                // Return the verification code
                Result.success(verificationCode)
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    // Verify email with 6-digit code
    suspend fun verifyEmail(code: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext Result.failure(Exception("User not found"))
            
            // Call the Cloud Function to verify the email
            val data = hashMapOf(
                "uid" to user.uid,
                "code" to code
            )
            
            try {
                functions
                    .getHttpsCallable("verifyEmail")
                    .call(data)
                    .await()
                
                // Reload user to get updated verification status
                user.reload().await()
                
                // Update profile with fullname
                val verificationDoc = verificationCodesCollection.document(user.uid).get().await()
                if (verificationDoc.exists()) {
                    val fullName = verificationDoc.getString("fullName") ?: ""
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                }
                
                return@withContext Result.success(user)
            } catch (e: FirebaseFunctionsException) {
                // Handle different error codes
                val message = when (e.code) {
                    FirebaseFunctionsException.Code.INVALID_ARGUMENT -> "Invalid verification code"
                    FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "Verification code expired. Please request a new one."
                    FirebaseFunctionsException.Code.NOT_FOUND -> "Verification record not found"
                    else -> e.message ?: "Verification failed"
                }
                return@withContext Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Resend verification code
    suspend fun resendVerificationCode(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext Result.failure(Exception("User not found"))
            
            // Call the Cloud Function to resend the verification code
            try {
                val result = functions
                    .getHttpsCallable("resendVerificationCode")
                    .call()
                    .await()
                    .data as HashMap<*, *>
                
                return@withContext Result.success(result["code"] as String)
            } catch (e: FirebaseFunctionsException) {
                return@withContext Result.failure(Exception(e.message ?: "Failed to resend verification code"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Generate a random 6-digit verification code
    private fun generateVerificationCode(): String {
        val random = Random()
        val code = random.nextInt(900000) + 100000 // ensures 6 digits
        return code.toString()
    }
    
    // Send password reset email
    suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    // Sign out
    fun signOut() {
        firebaseAuth.signOut()
    }
} 