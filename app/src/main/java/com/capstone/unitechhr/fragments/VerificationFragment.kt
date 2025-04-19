package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.os.CountDownTimer
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
import java.util.concurrent.TimeUnit

class VerificationFragment : Fragment() {
    private val authViewModel: AuthViewModel by activityViewModels()
    
    private lateinit var digit1: EditText
    private lateinit var digit2: EditText
    private lateinit var digit3: EditText
    private lateinit var digit4: EditText
    private lateinit var digit5: EditText
    private lateinit var digit6: EditText
    private lateinit var verifyButton: Button
    private lateinit var resendCodeLink: TextView
    private lateinit var timerText: TextView
    private lateinit var progressIndicator: CircularProgressIndicator
    
    private lateinit var countDownTimer: CountDownTimer
    private var isTimerRunning = false
    
    // Get arguments from bundle
    private var email: String? = null
    private var code: String? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            email = it.getString("email")
            code = it.getString("code")
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_verification, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        digit1 = view.findViewById(R.id.digit1)
        digit2 = view.findViewById(R.id.digit2)
        digit3 = view.findViewById(R.id.digit3)
        digit4 = view.findViewById(R.id.digit4)
        digit5 = view.findViewById(R.id.digit5)
        digit6 = view.findViewById(R.id.digit6)
        verifyButton = view.findViewById(R.id.verify_button)
        resendCodeLink = view.findViewById(R.id.resend_code_link)
        timerText = view.findViewById(R.id.timer_text)
        progressIndicator = view.findViewById(R.id.progressIndicator)
        
        // Set up the automatic focus change for verification code input
        setupVerificationCodeInput()
        
        // Start countdown timer for resend code
        startResendTimer()
        
        // Set up click listeners
        verifyButton.setOnClickListener {
            verifyCode()
        }
        
        resendCodeLink.setOnClickListener {
            if (!isTimerRunning) {
                resendVerificationCode()
            }
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
                        "Email verified successfully!",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Navigate to login screen
                    findNavController().navigate(
                        R.id.action_verificationFragment_to_loginFragment,
                        null,
                        androidx.navigation.NavOptions.Builder()
                            .setPopUpTo(R.id.registrationFragment, true)
                            .build()
                    )
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
        
        // Observe resend code result
        authViewModel.resendCodeResult.observe(viewLifecycleOwner) { result ->
            progressIndicator.visibility = View.GONE
            
            result.fold(
                onSuccess = { code ->
                    // Show success message
                    Toast.makeText(
                        requireContext(),
                        "Verification code resent to your email",
                        Toast.LENGTH_LONG
                    ).show()
                    
                    // Restart the timer
                    startResendTimer()
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
        
        // For the last digit, trigger verification if all digits are entered
        digit6.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s?.length == 1) {
                    // Check if all digits are filled
                    if (digit1.text.isNotEmpty() && 
                        digit2.text.isNotEmpty() && 
                        digit3.text.isNotEmpty() && 
                        digit4.text.isNotEmpty() && 
                        digit5.text.isNotEmpty() && 
                        digit6.text.isNotEmpty()) {
                        verifyCode()
                    }
                }
            }
            
            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun verifyCode() {
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
        
        // Attempt to verify
        authViewModel.verifyEmail(verificationCode)
    }
    
    private fun resendVerificationCode() {
        progressIndicator.visibility = View.VISIBLE
        
        // Attempt to resend verification code
        authViewModel.resendVerificationCode()
    }
    
    private fun startResendTimer() {
        // Disable resend link during countdown
        resendCodeLink.isEnabled = false
        resendCodeLink.alpha = 0.5f
        isTimerRunning = true
        
        // Set up countdown timer for 10 minutes
        countDownTimer = object : CountDownTimer(TimeUnit.MINUTES.toMillis(10), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) -
                        TimeUnit.MINUTES.toSeconds(minutes)
                
                timerText.text = String.format("Resend available in: %02d:%02d", minutes, seconds)
            }
            
            override fun onFinish() {
                timerText.text = "You can resend the code now"
                resendCodeLink.isEnabled = true
                resendCodeLink.alpha = 1.0f
                isTimerRunning = false
            }
        }
        
        countDownTimer.start()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        if (::countDownTimer.isInitialized) {
            countDownTimer.cancel()
        }
    }
    
    companion object {
        @JvmStatic
        fun newInstance(email: String, code: String) =
            VerificationFragment().apply {
                arguments = Bundle().apply {
                    putString("email", email)
                    putString("code", code)
                }
            }
    }
} 