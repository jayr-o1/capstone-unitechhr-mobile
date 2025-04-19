package com.capstone.unitechhr.utils

import android.content.Context
import android.os.StrictMode
import android.util.Log

/**
 * Utility class for network operations
 */
object NetworkUtils {
    
    /**
     * Configure network policy to allow network operations on the main thread
     * This should only be used for development/testing.
     * In production, all network operations should be on background threads.
     */
    fun configureNetworkPolicy() {
        Log.d("NetworkUtils", "Configuring network policy")
        
        // This policy allows network operations on the main thread
        // Not recommended for production code but can be useful for testing
        val policy = StrictMode.ThreadPolicy.Builder()
            .permitAll()
            .build()
        
        StrictMode.setThreadPolicy(policy)
    }
} 