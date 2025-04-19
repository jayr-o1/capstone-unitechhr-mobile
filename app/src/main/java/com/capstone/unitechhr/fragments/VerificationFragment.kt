package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
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
    
    private lateinit var emailInput: EditText
    private lateinit var digit1: EditText
    private lateinit var digit2: EditText
    private lateinit var digit3: EditText
    private lateinit var digit4: EditText
    private lateinit var digit5: EditText
    private lateinit var digit6: EditText
    private lateinit var verifyButton: Button
    private lateinit var backToLoginButton: Button
    private lateinit var progressIndicator: CircularProgressIndicator
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_verification, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        emailInput = view.findViewById(R.id.email_input)
        digit1 = view.findViewById(R.id.digit1)
        digit2 = view.findViewById(R.id.digit2)
        digit3 = view.findViewById(R.id.digit3)
        digit4 = view.findViewById(R.id.digit4)
        digit5 = view.findViewById(R.id.digit5)
        digit6 = view.findViewById(R.id.digit6)
        verifyButton = view.findViewById(R.id.verify_button)
        backToLoginButton = view.findViewById(R.id.back_to_login_button)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        
        // Hide the logout, timer elements, and resend code link - they're not needed in this flow
        view.findViewById<TextView>(R.id.resend_code_link)?.visibility = View.GONE
        view.findViewById<TextView>(R.id.timer_text)?.visibility = View.GONE
        view.findViewById<Button>(R.id.logout_button)?.visibility = View.GONE
        
        // Update instruction text
        view.findViewById<TextView>(R.id.instructions_text)?.text = 
            "Enter your email and the 6-digit verification code that was sent to you when you registered."
        
        // Set up the automatic focus change for verification code input
        setupVerificationCodeInput()
        
        // Set up click listeners
        verifyButton.setOnClickListener {
            verifyCode()
        }
        
        backToLoginButton.setOnClickListener {
            findNavController().navigate(R.id.action_verificationFragment_to_loginFragment)
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
    }
    
    private fun setupVerificationCodeInput() {
        // Helper function to move to next digit box
        val moveToNext = { current: EditText, next: EditText? ->
            current.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (s?.length == 1) {
                        next?.requestFocus()
                    }
                }
                
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        
        // Set up automatic focus change
        moveToNext(digit1, digit2)
        moveToNext(digit2, digit3)
        moveToNext(digit3, digit4)
        moveToNext(digit4, digit5)
        moveToNext(digit5, digit6)
    }
    
    private fun verifyCode() {
        val email = emailInput.text.toString().trim()
        if (email.isEmpty()) {
            emailInput.error = "Please enter your email"
            return
        }
        
        // Combine all digits to form the verification code
        val verificationCode = digit1.text.toString() +
                digit2.text.toString() +
                digit3.text.toString() +
                digit4.text.toString() +
                digit5.text.toString() +
                digit6.text.toString()
        
        // Validate the code length
        if (verificationCode.length != 6) {
            Toast.makeText(
                requireContext(),
                "Please enter all 6 digits",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Show loading indicator and disable verify button
        progressIndicator.visibility = View.VISIBLE
        verifyButton.isEnabled = false
        
        // We need to login first with the entered email to retrieve the user
        authViewModel.loginForVerification(email, verificationCode)
    }
} 