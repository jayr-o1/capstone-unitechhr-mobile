package com.capstone.unitechhr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.capstone.unitechhr.utils.NotificationUtils
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private val authViewModel: AuthViewModel by viewModels()
    private val TAG = "MainActivity"

    // Permission launcher for notification permission
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Notification permission granted
            Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show()
            // Get the FCM token after permission is granted
            retrieveFcmToken()
        } else {
            // Notification permission denied
            Toast.makeText(this, "Notification permission denied. You will not receive job updates.", 
                Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navController = navHostFragment.navController
        
        // Define top level destinations
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.jobListingFragment,
                R.id.profileFragment
            )
        )
        
        // Setup ActionBar with NavController
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Setup Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setupWithNavController(navController)

        // Check and request notification permission
        askNotificationPermission()
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    // Check and request notification permissions for Android 13+
    private fun askNotificationPermission() {
        // Check if we're running on Android 13 or higher
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission is already granted, retrieve token
                    retrieveFcmToken()
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explain to the user why notification permission is needed
                    Toast.makeText(
                        this,
                        "Notification permission is needed to receive job updates",
                        Toast.LENGTH_LONG
                    ).show()
                    // Request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Directly request permission
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // For Android 12 and below, notification permissions are granted upon installation
            retrieveFcmToken()
        }
    }

    // Get FCM token and log it
    private fun retrieveFcmToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.e(TAG, "Fetching FCM token failed", task.exception)
                return@addOnCompleteListener
            }
            
            // Get the token
            val token = task.result
            Log.d(TAG, "FCM Token: $token")
            
            // Subscribe to relevant topics
            NotificationUtils.subscribeToTopic("all_users")
            
            // All users in this app are job seekers
            NotificationUtils.subscribeToTopic("job_seekers")
        }
    }
}