package com.capstone.unitechhr.repositories

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import android.util.Log

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
                // Store user info in Firestore with isVerified flag
                val userData = hashMapOf(
                    "email" to email,
                    "fullName" to fullName,
                    "isVerified" to false,
                    "createdAt" to System.currentTimeMillis()
                )
                
                verificationCodesCollection
                    .document(authResult.user!!.uid)
                    .set(userData)
                    .await()
                
                // Send verification email using Firebase Auth
                try {
                    // Update user profile with display name
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()
                    authResult.user!!.updateProfile(profileUpdates).await()
                    
                    // Send verification email
                    authResult.user!!.sendEmailVerification().await()
                    Log.d("AuthRepository", "Firebase verification email sent successfully to $email")
                    
                    // Sign out after registration
                    firebaseAuth.signOut()
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Failed to send Firebase verification email: ${e.message}")
                    // Delete the user if we couldn't send verification email
                    try {
                        authResult.user!!.delete().await()
                    } catch (deleteError: Exception) {
                        Log.e("AuthRepository", "Failed to delete user after email error: ${deleteError.message}")
                    }
                    return@withContext Result.failure(e)
                }
                    
                // Return success
                Result.success(Unit)
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    // Verify email after user clicks on the verification link
    suspend fun verifyUserAfterEmailClick(user: FirebaseUser): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            // Reload the user to get the latest emailVerified status
            user.reload().await()
            
            if (user.isEmailVerified) {
                // Update Firestore to mark the user as verified
                verificationCodesCollection.document(user.uid)
                    .update("isVerified", true)
                    .await()
                
                return@withContext Result.success(user)
            } else {
                return@withContext Result.failure(Exception("Email not yet verified. Please check your email and click the verification link."))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    // Check if user's email is verified
    suspend fun isEmailVerified(uid: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Get the user from Firestore
            val doc = verificationCodesCollection.document(uid).get().await()
            if (!doc.exists()) return@withContext false
            
            // Get current user
            val currentUser = firebaseAuth.currentUser
            
            // If we have a Firebase user, check their email verification status
            if (currentUser != null && currentUser.uid == uid) {
                // Reload to get fresh status
                currentUser.reload().await()
                
                if (currentUser.isEmailVerified) {
                    // Update Firestore if Firebase says it's verified
                    if (doc.getBoolean("isVerified") != true) {
                        verificationCodesCollection.document(uid)
                            .update("isVerified", true)
                            .await()
                    }
                    return@withContext true
                }
            }
            
            // Fall back to Firestore value
            return@withContext doc.getBoolean("isVerified") == true
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
    
    // Resend verification email using Firebase Auth
    suspend fun resendVerificationCode(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = firebaseAuth.currentUser ?: return@withContext Result.failure(Exception("User not found"))
            
            // Send verification email directly using Firebase
            user.sendEmailVerification().await()
            Log.d("AuthRepository", "Firebase verification email resent successfully")
            
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to resend verification email: ${e.message}")
            return@withContext Result.failure(e)
        }
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
    
    // This method is no longer used with Firebase Auth verification
    // It's kept for backward compatibility but should be removed in the future
    suspend fun verifyEmailWithCredentials(email: String, code: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        return@withContext Result.failure(Exception("This method is deprecated. Firebase handles email verification now."))
    }
} 