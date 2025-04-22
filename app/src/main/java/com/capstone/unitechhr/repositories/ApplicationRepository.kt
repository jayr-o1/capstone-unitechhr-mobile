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
import java.util.UUID

class ApplicationRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val applicationRef = firestore.collection("applications")
    
    companion object {
        // API configuration options - modify these as needed
        private const val USE_EMULATOR = false  // Set to false for physical device via USB debugging
        private const val API_PORT = 8000      // The port your API is running on
        // Mock response has been completely removed
        
        // API endpoints for different environments
        private const val API_ENDPOINT_EMULATOR = "http://10.0.2.2:$API_PORT/analyze" // Special IP for emulator to host localhost
        private const val API_ENDPOINT_DEVICE = "http://127.0.0.1:$API_PORT/analyze" // For USB debugging with port forwarding
        private const val API_ENDPOINT_PRODUCTION = "https://jayr-o1--resume-scorer-api-fastapi-app.modal.run/analyze" // Deployed API endpoint
        
        // Debug flag - set to true for more detailed logging
        private const val DEBUG = true
    }
    
    /**
     * Get the appropriate API endpoint based on current configuration
     */
    private fun getApiEndpoint(isEmulator: Boolean = USE_EMULATOR): String {
        // Always use the production endpoint
        val endpoint = API_ENDPOINT_PRODUCTION
        
        Log.d("ApplicationRepository", "Using API endpoint: $endpoint")
        return endpoint
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
                .build()
            
            // Get appropriate API endpoint
            val apiUrl = getApiEndpoint()
            
            // Log API call details if in debug mode
            if (DEBUG) {
                Log.d("ApplicationRepository", "Making API request to: $apiUrl")
                Log.d("ApplicationRepository", "Request has resume file: ${tempFile.exists()}, size: ${tempFile.length()} bytes")
                Log.d("ApplicationRepository", "Form data parameters:")
                Log.d("ApplicationRepository", "- job_summary: ${jobSummary.take(50)}...")
                Log.d("ApplicationRepository", "- key_duties: ${keyDuties.take(50)}...")
                Log.d("ApplicationRepository", "- essential_skills: ${essentialSkills.take(50)}...")
                Log.d("ApplicationRepository", "- qualifications: ${qualifications.take(50)}...")
            }
            
            // Build the request
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/json")
                .post(requestBody)
                .build()
            
            // Execute the request
            try {
                Log.d("ApplicationRepository", "Attempting to execute API request to: $apiUrl")
                Log.d("ApplicationRepository", "Request headers: Accept=application/json")
                
                client.newCall(request).execute().use { response ->
                    // Log the response code and message
                    Log.d("ApplicationRepository", "API response code: ${response.code}")
                    
                    if (!response.isSuccessful) {
                        // Try to get error message from body
                        val errorResponseBody = response.body?.string()
                        val errorMsg = if (!errorResponseBody.isNullOrEmpty()) {
                            "API request failed with code: ${response.code}. Message: ${response.message}. Response: $errorResponseBody"
                        } else {
                            "API request failed with code: ${response.code}. Message: ${response.message}"
                        }
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
                        // Log the response for debugging
                        Log.d("ApplicationRepository", "Attempting to parse JSON response")
                        
                        // Extract critical values directly from JSON before attempting full parsing
                        val jsonObject = org.json.JSONObject(responseBody)
                        
                        // Get the match percentage directly - CRITICAL
                        val matchPercentage = jsonObject.optString("match_percentage", "0%")
                        Log.d("ApplicationRepository", "Directly extracted match_percentage: $matchPercentage")
                        
                        // Try full parsing
                        val analysis: ApplicationAnalysis
                        try {
                            // Configure Gson for more lenient parsing
                            analysis = gson.fromJson(responseBody, ApplicationAnalysis::class.java)
                            
                            // Ensure match percentage is set correctly
                            if (analysis.matchPercentage.isNullOrEmpty()) {
                                analysis.matchPercentage = matchPercentage
                            }
                            
                            // Fix skills match if needed
                            if (analysis.skillsMatch.matchedSkills.isEmpty() && analysis.skillsMatch.missingSkills.isEmpty()) {
                                // Try to manually extract skills
                                try {
                                    if (jsonObject.has("skills_match")) {
                                        val skillsJson = jsonObject.getJSONObject("skills_match")
                                        
                                        // Extract matched skills
                                        if (skillsJson.has("matched_skills")) {
                                            val matchedSkillsArray = skillsJson.getJSONArray("matched_skills")
                                            val matchedSkills = mutableListOf<String>()
                                            for (i in 0 until matchedSkillsArray.length()) {
                                                matchedSkills.add(matchedSkillsArray.getString(i))
                                            }
                                            analysis.skillsMatch.matchedSkills = matchedSkills
                                        }
                                        
                                        // Extract missing skills
                                        if (skillsJson.has("missing_skills")) {
                                            val missingSkillsArray = skillsJson.getJSONArray("missing_skills")
                                            val missingSkills = mutableListOf<String>()
                                            for (i in 0 until missingSkillsArray.length()) {
                                                missingSkills.add(missingSkillsArray.getString(i))
                                            }
                                            analysis.skillsMatch.missingSkills = missingSkills
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("ApplicationRepository", "Error manually extracting skills: ${e.message}")
                                }
                            }
                            
                            // Fix education if needed
                            if (analysis.education.applicantEducation.isEmpty() && jsonObject.has("education")) {
                                try {
                                    val eduJson = jsonObject.getJSONObject("education")
                                    if (eduJson.has("applicant_education")) {
                                        analysis.education.applicantEducation = eduJson.getString("applicant_education")
                                    }
                                    if (eduJson.has("required_education")) {
                                        analysis.education.requirement = eduJson.getString("required_education")
                                    }
                                    if (eduJson.has("assessment")) {
                                        analysis.education.assessment = eduJson.getString("assessment")
                                    }
                                } catch (e: Exception) {
                                    Log.e("ApplicationRepository", "Error fixing education data: ${e.message}")
                                }
                            }
                            
                            // Fix experience if needed
                            if (analysis.experience.applicantYears.isEmpty() && jsonObject.has("experience")) {
                                try {
                                    val expJson = jsonObject.getJSONObject("experience")
                                    if (expJson.has("applicant_years")) {
                                        analysis.experience.applicantYears = expJson.getString("applicant_years")
                                    }
                                    if (expJson.has("required_years")) {
                                        analysis.experience.requiredYears = expJson.getString("required_years")
                                    }
                                } catch (e: Exception) {
                                    Log.e("ApplicationRepository", "Error fixing experience data: ${e.message}")
                                }
                            }
                            
                            // Fix salary if needed
                            if ((analysis.salaryEstimate?.min ?: 0) == 0 && jsonObject.has("salary_estimate")) {
                                try {
                                    val salaryJson = jsonObject.getJSONObject("salary_estimate")
                                    if (analysis.salaryEstimate == null) {
                                        analysis.salaryEstimate = SalaryEstimate()
                                    }
                                    if (salaryJson.has("min")) {
                                        analysis.salaryEstimate?.min = salaryJson.optInt("min", 0)
                                    }
                                    if (salaryJson.has("max")) {
                                        analysis.salaryEstimate?.max = salaryJson.optInt("max", 0)
                                    }
                                    if (salaryJson.has("currency")) {
                                        analysis.salaryEstimate?.currency = salaryJson.optString("currency", "USD")
                                    }
                                } catch (e: Exception) {
                                    Log.e("ApplicationRepository", "Error fixing salary data: ${e.message}")
                                }
                            }
                            
                            // Log key fields to help with debugging
                            Log.d("ApplicationRepository", "Parsed analysis object:")
                            Log.d("ApplicationRepository", "- matchPercentage: ${analysis.matchPercentage}")
                            Log.d("ApplicationRepository", "- recommendation: ${analysis.recommendation}")
                            Log.d("ApplicationRepository", "- skillsMatch: matched=${analysis.skillsMatch.matchedSkills.size}, missing=${analysis.skillsMatch.missingSkills.size}")
                            Log.d("ApplicationRepository", "- experience: applicantYears=${analysis.experience.applicantYears}, requiredYears=${analysis.experience.requiredYears}")
                            Log.d("ApplicationRepository", "- education: applicantEdu=${analysis.education.applicantEducation}, requirement=${analysis.education.requirement}")
                            Log.d("ApplicationRepository", "- improvementSuggestions: ${analysis.improvementSuggestions != null}")
                            Log.d("ApplicationRepository", "- salaryEstimate: min=${analysis.salaryEstimate?.min}, max=${analysis.salaryEstimate?.max}")
                            
                        } catch (e: Exception) {
                            Log.e("ApplicationRepository", "JSON parsing exception: ${e.message}", e)
                            
                            // Try to create a minimal valid object from the response
                            val minimalAnalysis = createMinimalAnalysisFromJson(responseBody, userId, jobId, jobTitle, resumeUrl)
                            if (minimalAnalysis != null) {
                                Log.d("ApplicationRepository", "Created minimal analysis object from JSON")
                                return@withContext minimalAnalysis
                            } else {
                                throw IOException("Could not parse API response: ${e.message}")
                            }
                        }
                        
                        // Check for null fields that should not be null
                        if (analysis == null) {
                            throw IOException("API returned null analysis object")
                        }
                        
                        // Create a complete analysis object with additional data
                        val completeAnalysis = analysis.copy(
                            id = UUID.randomUUID().toString(),
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
            Log.e("ApplicationRepository", "Full exception stack trace:", e)
            
            // Print more detailed connection information for debugging
            Log.d("ApplicationRepository", "Connection details:" +
                "\nAPI URL: ${getApiEndpoint()}" +
                "\nIs API available: ${isApiServerRunning()}" +
                "\nInternet Connection: ${isInternetAvailable()}")
            
            // Create a minimal analysis object with error information
            val errorAnalysis = ApplicationAnalysis(
                id = UUID.randomUUID().toString(),
                userId = userId,
                jobId = jobId,
                jobTitle = jobTitle,
                resumeUrl = resumeUrl,
                analysisDate = Date(),
                recommendation = "Error connecting to analysis service: ${e.message}. Please check your internet connection and try again."
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
            // Convert email to consistent collection ID format
            val applicantId = analysis.userId.replace("@", "-").replace(".", "-")
            
            // Save analysis as a subcollection under the applicant's document
            firestore.collection("applicants")
                .document(applicantId)
                .collection("analysis")
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
    suspend fun downloadFileFromUrl(context: Context, fileUrl: String): File? = withContext(Dispatchers.IO) {
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
     * Simple test function to check if we can connect to the API server
     * This can be called separately to troubleshoot connectivity issues
     */
    suspend fun testApiConnection(): String = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()
            
            val apiUrl = getApiEndpoint()
            
            // Try to connect to the base URL without the /analyze path
            val baseUrl = apiUrl.substringBeforeLast("/")
            Log.d("ApplicationRepository", "Testing connection to remote API: $baseUrl")
            
            val request = Request.Builder()
                .url(baseUrl)
                .header("Accept", "application/json")
                .get() // Simple GET request
                .build()
            
            try {    
                client.newCall(request).execute().use { response ->
                    val message = if (response.isSuccessful) {
                        "Successfully connected to remote API server: ${response.code}"
                    } else {
                        "Connected but received error code: ${response.code}, message: ${response.message}"
                    }
                    
                    Log.d("ApplicationRepository", message)
                    return@withContext "API Connection Test Result:\n" +
                        "- Remote API Endpoint: $apiUrl\n" +
                        "- Result: $message\n" +
                        "- Internet Connection: " + isInternetAvailable()
                }
            } catch (e: Exception) {
                val errorDetails = "Failed to connect to remote API server: ${e.message}"
                Log.e("ApplicationRepository", errorDetails, e)
                
                return@withContext "API Connection Test Result:\n" +
                    "- Remote API Endpoint: $apiUrl\n" +
                    "- Error: ${e.message}\n" +
                    "- Internet Connection: " + isInternetAvailable() + "\n" +
                    "- Next steps: Make sure you have a working internet connection and the API server is online."
            }
        } catch (e: Exception) {
            val errorMessage = "Connection test setup failed: ${e.message}"
            Log.e("ApplicationRepository", errorMessage, e)
            return@withContext errorMessage
        }
    }

    /**
     * Simple internet connectivity check
     */
    private fun isInternetAvailable(): String {
        return try {
            val process = Runtime.getRuntime().exec("ping -c 1 google.com")
            val returnVal = process.waitFor()
            if (returnVal == 0) "Available" else "Limited or unavailable"
        } catch (e: Exception) {
            "Check failed: ${e.message}"
        }
    }

    /**
     * Provides information about API connection
     * @return Information about API connection
     */
    fun getApiConnectionInfo(): String {
        return """
            The application is configured to use the remote API endpoint:
            
            - API endpoint being used: ${getApiEndpoint()}
            
            No port forwarding is needed as we're using a remote API server.
            If you experience connection issues, please check your internet connection.
        """.trimIndent()
    }

    /**
     * Check if the API server is running and accessible
     */
    private suspend fun isApiServerRunning(): Boolean = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .build()
            
            val apiUrl = getApiEndpoint()
            val baseUrl = apiUrl.substringBeforeLast("/")
            
            // Use GET request instead of HEAD, as the server might not support HEAD requests
            val request = Request.Builder()
                .url(baseUrl)
                .get()
                .build()
                
            client.newCall(request).execute().use { response ->
                // Even if we get a 404, consider the API server running if we get any response
                return@withContext response.code < 500
            }
        } catch (e: Exception) {
            Log.d("ApplicationRepository", "API Server check failed: ${e.message}")
            return@withContext false
        }
    }

    /**
     * Create a minimal ApplicationAnalysis object from a JSON response
     * This is used when the standard parsing fails
     */
    private suspend fun createMinimalAnalysisFromJson(
        jsonResponse: String,
        userId: String,
        jobId: String,
        jobTitle: String,
        resumeUrl: String
    ): ApplicationAnalysis? = withContext(Dispatchers.IO) {
        try {
            // Use a more tolerant JSON parser approach
            val analysisId = UUID.randomUUID().toString()
            val jsonObject = org.json.JSONObject(jsonResponse)
            
            // Extract the recommendation if available
            val recommendation = try {
                jsonObject.optString("recommendation", "No recommendation available")
            } catch (e: Exception) {
                "No recommendation available"
            }
            
            // Extract match percentage if available
            val matchPercentage = try {
                jsonObject.optString("match_percentage", "0")
            } catch (e: Exception) {
                "0"
            }
            
            // Try to extract skills information
            val skillsMatch = SkillsMatch()
            try {
                if (jsonObject.has("skills_match")) {
                    val skillsJson = jsonObject.getJSONObject("skills_match")
                    
                    // Extract matched skills
                    if (skillsJson.has("matched_skills")) {
                        val matchedSkillsArray = skillsJson.getJSONArray("matched_skills")
                        val matchedSkills = mutableListOf<String>()
                        for (i in 0 until matchedSkillsArray.length()) {
                            matchedSkills.add(matchedSkillsArray.getString(i))
                        }
                        skillsMatch.matchedSkills = matchedSkills
                    }
                    
                    // Extract missing skills
                    if (skillsJson.has("missing_skills")) {
                        val missingSkillsArray = skillsJson.getJSONArray("missing_skills")
                        val missingSkills = mutableListOf<String>()
                        for (i in 0 until missingSkillsArray.length()) {
                            missingSkills.add(missingSkillsArray.getString(i))
                        }
                        skillsMatch.missingSkills = missingSkills
                    }
                    
                    // Extract match ratio
                    if (skillsJson.has("match_ratio")) {
                        skillsMatch.matchRatio = skillsJson.optString("match_ratio", "0")
                    }
                }
            } catch (e: Exception) {
                Log.e("ApplicationRepository", "Error parsing skills: ${e.message}")
            }
            
            // Log the successful parsing
            Log.d("ApplicationRepository", "Successfully created minimal analysis from JSON")
            
            // Create a basic analysis object with the extracted information
            val analysis = ApplicationAnalysis(
                id = analysisId,
                userId = userId,
                jobId = jobId,
                jobTitle = jobTitle,
                resumeUrl = resumeUrl,
                analysisDate = Date(),
                recommendation = recommendation,
                matchPercentage = matchPercentage,
                skillsMatch = skillsMatch,
                benchmark = null,
                confidenceScores = null
            )
            
            // Save this minimal analysis to Firestore
            saveAnalysisToFirestore(analysis)
            
            return@withContext analysis
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error creating minimal analysis: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Get analysis data for a specific applicant
     * @param applicantId ID of the applicant
     * @return List of analysis objects for the applicant
     */
    suspend fun getAnalysisForApplicant(applicantId: String): List<ApplicationAnalysis> = withContext(Dispatchers.IO) {
        try {
            val snapshot = firestore.collection("applicants")
                .document(applicantId)
                .collection("analysis")
                .orderBy("analysisDate", Query.Direction.DESCENDING)
                .get()
                .await()
                
            return@withContext snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(ApplicationAnalysis::class.java)?.copy(id = doc.id)
                } catch (e: Exception) {
                    Log.e("ApplicationRepository", "Error converting analysis document: ${e.message}")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error fetching analysis for applicant: ${e.message}")
            return@withContext emptyList()
        }
    }
    
    /**
     * Get a specific analysis for an applicant
     * @param applicantId ID of the applicant
     * @param analysisId ID of the analysis
     * @return The analysis object, or null if not found
     */
    suspend fun getAnalysis(applicantId: String, analysisId: String): ApplicationAnalysis? = withContext(Dispatchers.IO) {
        try {
            val doc = firestore.collection("applicants")
                .document(applicantId)
                .collection("analysis")
                .document(analysisId)
                .get()
                .await()
                
            return@withContext doc.toObject(ApplicationAnalysis::class.java)?.copy(id = doc.id)
        } catch (e: Exception) {
            Log.e("ApplicationRepository", "Error fetching analysis: ${e.message}")
            return@withContext null
        }
    }
} 