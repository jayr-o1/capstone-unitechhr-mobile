package com.capstone.unitechhr.fragments

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.material.button.MaterialButton
import java.lang.ref.WeakReference

class ProfileFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var logoutButton: MaterialButton
    private lateinit var emailTextView: TextView
    private lateinit var nameTextView: TextView
    
    // Store a weak reference to context to avoid memory leaks
    private var weakContext: WeakReference<Context>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Store weak reference to context when fragment attaches
        weakContext = WeakReference(context)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        logoutButton = view.findViewById(R.id.logoutButton)
        emailTextView = view.findViewById(R.id.emailTextView)
        nameTextView = view.findViewById(R.id.nameTextView)
        
        // Set up back button
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Store context in weak reference for safety
        weakContext = WeakReference(requireContext())
        
        // Load current user data
        authViewModel.loadCurrentUser(requireContext())
        
        // Display current user data
        authViewModel.currentUser.observe(viewLifecycleOwner) { userData ->
            emailTextView.text = userData?.email ?: "Not logged in"
            nameTextView.text = userData?.displayName ?: "User"
        }
        
        // Set up resume section click listener
        view.findViewById<View>(R.id.resumeSection).setOnClickListener {
            findNavController().navigate(R.id.action_profileFragment_to_resumeUploadFragment)
        }
        
        // Set up logout button
        logoutButton.setOnClickListener {
            logout()
        }
    }
    
    private fun logout() {
        try {
            // Get current context safely
            val currentContext = weakContext?.get() ?: return
            
            // Show a toast that logout is in progress
            Toast.makeText(currentContext, "Signing out...", Toast.LENGTH_SHORT).show()
            
            // Immediate UI updates to prevent showing bottom nav on login screen
            val bottomNav = activity?.findViewById<View>(R.id.bottom_navigation)
            bottomNav?.visibility = View.GONE
            
            // Set system property to ensure logout persists across app restarts
            System.setProperty("com.capstone.unitechhr.user.logged_out", "true")
            
            // First, clear all shared preferences
            currentContext.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .putBoolean("is_logged_out", true)
                .putLong("logout_timestamp", System.currentTimeMillis())
                .apply()
            
            // Get Google Sign In client for sign out
            val webClientId = getString(R.string.web_client_id)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(currentContext, gso)
            
            // Create bundle with from_logout flag
            val bundle = Bundle().apply {
                putBoolean("from_logout", true)
            }
            
            // Navigate to login immediately to prevent any UI glitches
            findNavController().navigate(
                R.id.loginFragment,
                bundle,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            )
            
            // Continue with logout process in background but with safety checks
            // Store application context to avoid fragment context issues
            val appContext = currentContext.applicationContext
            
            // Revoke access before sign out (more thorough than just sign out)
            googleSignInClient.revokeAccess().addOnCompleteListener {
                // Then sign out (using application context for safety)
                googleSignInClient.signOut().addOnCompleteListener {
                    // Perform app sign out using application context
                    // This prevents the fragment context error
                    authViewModel.logout(appContext, null)
                    Log.d("ProfileFragment", "Completed Google Sign-Out")
                }
            }
        } catch (e: Exception) {
            Log.e("ProfileFragment", "Error during logout", e)
        }
    }
    
    override fun onDetach() {
        super.onDetach()
        // Clear the weak reference when fragment detaches
        weakContext = null
    }
} 