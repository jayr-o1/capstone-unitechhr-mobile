package com.capstone.unitechhr.fragments

import android.os.Bundle
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

class ProfileFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var logoutButton: Button
    private lateinit var emailTextView: TextView
    private lateinit var nameTextView: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        logoutButton = view.findViewById(R.id.logoutButton)
        emailTextView = view.findViewById(R.id.emailTextView)
        nameTextView = view.findViewById(R.id.nameTextView)
        
        // Load current user data
        authViewModel.loadCurrentUser(requireContext())
        
        // Display current user data
        authViewModel.currentUser.observe(viewLifecycleOwner) { userData ->
            emailTextView.text = userData?.email ?: "Not logged in"
            nameTextView.text = userData?.displayName ?: "User"
        }
        
        // Set up logout button
        logoutButton.setOnClickListener {
            // Get Google Sign In client for sign out
            val webClientId = getString(R.string.web_client_id)
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build()
            val googleSignInClient = GoogleSignIn.getClient(requireContext(), gso)
            
            // Show a toast that logout is in progress
            Toast.makeText(requireContext(), "Signing out...", Toast.LENGTH_SHORT).show()
            
            // Revoke access before sign out (more thorough than just sign out)
            googleSignInClient.revokeAccess().addOnCompleteListener {
                // Then sign out
                googleSignInClient.signOut().addOnCompleteListener {
                    // Perform app sign out
                    authViewModel.logout(requireContext(), null) // Pass null since we already signed out
                    
                    // Create bundle with from_logout flag
                    val bundle = Bundle().apply {
                        putBoolean("from_logout", true)
                    }
                    
                    // Navigate to login screen with the flag
                    findNavController().navigate(
                        R.id.loginFragment,
                        bundle,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.nav_graph, true)
                            .build()
                    )
                }
            }
        }
    }
} 