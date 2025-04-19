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

class LoginFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var registerLink: TextView
    private lateinit var forgotPasswordLink: TextView
    private lateinit var progressIndicator: CircularProgressIndicator

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
        
        // Set up click listeners
        loginButton.setOnClickListener {
            login()
        }
        
        registerLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registrationFragment)
        }
        
        forgotPasswordLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_forgotPasswordFragment)
        }
        
        // Add verification link
        val verifyAccountLink: TextView = view.findViewById(R.id.verify_account_link)
        verifyAccountLink.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_verificationFragment)
        }
        
        // Observe login result
        authViewModel.loginResult.observe(viewLifecycleOwner) { result ->
            progressIndicator.visibility = View.GONE
            loginButton.isEnabled = true
            
            result.fold(
                onSuccess = { user ->
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
                    
                    // If it's a verification error, suggest verifying the email
                    if (errorMessage.contains("not verified", ignoreCase = true)) {
                        // Show a dialog with more detailed information
                        AlertDialog.Builder(requireContext())
                            .setTitle("Email Not Verified")
                            .setMessage("Your email is not verified. You need to verify your email before you can log in. Check your email for a verification code or click 'Verify Account' below.")
                            .setPositiveButton("Verify Account") { _, _ ->
                                findNavController().navigate(R.id.action_loginFragment_to_verificationFragment)
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    } else {
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
        authViewModel.login(email, password)
    }
}