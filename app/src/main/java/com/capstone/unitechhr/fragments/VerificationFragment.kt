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
import com.google.android.material.progressindicator.CircularProgressIndicator
import android.content.Intent
import android.net.Uri

class VerificationFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var verifyButton: Button
    private lateinit var backToLoginButton: Button
    private lateinit var resendCodeLink: TextView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var instructionsText: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_verification, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        verifyButton = view.findViewById(R.id.verify_button)
        backToLoginButton = view.findViewById(R.id.back_to_login_button)
        resendCodeLink = view.findViewById(R.id.resend_code_link)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        instructionsText = view.findViewById(R.id.instructions_text)
        
        // Show verification code inputs
        view.findViewById<View>(R.id.email_input)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit1)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit2)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit3)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit4)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit5)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit6)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.verification_code_container)?.visibility = View.VISIBLE
        
        // Update text to reflect code verification flow
        verifyButton.text = "Verify"
        instructionsText.text = "Enter your email and the 6-digit verification code that was sent to you when you registered."
        
        // Show resend link
        resendCodeLink.visibility = View.VISIBLE
        
        // Hide timer
        view.findViewById<TextView>(R.id.timer_text)?.visibility = View.GONE
        
        // Set up click listeners
        verifyButton.setOnClickListener {
            verifyWithCode()
        }
        
        backToLoginButton.setOnClickListener {
            findNavController().navigate(R.id.action_verificationFragment_to_loginFragment)
        }
        
        resendCodeLink.setOnClickListener {
            resendVerificationEmail()
        }
        
        // Add a logout button if the user is logged in
        val logoutButton = view.findViewById<Button>(R.id.logout_button)
        if (authViewModel.isUserLoggedIn()) {
            logoutButton?.visibility = View.VISIBLE
            logoutButton?.setOnClickListener {
                authViewModel.logout()
                findNavController().navigate(R.id.action_verificationFragment_to_loginFragment)
            }
        } else {
            logoutButton?.visibility = View.GONE
        }
        
        // Observe verification result
        authViewModel.verifyEmailResult.observe(viewLifecycleOwner) { result ->
            progressIndicator.visibility = View.GONE
            verifyButton.isEnabled = true
            
            result.fold(
                onSuccess = {
                    // Show success message
                    Toast.makeText(
                        requireContext(),
                        "Email verified successfully! You can now log in.",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Navigate to login screen
                    findNavController().navigate(R.id.action_verificationFragment_to_loginFragment)
                },
                onFailure = { exception ->
                    // Show error message
                    Toast.makeText(
                        requireContext(),
                        "Verification failed: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
        
        // Observe resend verification email result
        authViewModel.resendVerificationEmailResult.observe(viewLifecycleOwner) { result ->
            progressIndicator.visibility = View.GONE
            
            result.fold(
                onSuccess = {
                    // Show success message
                    Toast.makeText(
                        requireContext(),
                        "Verification email sent. Please check your inbox.",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onFailure = { exception ->
                    // Show error message
                    Toast.makeText(
                        requireContext(),
                        "Failed to send verification email: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
    
    private fun verifyWithCode() {
        // Get the email and verification code
        val email = view?.findViewById<TextView>(R.id.email_input)?.text.toString().trim()
        val digit1 = view?.findViewById<TextView>(R.id.digit1)?.text.toString()
        val digit2 = view?.findViewById<TextView>(R.id.digit2)?.text.toString()
        val digit3 = view?.findViewById<TextView>(R.id.digit3)?.text.toString()
        val digit4 = view?.findViewById<TextView>(R.id.digit4)?.text.toString()
        val digit5 = view?.findViewById<TextView>(R.id.digit5)?.text.toString()
        val digit6 = view?.findViewById<TextView>(R.id.digit6)?.text.toString()
        
        val code = digit1 + digit2 + digit3 + digit4 + digit5 + digit6
        
        // Validate inputs
        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (code.length != 6) {
            Toast.makeText(requireContext(), "Please enter the complete 6-digit code", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading indicator and disable verify button
        progressIndicator.visibility = View.VISIBLE
        verifyButton.isEnabled = false
        
        // Show a toast message to let the user know the verification is in progress
        Toast.makeText(
            requireContext(),
            "Verifying your code...",
            Toast.LENGTH_SHORT
        ).show()
        
        // Call the viewModel method to verify with code
        authViewModel.verifyWithCode(email, code)
    }
    
    private fun hideVerificationCodeInputs(view: View) {
        // Hide all digit inputs and the email input
        view.findViewById<View>(R.id.email_input)?.visibility = View.GONE
        view.findViewById<View>(R.id.digit1)?.visibility = View.GONE
        view.findViewById<View>(R.id.digit2)?.visibility = View.GONE
        view.findViewById<View>(R.id.digit3)?.visibility = View.GONE
        view.findViewById<View>(R.id.digit4)?.visibility = View.GONE
        view.findViewById<View>(R.id.digit5)?.visibility = View.GONE
        view.findViewById<View>(R.id.digit6)?.visibility = View.GONE
        view.findViewById<View>(R.id.verification_code_container)?.visibility = View.GONE
    }
    
    private fun resendVerificationEmail() {
        // Show loading indicator
        progressIndicator.visibility = View.VISIBLE
        
        // Request a new verification email
        authViewModel.resendVerificationEmail()
    }
} 