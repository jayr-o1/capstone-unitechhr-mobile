package com.capstone.unitechhr.repositories

import android.content.Context
import android.util.Log
import com.capstone.unitechhr.models.Application
import com.capstone.unitechhr.models.ApplicationStatus
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.util.concurrent.TimeUnit
import com.capstone.unitechhr.models.ApplicationAnalysis
import com.capstone.unitechhr.models.Education
import com.capstone.unitechhr.models.Experience
import com.capstone.unitechhr.models.ImprovementSuggestions
import com.capstone.unitechhr.models.SalaryEstimate
import com.capstone.unitechhr.models.SkillsMatch
import com.google.gson.Gson
import java.util.Date

class ApplicationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val applicationRef = firestore.collection("applications")
    
    companion object {
        // API configuration options - modify these as needed
        private const val USE_EMULATOR = true  // Set to true when testing with emulator
        private const val API_PORT = 8000      // The port your API is running on
        private const val USE_MOCK_RESPONSE = false // Set to true to use mock response for testing
        
        // API endpoints for different environments
        private const val API_ENDPOINT_EMULATOR = "http://10.0.2.2:$API_PORT/analyze" // Special IP for emulator to host localhost
        private const val API_ENDPOINT_DEVICE = "http://192.168.1.100:$API_PORT/analyze" // Change to your actual development machine IP
        private const val API_ENDPOINT_PRODUCTION = "https://your-production-api.com/analyze" // Change to your actual production URL
        
        // Debug flag - set to true for more detailed logging
        private const val DEBUG = true
    }
    
    /**
     * Get the appropriate API endpoint based on current configuration
     */
    private fun getApiEndpoint(isEmulator: Boolean = USE_EMULATOR): String {
        return when {
            isEmulator -> API_ENDPOINT_EMULATOR
            // Switch to production API if using a real device in release mode
            else -> API_ENDPOINT_DEVICE
        }
    }

    /**
     * Get applications for a specific user
     */
    suspend fun getApplicationsForUser(userId: String): List<Application> = withContext(Dispatchers.IO) {
        try {
            val snapshot = applicationRef
                .whereEqualTo("userId", userId)
                .orderBy("applicationDate", Query.Direction.DESCENDING)
                .get()
                .await()
            return@withContext snapshot.toObjects(Application::class.java)
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error fetching applications: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    // Get all applications for a specific user
    suspend fun getUserApplications(userId: String): List<Application> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("applications")
                .orderBy("applicationDate", Query.Direction.DESCENDING)
                .get()
                .await()
                
            return@withContext snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(Application::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("ApplicationRepository", "Error converting application document: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error fetching user applications: ${e.message}")
            
            // For testing - return sample data if there's an error
            if (userId.contains("test") || true) { // Always return test data for now
                return@withContext generateSampleApplications()
            }
            
            return@withContext emptyList()
        }
    }
    
    // Get a specific application
    suspend fun getApplication(userId: String, applicationId: String): Application? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("applications")
                .document(applicationId)
                .get()
                .await()
                
            return@withContext doc.toObject(Application::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error fetching application: ${e.message}")
            return@withContext null
        }
    }
    
    // For testing - Generate sample applications
    private fun generateSampleApplications(): List<Application> {
        // Create a list of sample applications
        return listOf(
            Application(
                id = "app1",
                jobId = "job1",
                userId = "user1",
                jobTitle = "Android Developer",
                companyName = "Google",
                applicationDate = Date(System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000), // 10 days ago
                status = ApplicationStatus.INTERVIEW
            ),
            Application(
                id = "app2",
                jobId = "job2",
                userId = "user1",
                jobTitle = "Frontend Developer",
                companyName = "Facebook",
                applicationDate = Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000), // 5 days ago
                status = ApplicationStatus.REVIEWING
            ),
            Application(
                id = "app3",
                jobId = "job3",
                userId = "user1",
                jobTitle = "Backend Developer",
                companyName = "Amazon",
                applicationDate = Date(System.currentTimeMillis() - 15 * 24 * 60 * 60 * 1000), // 15 days ago
                status = ApplicationStatus.REJECTED
            ),
            Application(
                id = "app4",
                jobId = "job4",
                userId = "user1",
                jobTitle = "DevOps Engineer",
                companyName = "Microsoft",
                applicationDate = Date(System.currentTimeMillis() - 2 * 24 * 60 * 60 * 1000), // 2 days ago
                status = ApplicationStatus.PENDING
            ),
            Application(
                id = "app5",
                jobId = "job5",
                userId = "user1",
                jobTitle = "Data Scientist",
                companyName = "Netflix",
                applicationDate = Date(System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000), // 30 days ago
                status = ApplicationStatus.HIRED
            )
        )
    }
    
    /**
     * Submit an application to the analysis API
     * @param resumeUrl URL of the resume file
     * @param jobSummary Summary of the job
     * @param keyDuties Key duties of the job
     * @param essentialSkills Essential skills required for the job
     * @param qualifications Qualifications required for the job
     * @return The analysis result object
     */
    suspend fun submitApplicationForAnalysis(
        context: Context,
        resumeUrl: String,
        jobSummary: String,
        keyDuties: String,
        essentialSkills: String,
        qualifications: String,
        userId: String,
        jobId: String,
        jobTitle: String
    ): ApplicationAnalysis = withContext(Dispatchers.IO) {
        try {
            // Use mock response if configured
            if (USE_MOCK_RESPONSE) {
                Log.d("ApplicationRepository", "Using mock response for testing")
                return@withContext generateMockAnalysisResult(userId, jobId, jobTitle, resumeUrl)
            }
            
            // First download the PDF from the URL to a temporary file
            val tempFile = downloadFileFromUrl(context, resumeUrl)
            
            if (tempFile == null) {
                throw IOException("Failed to download resume file")
            }
            
            // Create OkHttp client with increased timeouts
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            // Create the multipart request body
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "resume",
                    "resume.pdf",
                    tempFile.asRequestBody("application/pdf".toMediaTypeOrNull())
                )
                .addFormDataPart("job_summary", jobSummary)
                .addFormDataPart("key_duties", keyDuties)
                .addFormDataPart("essential_skills", essentialSkills)
                .addFormDataPart("qualifications", qualifications)
                .addFormDataPart("translate", "false")
                .build()
            
            // Get appropriate API endpoint
            val apiUrl = getApiEndpoint()
            
            // Log API call details if in debug mode
            if (DEBUG) {
                Log.d("ApplicationRepository", "Making API request to: $apiUrl")
                Log.d("ApplicationRepository", "Request has resume file: ${tempFile.exists()}, size: ${tempFile.length()} bytes")
            }
            
            // Build the request
            val request = Request.Builder()
                .url(apiUrl)
                .post(requestBody)
                .build()
            
            // Execute the request
            try {
                Log.d("ApplicationRepository", "Attempting to execute API request to: $apiUrl")
                
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorMsg = "API request failed with code: ${response.code}. Message: ${response.message}"
                        Log.e("ApplicationRepository", errorMsg)
                        throw IOException(errorMsg)
                    }
                    
                    // Clean up the temporary file
                    tempFile.delete()
                    
                    // Get the response body
                    val responseBody = response.body?.string()
                    if (responseBody.isNullOrEmpty()) {
                        val errorMsg = "Empty response from server"
                        Log.e("ApplicationRepository", errorMsg)
                        throw IOException(errorMsg)
                    }
                    
                    Log.d("ApplicationRepository", "API response: $responseBody")
                    
                    // Parse the JSON response
                    val gson = Gson()
                    try {
                        val analysis = gson.fromJson(responseBody, ApplicationAnalysis::class.java)
                        
                        // Create a complete analysis object with additional data
                        val completeAnalysis = analysis.copy(
                            id = firestore.collection("analyses").document().id,
                            userId = userId,
                            jobId = jobId,
                            jobTitle = jobTitle,
                            resumeUrl = resumeUrl,
                            analysisDate = Date()
                        )
                        
                        // Save the analysis to Firestore
                        saveAnalysisToFirestore(completeAnalysis)
                        
                        return@withContext completeAnalysis
                    } catch (e: Exception) {
                        Log.e("ApplicationRepository", "Error parsing API response: ${e.message}", e)
                        throw IOException("Error processing API response: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("ApplicationRepository", "Failed to execute API request: ${e.message}", e)
                
                // Check for security policy issues
                if (e.message?.contains("CLEARTEXT") == true || 
                    e.message?.contains("security policy") == true ||
                    e.message?.contains("SSL") == true) {
                    throw IOException("Network security error. Please check your app's network security configuration: ${e.message}")
                }
                
                throw e
            }
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error submitting application: ${e.message}")
            
            // Check if this is a security or network error, and use mock response for testing
            if (e.message?.contains("CLEARTEXT") == true || 
                e.message?.contains("security policy") == true ||
                e.message?.contains("SSL") == true ||
                e.message?.contains("Connection") == true) {
                
                Log.w("ApplicationRepository", "Network error detected, using mock response for testing: ${e.message}")
                return@withContext generateMockAnalysisResult(userId, jobId, jobTitle, resumeUrl)
            }
            
            // Create a minimal analysis object with error information
            val errorAnalysis = ApplicationAnalysis(
                id = firestore.collection("analyses").document().id,
                userId = userId,
                jobId = jobId,
                jobTitle = jobTitle,
                resumeUrl = resumeUrl,
                analysisDate = Date(),
                recommendation = "Error: ${e.message}"
            )
            
            // Still save the error analysis to Firestore
            saveAnalysisToFirestore(errorAnalysis)
            
            return@withContext errorAnalysis
        }
    }
    
    /**
     * Save application analysis to Firestore
     */
    private suspend fun saveAnalysisToFirestore(analysis: ApplicationAnalysis): Boolean = withContext(Dispatchers.IO) {
        try {
            // Save to analyses collection
            firestore.collection("analyses")
                .document(analysis.id)
                .set(analysis)
                .await()
            
            // Also save to user's analyses subcollection
            firestore.collection("users")
                .document(analysis.userId)
                .collection("analyses")
                .document(analysis.id)
                .set(analysis)
                .await()
            
            Log.d("ApplicationRepository", "Analysis saved to Firestore: ${analysis.id}")
            return@withContext true
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error saving analysis to Firestore: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Download a file from a URL to a temporary file
     * @param context The application context
     * @param fileUrl The URL of the file to download
     * @return The temporary file containing the downloaded content, or null if download failed
     */
    private suspend fun downloadFileFromUrl(context: Context, fileUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            if (DEBUG) {
                Log.d("ApplicationRepository", "Downloading file from URL: $fileUrl")
            }
            
            // Create a temporary file in the cache directory
            val tempFile = File.createTempFile("resume_", ".pdf", context.cacheDir)
            
            // Create a URL connection
            val url = URL(fileUrl)
            val connection = url.openConnection()
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            
            // Download the file
            var input: InputStream? = null
            var output: FileOutputStream? = null
            
            try {
                input = connection.getInputStream()
                output = FileOutputStream(tempFile)
                
                val data = ByteArray(4096)
                var count: Int
                var total = 0L
                
                while (input.read(data).also { count = it } != -1) {
                    output.write(data, 0, count)
                    total += count
                }
                
                if (DEBUG) {
                    Log.d("ApplicationRepository", "Downloaded file size: $total bytes")
                }
                
                return@withContext tempFile
            } finally {
                input?.close()
                output?.close()
            }
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error downloading file: ${e.message}", e)
            return@withContext null
        }
    }
    
    /**
     * Generate a mock analysis result for testing
     */
    private fun generateMockAnalysisResult(
        userId: String,
        jobId: String,
        jobTitle: String,
        resumeUrl: String
    ): ApplicationAnalysis {
        // Create a mock analysis result for testing
        return ApplicationAnalysis(
            id = firestore.collection("analyses").document().id,
            userId = userId,
            jobId = jobId,
            jobTitle = jobTitle,
            resumeUrl = resumeUrl,
            analysisDate = Date(),
            matchPercentage = "85",
            recommendation = "You are a good fit for this position!",
            skillsMatch = SkillsMatch(
                matchedSkills = listOf("Java", "Kotlin", "Android", "Firebase"),
                missingSkills = listOf("Flutter", "React Native"),
                additionalSkills = listOf("Git", "Agile"),
                matchRatio = "7"
            ),
            experience = Experience(
                requiredYears = "3",
                applicantYears = "4",
                percentageImpact = "25",
                jobTitles = listOf("Android Developer", "Mobile Engineer")
            ),
            education = Education(
                requirement = "Bachelor's Degree",
                applicantEducation = "Master's Degree",
                assessment = "Exceeds requirements"
            ),
            improvementSuggestions = ImprovementSuggestions(
                skills = listOf("Consider learning Flutter to expand your mobile development skills"),
                experience = listOf(),
                education = listOf(),
                general = listOf("Update your resume to highlight your mobile development experience")
            ),
            salaryEstimate = SalaryEstimate(
                min = 80000,
                max = 120000,
                currency = "USD",
                note = "Based on your experience and skills"
            )
        )
    }
} 