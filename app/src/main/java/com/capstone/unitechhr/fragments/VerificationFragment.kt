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

class VerificationFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var verifyButton: Button
    private lateinit var backToLoginButton: Button
    private lateinit var resendCodeLink: TextView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var instructionsText: TextView
    
    private var userEmail: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_verification, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get email from arguments
        userEmail = arguments?.getString("email")
        
        // Initialize views
        verifyButton = view.findViewById(R.id.verify_button)
        backToLoginButton = view.findViewById(R.id.back_to_login_button)
        resendCodeLink = view.findViewById(R.id.resend_code_link)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        instructionsText = view.findViewById(R.id.instructions_text)
        
        // Hide email input and show verification code inputs
        view.findViewById<View>(R.id.email_input)?.visibility = View.GONE
        view.findViewById<View>(R.id.digit1)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit2)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit3)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit4)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit5)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.digit6)?.visibility = View.VISIBLE
        view.findViewById<View>(R.id.verification_code_container)?.visibility = View.VISIBLE
        
        // Update text to reflect code verification flow
        verifyButton.text = "Verify"
        instructionsText.text = "Enter the 6-digit verification code that was sent when you registered."
        
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
            resendVerificationCode()
        }
        
        // Add a logout button if the user is logged in
        val logoutButton = view.findViewById<Button>(R.id.logout_button)
        logoutButton?.visibility = View.GONE
        
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
        
        // Observe resend verification code result
        authViewModel.resendVerificationCodeResult.observe(viewLifecycleOwner) { result ->
            progressIndicator.visibility = View.GONE
            
            result.fold(
                onSuccess = { code ->
                    // Show success message with the code (for demo purposes)
                    Toast.makeText(
                        requireContext(),
                        "New verification code sent! For demo: $code",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onFailure = { exception ->
                    // Show error message
                    Toast.makeText(
                        requireContext(),
                        "Failed to resend code: ${exception.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            )
        }
    }
    
    private fun verifyWithCode() {
        // If we don't have the email, we can't verify
        if (userEmail == null) {
            Toast.makeText(requireContext(), "Email address is missing. Please go back to registration.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get verification code
        val digit1 = view?.findViewById<TextView>(R.id.digit1)?.text.toString()
        val digit2 = view?.findViewById<TextView>(R.id.digit2)?.text.toString()
        val digit3 = view?.findViewById<TextView>(R.id.digit3)?.text.toString()
        val digit4 = view?.findViewById<TextView>(R.id.digit4)?.text.toString()
        val digit5 = view?.findViewById<TextView>(R.id.digit5)?.text.toString()
        val digit6 = view?.findViewById<TextView>(R.id.digit6)?.text.toString()
        
        val code = digit1 + digit2 + digit3 + digit4 + digit5 + digit6
        
        // Validate code
        if (code.length != 6) {
            Toast.makeText(requireContext(), "Please enter the complete 6-digit code", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading indicator and disable verify button
        progressIndicator.visibility = View.VISIBLE
        verifyButton.isEnabled = false
        
        // Call the viewModel method to verify with code
        authViewModel.verifyEmail(userEmail!!, code)
    }
    
    private fun resendVerificationCode() {
        // If we don't have the email, we can't resend the code
        if (userEmail == null) {
            Toast.makeText(requireContext(), "Email address is missing. Please go back to registration.", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading indicator
        progressIndicator.visibility = View.VISIBLE
        
        // Request a new verification code
        authViewModel.resendVerificationCode(userEmail!!)
    }
} 