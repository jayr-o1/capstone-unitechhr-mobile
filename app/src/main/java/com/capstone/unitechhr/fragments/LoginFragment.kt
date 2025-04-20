package com.capstone.unitechhr.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.NavOptions
import com.capstone.unitechhr.R
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.app.AlertDialog
import androidx.core.os.bundleOf

class LoginFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var verificationNotice: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_login, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views with the correct IDs from layout
        emailEditText = view.findViewById(R.id.email_input)
        passwordEditText = view.findViewById(R.id.password)
        loginButton = view.findViewById(R.id.login_button)
        registerLink = view.findViewById(R.id.registerLink)
        forgotPasswordLink = view.findViewById(R.id.forgotPasswordLink)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        
        // Get verification notice view (you'll need to add this to your layout)
        verificationNotice = view.findViewById(R.id.verification_notice)
        verificationNotice.visibility = View.GONE
        
        // Set up click listeners
        loginButton.setOnClickListener {
            login()
        }
        
        registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registrationFragment)
        }
        
        forgotPasswordLink.setOnClickListener {
            // You may want to implement this later
            Toast.makeText(requireContext(), "Function not available", Toast.LENGTH_SHORT).show()
        }
        
        // Add click listener to verification notice
        verificationNotice.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                val bundle = bundleOf("email" to email)
                findNavController().navigate(R.id.action_loginFragment_to_verificationFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "Please enter your email first", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe login result
        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            progressIndicator.visibility = View.GONE
            loginButton.isEnabled = true
            
            result.fold(
                onSuccess = { email ->
                    // Login successful, navigate to home
                    findNavController().navigate(
                        R.id.action_loginFragment_to_homeFragment,
                        null,
                        NavOptions.Builder()
                            .setPopUpTo(R.id.loginFragment, true)
                            .build()
                    )
                },
                onFailure = { exception ->
                    val errorMessage = exception.message ?: "Login failed"
                    
                    // If it's a verification error, show verification notice
                    if (errorMessage.contains("not verified", ignoreCase = true)) {
                        // Show verification notice above email field
                        verificationNotice.text = "Please verify your account first. Verify Now"
                        verificationNotice.visibility = View.VISIBLE
                    } else {
                        // Hide verification notice for other errors
                        verificationNotice.visibility = View.GONE
                        
                        // Show toast with error message
                        Toast.makeText(
                            requireContext(),
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }
    }
    
    private fun login() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        
        // Basic validation
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return
        }
        
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return
        }
        
        // Show loading indicator and disable login button
        progressIndicator.visibility = View.VISIBLE
        loginButton.isEnabled = false
        
        // Attempt login
        authViewModel.login(requireContext(), email, password)
    }
}