package com.capstone.unitechhr.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.google.android.material.progressindicator.CircularProgressIndicator
import java.util.regex.Pattern
import android.app.AlertDialog

class RegistrationFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var fullNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var termsCheckBox: CheckBox
    private lateinit var registerButton: Button
    private lateinit var loginLink: TextView
    private lateinit var progressIndicator: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_registration, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        fullNameEditText = view.findViewById(R.id.full_name_input)
        emailEditText = view.findViewById(R.id.email_input)
        passwordEditText = view.findViewById(R.id.password)
        confirmPasswordEditText = view.findViewById(R.id.confirm_password)
        termsCheckBox = view.findViewById(R.id.terms_checkbox)
        registerButton = view.findViewById(R.id.register_button)
        loginLink = view.findViewById(R.id.login_link)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        
        // Set up click listeners
        registerButton.setOnClickListener {
            register()
        }
        
        loginLink.setOnClickListener {
            findNavController().navigate(R.id.action_registrationFragment_to_loginFragment)
        }
        
        // Observe registration result
        authViewModel.registerResult.observe(viewLifecycleOwner) { result ->
            progressIndicator.visibility = View.GONE
            registerButton.isEnabled = true
            
            result.fold(
                onSuccess = {
                    // Show detailed success message
                    val message = "Registration successful!\n\nWe've sent a verification link to your email. Please check your inbox (including spam folder) and click the link to verify your account."
                    
                    // Create a dialog for better visibility
                    AlertDialog.Builder(requireContext())
                        .setTitle("Registration Complete")
                        .setMessage(message)
                        .setPositiveButton("Go to Verification") { _, _ ->
                            // Navigate to verification screen instead of login
                            findNavController().navigate(R.id.action_registrationFragment_to_verificationFragment)
                        }
                        .setCancelable(false)
                        .show()
                },
                onFailure = { exception ->
                    // Show error message
                    Toast.makeText(
                        requireContext(),
                        "Registration failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
    
    private fun register() {
        val fullName = fullNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        
        // Basic validation
        if (fullName.isEmpty()) {
            fullNameEditText.error = "Full name is required"
            return
        }
        
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return
        }
        
        if (!isValidEmail(email)) {
            emailEditText.error = "Please enter a valid email address"
            return
        }
        
        if (password.isEmpty()) {
            passwordEditText.error = "Password is required"
            return
        }
        
        if (password.length < 6) {
            passwordEditText.error = "Password must be at least 6 characters"
            return
        }
        
        if (confirmPassword.isEmpty()) {
            confirmPasswordEditText.error = "Please confirm your password"
            return
        }
        
        if (password != confirmPassword) {
            confirmPasswordEditText.error = "Passwords do not match"
            return
        }
        
        if (!termsCheckBox.isChecked) {
            Toast.makeText(
                requireContext(),
                "You must agree to the Terms and Conditions",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Show loading indicator and disable register button
        progressIndicator.visibility = View.VISIBLE
        registerButton.isEnabled = false
        
        // Attempt registration
        authViewModel.register(email, password, fullName)
    }
    
    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "[a-zA-Z0-9+._%-+]{1,256}" +
                    "@" +
                    "[a-zA-Z0-9][a-zA-Z0-9-]{0,64}" +
                    "(" +
                    "." +
                    "[a-zA-Z0-9][a-zA-Z0-9-]{0,25}" +
                    ")+"
        )
        return emailPattern.matcher(email).matches()
    }
}