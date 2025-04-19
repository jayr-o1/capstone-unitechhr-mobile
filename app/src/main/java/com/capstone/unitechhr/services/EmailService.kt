package com.capstone.unitechhr.services

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmailService {
    companion object {
        private const val TAG = "EmailService"
        
        // Configure these with your email credentials
        private const val EMAIL_USERNAME = "olores.jayrm@gmail.com"
        private const val EMAIL_PASSWORD = "zpozgyqczzycyiql" // App password, not regular password
        
        /**
         * Send a verification email with the provided code
         */
        suspend fun sendVerificationEmail(recipientEmail: String, code: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
            try {
                val properties = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.socketFactory.port", "465")
                    put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.port", "465")
                }
                
                // Create session with authentication
                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD)
                    }
                })
                
                // Create message
                val message = MimeMessage(session).apply {
                    setFrom(InternetAddress(EMAIL_USERNAME, "UniTech HR"))
                    addRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                    subject = "Verify Your UniTech HR Account"
                    
                    // Create HTML content for the email
                    val currentYear = SimpleDateFormat("yyyy", Locale.getDefault()).format(Date())
                    val htmlContent = """
                        <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0; border-radius: 5px;">
                            <div style="text-align: center; margin-bottom: 20px;">
                                <h1 style="color: #4a6ee0;">UniTech HR</h1>
                            </div>
                            <h2 style="color: #4a6ee0;">Welcome to UniTech HR, $name!</h2>
                            <p>Thank you for registering. To complete your account setup, please verify your email address using the code below:</p>
                            <div style="background-color: #f5f5f5; padding: 15px; border-radius: 5px; text-align: center; margin: 20px 0;">
                                <h1 style="color: #4a6ee0; letter-spacing: 5px; font-size: 32px;">$code</h1>
                            </div>
                            <p>This code will expire in 10 minutes.</p>
                            <p>If you didn't create an account with UniTech HR, you can safely ignore this email.</p>
                            <div style="margin-top: 30px; text-align: center; color: #888; font-size: 12px;">
                                <p>&copy; $currentYear UniTech HR. All rights reserved.</p>
                            </div>
                        </div>
                    """.trimIndent()
                    
                    // Set HTML content
                    setContent(htmlContent, "text/html; charset=utf-8")
                }
                
                // Send the message
                Log.d(TAG, "Sending verification email to $recipientEmail with code $code")
                Transport.send(message)
                Log.d(TAG, "Email sent successfully to $recipientEmail")
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send verification email", e)
                Result.failure(e)
            }
        }
    }
} 