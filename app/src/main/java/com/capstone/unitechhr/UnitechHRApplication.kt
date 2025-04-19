package com.capstone.unitechhr

import android.app.Application
import android.util.Log
import com.capstone.unitechhr.utils.NetworkUtils

class UnitechHRApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        Log.d("UnitechHRApplication", "Initializing application")
        
        // Configure network policy for email operations
        NetworkUtils.configureNetworkPolicy()
    }
} 