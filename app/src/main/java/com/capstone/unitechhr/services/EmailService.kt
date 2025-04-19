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
        private const val EMAIL_PASSWORD = "zpoz gyqc zzyc yiql" // App password, not regular password
        
        /**
         * Test SMTP connection only
         */
        suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Testing SMTP connection...")
                
                val properties = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    put("mail.debug", "true")
                    put("mail.debug.auth", "true")
                }
                
                // Create session with authentication
                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD)
                    }
                })
                
                // Enable session debugging
                session.debug = true
                
                try {
                    Log.d(TAG, "Opening connection to SMTP server...")
                    val transport = session.getTransport("smtp")
                    transport.connect("smtp.gmail.com", EMAIL_USERNAME, EMAIL_PASSWORD)
                    val isConnected = transport.isConnected
                    Log.d(TAG, "SMTP Connection successful: $isConnected")
                    transport.close()
                    return@withContext Result.success("SMTP connection test successful")
                } catch (e: Exception) {
                    Log.e(TAG, "SMTP connection test failed: ${e.javaClass.simpleName}: ${e.message}")
                    e.printStackTrace()
                    return@withContext Result.failure(e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to test SMTP connection: ${e.javaClass.simpleName}", e)
                e.printStackTrace()
                return@withContext Result.failure(e)
            }
        }
        
        /**
         * Send a verification email with the provided code
         */
        suspend fun sendVerificationEmail(recipientEmail: String, code: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
            // First test the connection
            val connectionTest = testConnection()
            if (connectionTest.isFailure) {
                Log.e(TAG, "Aborting email send - connection test failed: ${connectionTest.exceptionOrNull()?.message}")
                return@withContext Result.failure(connectionTest.exceptionOrNull() ?: Exception("Connection test failed"))
            }
            
            try {
                Log.d(TAG, "Starting email sending process to $recipientEmail")
                
                val properties = Properties().apply {
                    put("mail.smtp.host", "smtp.gmail.com")
                    put("mail.smtp.port", "587")
                    put("mail.smtp.auth", "true")
                    put("mail.smtp.starttls.enable", "true")
                    // Add these to debug connections
                    put("mail.debug", "true")
                    put("mail.debug.auth", "true")
                }
                
                Log.d(TAG, "Creating mail session with properties")
                
                // Create session with authentication
                val session = Session.getInstance(properties, object : Authenticator() {
                    override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(EMAIL_USERNAME, EMAIL_PASSWORD)
                    }
                })
                
                // Enable session debugging
                session.debug = true
                
                Log.d(TAG, "Creating message")
                
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
                    
                    Log.d(TAG, "Message created successfully")
                }
                
                try {
                    // Send the message
                    Log.d(TAG, "Attempting to send verification email to $recipientEmail with code $code")
                    Transport.send(message)
                    Log.d(TAG, "Email sent successfully to $recipientEmail")
                } catch (e: Exception) {
                    Log.e(TAG, "Transport.send failed: ${e.javaClass.simpleName}: ${e.message}")
                    e.printStackTrace()
                    throw e
                }
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send verification email: ${e.javaClass.simpleName}", e)
                e.printStackTrace()
                Result.failure(e)
            }
        }
    }
} 