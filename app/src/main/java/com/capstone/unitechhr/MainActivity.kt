package com.capstone.unitechhr

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.view.View
import android.view.WindowManager
import androidx.core.content.ContextCompat
import android.os.Build
import android.widget.Toast

class MainActivity : AppCompatActivity() {
    
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration
    private val authViewModel: AuthViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Make status bar transparent with visible icons
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        
        // Make status bar icons dark or light based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // For light backgrounds, use dark status bar icons
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        } else {
            // For older versions, just ensure system UI is visible
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
        }
        
        setContentView(R.layout.activity_main)
        
        // Set up the toolbar but hide it
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.hide() // Hide the action bar completely for all fragments
        
        // Setup Navigation
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainer) as NavHostFragment
        navController = navHostFragment.navController
        
        // Get the nav graph
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        navGraph.setStartDestination(R.id.loginFragment)
        navController.graph = navGraph
        
        // Define top level destinations
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.homeFragment,
                R.id.jobListingFragment,
                R.id.interviewListFragment,
                R.id.onboardingListFragment,
                R.id.profileFragment
            )
        )
        
        // Setup ActionBar with NavController but it's hidden
        setupActionBarWithNavController(navController, appBarConfiguration)
        
        // Setup Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        
        // Show/hide bottom navigation based on current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Hide bottom navigation on auth screens
            if (destination.id == R.id.loginFragment || 
                destination.id == R.id.registrationFragment ||
                destination.id == R.id.forgotPasswordFragment ||
                destination.id == R.id.verificationFragment ||
                destination.id == R.id.resetPasswordFragment) {
                bottomNavigationView.visibility = android.view.View.GONE
            } else {
                bottomNavigationView.visibility = android.view.View.VISIBLE
            }
            
            // Find the appropriate menu item ID based on the destination
            val menuItemId = when (destination.id) {
                R.id.homeFragment -> R.id.homeFragment
                R.id.jobListingFragment -> R.id.jobListingFragment
                R.id.interviewListFragment -> R.id.interviewListFragment
                R.id.onboardingListFragment -> R.id.onboardingListFragment
                R.id.profileFragment -> R.id.profileFragment
                else -> null
            }
            
            // Update the selected menu item if we have a valid ID
            if (menuItemId != null) {
                bottomNavigationView.menu.findItem(menuItemId)?.isChecked = true
            }
        }
        
        // Instead of just using setupWithNavController, we'll handle navigation ourselves
        bottomNavigationView.setOnItemSelectedListener { item ->
            // Create navigation options that clear the back stack
            val navOptions = NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setPopUpTo(navController.graph.startDestinationId, false)
                .build()
            
            // Check if user is not verified - redirect to verification if needed
            val currentUser = authViewModel.getCurrentUser()
            if (currentUser != null && !currentUser.isEmailVerified) {
                // User is logged in but not verified - show a message and redirect to verification
                Toast.makeText(this, "Please verify your email before accessing the app", Toast.LENGTH_SHORT).show()
                
                // Check if already on verification fragment to avoid infinite loop
                if (navController.currentDestination?.id != R.id.verificationFragment) {
                    // Navigate to verification fragment
                    authViewModel.resendVerificationEmail()
                    navController.navigate(R.id.verificationFragment)
                }
                return@setOnItemSelectedListener false
            }
                
            when (item.itemId) {
                R.id.homeFragment -> {
                    navController.navigate(R.id.homeFragment, null, navOptions)
                    true
                }
                R.id.jobListingFragment -> {
                    navController.navigate(R.id.jobListingFragment, null, navOptions)
                    true
                }
                R.id.interviewListFragment -> {
                    navController.navigate(R.id.interviewListFragment, null, navOptions)
                    true
                }
                R.id.onboardingListFragment -> {
                    navController.navigate(R.id.onboardingListFragment, null, navOptions)
                    true
                }
                R.id.profileFragment -> {
                    navController.navigate(R.id.profileFragment, null, navOptions)
                    true
                }
                else -> false
            }
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}