package com.capstone.unitechhr.repositories

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Random
import java.util.concurrent.TimeUnit
import android.util.Log
import com.capstone.unitechhr.services.EmailService

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val verificationCodesCollection = firestore.collection("verification_codes")
    
    // Check if the user is logged in
    fun isUserLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
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
                // Check if the user's email is verified in Firestore
                val isVerified = isEmailVerified(authResult.user!!.uid)
                
                if (isVerified) {
                    Result.success(authResult.user!!)
                } else {
                    // Sign out the user since they're not verified
                    firebaseAuth.signOut()
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
    suspend fun register(email: String, password: String, fullName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            return@withContext if (authResult.user != null) {
                // Generate a 6-digit verification code
                val verificationCode = generateVerificationCode()
                
                // Store verification code in Firestore with timestamp and isVerified flag
                val verificationData = hashMapOf(
                    "email" to email,
                    "code" to verificationCode,
                    "timestamp" to System.currentTimeMillis(),
                    "isVerified" to false,
                    "fullName" to fullName
                )
                
                verificationCodesCollection
                    .document(authResult.user!!.uid)
                    .set(verificationData)
                    .await()
                
                // Send verification email using our EmailService instead of Cloud Functions
                try {
                    val emailResult = EmailService.sendVerificationEmail(email, verificationCode, fullName)
                    
                    if (emailResult.isFailure) {
                        Log.e("AuthRepository", "Failed to send verification email: ${emailResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Failed to send verification email: ${e.message}")
                }
                    
                // Return success without the verification code
                Result.success(Unit)
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
            
            // Get the verification record from Firestore
            val verificationDoc = verificationCodesCollection.document(user.uid).get().await()
            
            if (!verificationDoc.exists()) {
                return@withContext Result.failure(Exception("Verification record not found"))
            }
            
            val verificationData = verificationDoc.data
            val storedCode = verificationData?.get("code") as? String
            val timestamp = verificationData?.get("timestamp") as? Long
            val currentTime = System.currentTimeMillis()
            
            // Validate code
            if (storedCode == null || storedCode != code) {
                return@withContext Result.failure(Exception("Invalid verification code"))
            }
            
            // Check if code is expired (10 minutes validity)
            val TEN_MINUTES_MS = 10 * 60 * 1000
            if (timestamp != null && currentTime - timestamp > TEN_MINUTES_MS) {
                return@withContext Result.failure(Exception("Verification code expired. Please request a new one"))
            }
            
            // Update Firestore to mark as verified
            verificationCodesCollection.document(user.uid)
                .update("isVerified", true)
                .await()
            
            // Update the user's profile
            val fullName = verificationData?.get("fullName") as? String ?: ""
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            user.updateProfile(profileUpdates).await()
            
            // Mark email as verified in Firebase Auth
            user.updateProfile(profileUpdates).await()
            user.reload().await()
                
            return@withContext Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // Check if user's email is verified based on Firestore record
    suspend fun isEmailVerified(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val doc = verificationCodesCollection.document(uid).get().await()
            return@withContext doc.exists() && doc.getBoolean("isVerified") == true
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error checking verification status: ${e.message}")
            return@withContext false
        }
    }
    
    // Check if the user is logged in and verified
    suspend fun isUserLoggedInAndVerified(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        return isEmailVerified(user.uid)
    }
    
    // Resend verification code
    suspend fun resendVerificationCode(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext Result.failure(Exception("User not found"))
            
            // Generate a new 6-digit verification code
            val verificationCode = generateVerificationCode()
            
            // Get the user's email and name from Firestore
            val verificationDoc = verificationCodesCollection.document(user.uid).get().await()
            
            if (!verificationDoc.exists()) {
                return@withContext Result.failure(Exception("Verification record not found"))
            }
            
            val email = verificationDoc.getString("email") ?: return@withContext Result.failure(Exception("Email not found"))
            val fullName = verificationDoc.getString("fullName") ?: ""
            
            // Update the verification code in Firestore
            verificationCodesCollection.document(user.uid)
                .update(mapOf(
                    "code" to verificationCode,
                    "timestamp" to System.currentTimeMillis()
                ))
                .await()
            
            // Send new verification email
            try {
                val emailResult = EmailService.sendVerificationEmail(email, verificationCode, fullName)
                
                if (emailResult.isFailure) {
                    Log.e("AuthRepository", "Failed to send verification email: ${emailResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.e("AuthRepository", "Failed to send verification email: ${e.message}")
            }
            
            return@withContext Result.success(verificationCode)
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
    
    // Verify email with credentials (email + code)
    suspend fun verifyEmailWithCredentials(email: String, code: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            // Check if the email exists in the verification_codes collection
            val querySnapshot = verificationCodesCollection
                .whereEqualTo("email", email)
                .get()
                .await()
            
            if (querySnapshot.isEmpty) {
                return@withContext Result.failure(Exception("No verification record found for this email"))
            }
            
            // Get the first document (there should be only one)
            val verificationDoc = querySnapshot.documents[0]
            val verificationData = verificationDoc.data
            val storedCode = verificationData?.get("code") as? String
            val timestamp = verificationData?.get("timestamp") as? Long
            val currentTime = System.currentTimeMillis()
            val userId = verificationDoc.id
            
            // Validate code
            if (storedCode == null || storedCode != code) {
                return@withContext Result.failure(Exception("Invalid verification code"))
            }
            
            // Check if code is expired (10 minutes validity)
            val TEN_MINUTES_MS = 10 * 60 * 1000
            if (timestamp != null && currentTime - timestamp > TEN_MINUTES_MS) {
                return@withContext Result.failure(Exception("Verification code expired. Please register again"))
            }
            
            // Update Firestore to mark as verified
            verificationCodesCollection.document(userId)
                .update("isVerified", true)
                .await()
            
            // Find the user in Firebase Auth
            try {
                // We need to sign in to get access to the user
                val authResult = firebaseAuth.signInWithEmailAndPassword(email, "temporary").await()
                val user = authResult.user
                
                if (user != null) {
                    // Update the user's profile
                    val fullName = verificationData?.get("fullName") as? String ?: ""
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                    user.updateProfile(profileUpdates).await()
                    
                    // Sign out as we just needed to get the user for verification
                    firebaseAuth.signOut()
                    
                    return@withContext Result.success(user)
                } else {
                    return@withContext Result.failure(Exception("User not found"))
                }
            } catch (e: Exception) {
                // If we can't sign in, the user likely doesn't exist or has incorrect credentials
                return@withContext Result.failure(Exception("Could not verify this account: ${e.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 