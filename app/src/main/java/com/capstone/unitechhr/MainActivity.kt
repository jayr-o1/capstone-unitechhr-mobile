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

        // Check login status first before setting up UI
        val isLoggedOut = checkInitialLoginState()
        Log.d(TAG, "Is logged out on startup: $isLoggedOut")
        
        // If the user is logged in, load their profile data
        if (!isLoggedOut) {
            authViewModel.loadCurrentUser(this)
            Log.d(TAG, "Loading user profile on startup")
        }

        // Set up the toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainer) as NavHostFragment
        val inflater = navHostFragment.navController.navInflater
        val graph = inflater.inflate(R.navigation.nav_graph)
        
        // Set start destination based on login status
        graph.setStartDestination(if (isLoggedOut) R.id.loginFragment else R.id.homeFragment)
        navController = navHostFragment.navController
        navController.graph = graph
        
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
        
        // Add destination changed listener to manage bottom nav visibility
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Show bottom navigation only for main tabs
            val showBottomNav = when (destination.id) {
                R.id.homeFragment, R.id.jobListingFragment, R.id.profileFragment -> true
                else -> false
            }
            
            // Log and update bottom nav visibility
            Log.d(TAG, "Navigation destination changed to: ${destination.label}, showBottomNav: $showBottomNav")
            bottomNavigationView.visibility = if (showBottomNav) android.view.View.VISIBLE else android.view.View.GONE
        }

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

    /**
     * Checks if the user is logged in or out
     * @return true if the user is logged out, false if logged in
     */
    private fun checkInitialLoginState(): Boolean {
        // First check for system property that would indicate a logout across process boundaries
        val systemLoggedOut = System.getProperty("com.capstone.unitechhr.user.logged_out")
        if (systemLoggedOut == "true") {
            Log.d(TAG, "Found system property indicating user is logged out")
            // Clear this flag now that we've detected it
            System.clearProperty("com.capstone.unitechhr.user.logged_out")
            // Ensure shared prefs are consistent
            getSharedPreferences("auth_prefs", MODE_PRIVATE).edit()
                .putBoolean("is_logged_out", true)
                .apply()
            return true
        }
        
        val sharedPreferences = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        
        // Next, directly check if logged_out flag is set
        if (sharedPreferences.getBoolean("is_logged_out", false)) {
            Log.d(TAG, "User is flagged as logged out in shared prefs")
            return true
        }
        
        // Check if we have no stored email (another indicator of being logged out)
        val storedEmail = sharedPreferences.getString("current_user_email", null)
        if (storedEmail == null) {
            // No email stored means not logged in - set logged out flag for consistency
            sharedPreferences.edit()
                .putBoolean("is_logged_out", true)
                .apply()
            Log.d(TAG, "No stored email, treating as logged out")
            return true
        }
        
        // If we reach here, we seem to be logged in
        // Reset the is_logged_out flag to false to ensure consistency
        sharedPreferences.edit()
            .putBoolean("is_logged_out", false)
            .apply()
            
        Log.d(TAG, "User appears to be logged in with email: $storedEmail")
        return false
    }

    // Public method to access the NavController
    fun getNavController(): NavController {
        return navController
    }
}