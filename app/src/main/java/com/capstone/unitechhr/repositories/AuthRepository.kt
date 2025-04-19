package com.capstone.unitechhr.repositories

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class AuthRepository {
    private val firebaseAuth = FirebaseAuth.getInstance()
    
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
                Result.success(authResult.user!!)
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            return@withContext Result.failure(e)
        }
    }
    
    // Register a new user
    suspend fun register(email: String, password: String): Result<FirebaseUser> = withContext(Dispatchers.IO) {
        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            return@withContext if (authResult.user != null) {
                Result.success(authResult.user!!)
            } else {
                Result.failure(Exception("Registration failed"))
            }
        } catch (e: Exception) {
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
} 