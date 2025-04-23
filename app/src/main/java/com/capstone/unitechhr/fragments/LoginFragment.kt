package com.capstone.unitechhr.fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import com.capstone.unitechhr.R
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.util.Log
import android.os.Handler
import android.os.Looper
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.Context
import com.capstone.unitechhr.MainActivity

class LoginFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var googleSignInButton: View
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var appTitle: TextView
    private lateinit var appDescription: TextView
    
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var signInLauncher: ActivityResultLauncher<Intent>
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if we arrived here from a logout action
        val fromLogout = arguments?.getBoolean("from_logout", false) ?: false
        
        try {
            // Get required configuration
            val webClientId = getString(R.string.web_client_id)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            
            // Get Google client
            val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
            
            if (fromLogout) {
                // More aggressive approach to clear Google sign-in state
                // 1. First revoke access (removes permissions)
                googleSignInClient.revokeAccess().addOnCompleteListener {
                    // 2. Then sign out (clears token)
                    googleSignInClient.signOut().addOnCompleteListener {
                        Log.d("LoginFragment", "Completed aggressive Google sign-out")
                        
                        // 3. Clear shared preferences for extra safety
                        context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                               ?.edit()
                               ?.putBoolean("is_logged_out", true)
                               ?.apply()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error during Google sign-out", e)
        }
        
        // Initialize Google Sign-In Launcher
        signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            // Always reset the UI first to ensure it's in a consistent state
            Handler(Looper.getMainLooper()).post {
                if (isAdded() && !isDetached()) {
                    progressIndicator.visibility = View.GONE
                    googleSignInButton.isEnabled = true
                    googleSignInButton.visibility = View.VISIBLE
                    
                    // Show the card view again
                    view?.findViewById<androidx.cardview.widget.CardView>(R.id.google_sign_in_card)?.visibility = View.VISIBLE
                }
            }
            
            if (result.resultCode == Activity.RESULT_OK) {
                try {
                    Log.d("LoginFragment", "Got successful result from Google Sign-In")
                    val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    
                    if (task.isSuccessful) {
                        Log.d("LoginFragment", "Google Sign-In task successful, handling result")
                    } else {
                        Log.d("LoginFragment", "Google Sign-In task not successful: ${task.exception?.message}")
                    }
                    
                    // Force flag to ensure we're not considered logged out anymore
                    context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                        ?.edit()
                        ?.putBoolean("is_logged_out", false)
                        ?.apply()
                    
                    authViewModel.handleSignInResult(requireContext(), task)
                    
                    // EMERGENCY: Directly trigger navigation after a short delay
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (isAdded() && !isDetached()) {
                            try {
                                Log.d("LoginFragment", "DIRECT LAUNCHER NAVIGATION: Attempting to go to home screen")
                                findNavController().navigate(
                                    R.id.homeFragment,
                                    null,
                                    NavOptions.Builder()
                                        .setPopUpTo(R.id.nav_graph, true)
                                        .build()
                                )
                            } catch (e: Exception) {
                                Log.e("LoginFragment", "Error in launcher direct navigation", e)
                            }
                        }
                    }, 2000) // Give the auth process time to complete
                } catch (e: Exception) {
                    Log.e("LoginFragment", "Error processing Google sign-in result", e)
                    if (isAdded() && !isDetached()) {
                        Toast.makeText(
                            requireContext(),
                            "Sign-in failed: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            } else {
                // Provide more informative error message based on result code
                when (result.resultCode) {
                    Activity.RESULT_CANCELED -> {
                        if (isAdded() && !isDetached()) {
                            Toast.makeText(
                                requireContext(),
                                "Sign in was cancelled. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.d("LoginFragment", "Google sign-in was cancelled by user")
                    }
                    else -> {
                        if (isAdded() && !isDetached()) {
                            Toast.makeText(
                                requireContext(),
                                "Sign in failed with code: ${result.resultCode}. Please try again.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.e("LoginFragment", "Google sign-in failed with result code: ${result.resultCode}")
                    }
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // IMMEDIATE CHECK: If we have the system property set, ensure we stay on login
        if (System.getProperty("com.capstone.unitechhr.user.logged_out") == "true") {
            Log.d("LoginFragment", "Found system property for logout, staying on login")
            
            // Clear the property now that we're handling it
            System.clearProperty("com.capstone.unitechhr.user.logged_out")
            
            // Ensure SharedPreferences is consistent
            context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                   ?.edit()
                   ?.putBoolean("is_logged_out", true)
                   ?.apply()
                   
            // Ensure bottom navigation is hidden
            activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE
            
            // Exit early - no auto login checks
            setupViews(view)
            return
        }
        
        // Initialize views
        setupViews(view)
        
        // Check if we arrived from logout or if logged out flag is set
        val fromLogout = arguments?.getBoolean("from_logout", false) ?: false
        val isLoggedOut = context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            ?.getBoolean("is_logged_out", false) ?: false
        
        // Ensure bottom navigation is hidden if we're on login
        activity?.findViewById<View>(R.id.bottom_navigation)?.visibility = View.GONE
        
        if (fromLogout || isLoggedOut) {
            Log.d("LoginFragment", "Coming from logout or is_logged_out flag is set, preventing auto-login")
            // Force reset state
            context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                   ?.edit()
                   ?.putBoolean("is_logged_out", true)
                   ?.apply()
        } else {
            // Only check sign-in status after a delay to ensure any logout has been processed
            Handler(Looper.getMainLooper()).postDelayed({
                if (isAdded() && !isDetached() && !fromLogout && !isLoggedOut) {
                    if (authViewModel.checkSignInStatus(requireContext())) {
                        Log.d("LoginFragment", "User already signed in, navigating to home")
                        navigateToHome()
                    }
                }
            }, 1000) // 1 second delay
        }
        
        // Observe sign-in result
        authViewModel.signInResult.observe(viewLifecycleOwner) { result ->
            // Always ensure UI is in a consistent state first
            progressIndicator.visibility = View.GONE
            googleSignInButton.isEnabled = true
            googleSignInButton.visibility = View.VISIBLE
            view?.findViewById<androidx.cardview.widget.CardView>(R.id.google_sign_in_card)?.visibility = View.VISIBLE
            
            result.fold(
                onSuccess = { email ->
                    try {
                        Log.d("LoginFragment", "Sign-in successful for: $email")
                        
                        // Clear the logged_out flag since we're signing in now
                        context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                               ?.edit()
                               ?.putBoolean("is_logged_out", false)
                               ?.apply()
                        
                        // Clear any system property that might be set
                        System.clearProperty("com.capstone.unitechhr.user.logged_out")
                        
                        // Ensure user profile is fully loaded
                        authViewModel.loadCurrentUser(requireContext())
                        
                        // CRITICAL: Force immediate navigation with explicit NavController access
                        val navController = findNavController()
                        Log.d("LoginFragment", "IMMEDIATE NAVIGATION: NavController available = ${navController != null}")
                        
                        Log.d("LoginFragment", "Attempting DIRECT navigation to homeFragment")
                        navController.navigate(
                            R.id.homeFragment,
                            null,
                            NavOptions.Builder()
                                .setPopUpTo(R.id.nav_graph, true)
                                .build()
                        )
                        
                        Log.d("LoginFragment", "Direct navigation command issued")
                    } catch (e: Exception) {
                        Log.e("LoginFragment", "Error during successful sign-in navigation", e)
                        
                        // Last resort emergency navigation attempt
                        try {
                            Log.d("LoginFragment", "Attempting emergency navigation")
                            findNavController().navigate(R.id.homeFragment)
                        } catch (e2: Exception) {
                            Log.e("LoginFragment", "ALL navigation attempts failed", e2)
                        }
                    }
                },
                onFailure = { exception ->
                    Log.e("LoginFragment", "Sign-in failed", exception)
                    if (isAdded() && !isDetached()) {
                        Toast.makeText(
                            requireContext(),
                            "Sign in failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }
    }
    
    private fun setupViews(view: View) {
        // Initialize views
        googleSignInButton = view.findViewById(R.id.google_sign_in_button)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        appTitle = view.findViewById(R.id.app_title)
        appDescription = view.findViewById(R.id.app_description)
        
        // Set up Google Sign-In
        val webClientId = getString(R.string.web_client_id)
        googleSignInClient = authViewModel.getGoogleSignInClient(requireContext(), webClientId)
        
        // Set up click listeners
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }
    }
    
    private fun signInWithGoogle() {
        // Get web client ID and check if it's properly configured
        val webClientId = getString(R.string.web_client_id)
        
        // Check if webClientId is still a placeholder
        if (webClientId == "YOUR_WEB_CLIENT_ID_HERE") {
            Toast.makeText(
                requireContext(),
                "Google Sign-In is not properly configured. Please update web_client_id in strings.xml",
                Toast.LENGTH_LONG
            ).show()
            return
        }
        
        // Find the parent CardView of the Google sign-in button to hide both
        val signInCardView = view?.findViewById<androidx.cardview.widget.CardView>(R.id.google_sign_in_card)
        
        try {
            Log.d("LoginFragment", "Starting Google sign-in process...")
            
            // Make sure the views are reset to a known state first
            progressIndicator.visibility = View.VISIBLE
            googleSignInButton.isEnabled = false
            googleSignInButton.visibility = View.INVISIBLE
            signInCardView?.visibility = View.INVISIBLE
            
            // Force logout any existing Google account to ensure clean sign-in
            // This is especially important after a user has logged out
            val isLoggedOut = context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                ?.getBoolean("is_logged_out", false) ?: false
                
            Log.d("LoginFragment", "isLoggedOut flag before sign-in: $isLoggedOut")
                
            // Completely reset the sign-in state
            // Create a new Google client with fresh configuration
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .requestProfile()
                .build()
            
            googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
            
            // Clear the is_logged_out flag before signin attempt to prevent conflicts
            context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                ?.edit()
                ?.putBoolean("is_logged_out", false)
                ?.apply()
            
            // Clear any system property that might be set
            System.clearProperty("com.capstone.unitechhr.user.logged_out")
            
            Log.d("LoginFragment", "Launching Google Sign-In intent")
            
            // Launch Google Sign-In
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
            
        } catch (e: Exception) {
            // If there's any error, restore the UI and show error
            progressIndicator.visibility = View.GONE
            googleSignInButton.isEnabled = true
            googleSignInButton.visibility = View.VISIBLE
            signInCardView?.visibility = View.VISIBLE
            
            Log.e("LoginFragment", "Error during sign-in attempt", e)
            Toast.makeText(
                requireContext(),
                "Sign-in failed: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun navigateToHome() {
        try {
            Log.d("LoginFragment", "Attempting navigation to home screen...")
            
            if (!isAdded() || isDetached()) {
                Log.e("LoginFragment", "Cannot navigate: Fragment not attached to activity")
                return
            }
            
            // Make sure to clear any "logged out" flags
            context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                   ?.edit()
                   ?.putBoolean("is_logged_out", false)
                   ?.apply()
            
            // Clear system property if set
            System.clearProperty("com.capstone.unitechhr.user.logged_out")
            
            // Attempt navigation with try-catch for safety
            findNavController().navigate(
                R.id.action_loginFragment_to_homeFragment,
                null,
                NavOptions.Builder()
                    .setPopUpTo(R.id.loginFragment, true)
                    .build()
            )
            
            Log.d("LoginFragment", "Successfully navigated to home screen")
        } catch (e: Exception) {
            Log.e("LoginFragment", "Error during navigation to home", e)
            // Try a fallback navigation without popUp options if the first attempt fails
            try {
                findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
            } catch (e2: Exception) {
                Log.e("LoginFragment", "Fallback navigation also failed", e2)
            }
        }
    }

    /**
     * Show a success dialog when login is successful
     */
    private fun showSuccessDialog(email: String) {
        // Check if we're coming from logout or if logged out flag is set
        val fromLogout = arguments?.getBoolean("from_logout", false) ?: false
        val isLoggedOut = context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            ?.getBoolean("is_logged_out", false) ?: false
            
        if (fromLogout || isLoggedOut) {
            Log.d("LoginFragment", "Blocking success dialog because we came from logout or logged out state")
            return
        }
        
        // Get user's name from SharedPreferences directly to ensure we have the latest data
        val prefs = context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
        val displayName = prefs?.getString("current_user_name", null) 
            ?: authViewModel.currentUser.value?.displayName 
            ?: email.substringBefore("@")
            ?: "User"
        
        // Get first name only for more personal greeting
        val firstName = displayName.split(" ")[0]
        
        Log.d("LoginFragment", "Showing welcome dialog for user: $firstName (full name: $displayName)")
        
        MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.ic_check_circle)
            .setTitle("Welcome, $firstName!")
            .setMessage("You've successfully signed in")
            .setPositiveButton("Continue") { dialog, _ ->
                dialog.dismiss()
                navigateToHome()
            }
            .setCancelable(false)
            .show()
    }

    override fun onResume() {
        super.onResume()
        
        // EMERGENCY CHECK: If we have authenticated user but are still on login screen, force navigation
        val isAuthenticated = context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            ?.getBoolean("is_logged_out", true)?.not() ?: false
            
        val userEmail = context?.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
            ?.getString("current_user_email", null)
            
        if (isAuthenticated && userEmail != null) {
            Log.d("LoginFragment", "EMERGENCY: Detected authenticated user in onResume: $userEmail")
            try {
                // Force navigation to home
                findNavController().navigate(
                    R.id.homeFragment,
                    null,
                    NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build()
                )
                Log.d("LoginFragment", "Successfully performed emergency navigation in onResume")
            } catch (e: Exception) {
                Log.e("LoginFragment", "Failed emergency navigation in onResume", e)
                
                try {
                    // One last attempt with activity reference
                    (activity as? MainActivity)?.let { mainActivity ->
                        Log.d("LoginFragment", "Attempting navigation via MainActivity")
                        mainActivity.findViewById<View>(R.id.bottom_navigation)?.visibility = View.VISIBLE
                        mainActivity.getNavController().navigate(R.id.homeFragment)
                    }
                } catch (e2: Exception) {
                    Log.e("LoginFragment", "All navigation attempts failed", e2)
                }
            }
        }
    }
}