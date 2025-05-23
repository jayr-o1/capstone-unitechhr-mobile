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
import com.capstone.unitechhr.models.Job
import com.capstone.unitechhr.repositories.JobRepository
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Locale
import com.capstone.unitechhr.models.JobApplication
import com.capstone.unitechhr.models.JobApplicationStatus
import com.capstone.unitechhr.models.formatForDisplay

class ApplicationRepository {
    private val TAG = "ApplicationRepository"
    private val firestore: FirebaseFirestore
    private val applicationRef: CollectionReference
    private var jobRepositoryForTesting: JobRepository? = null
    
    // Primary constructor
    constructor() {
        firestore = FirebaseFirestore.getInstance()
        applicationRef = firestore.collection("applications")
    }
    
    // Secondary constructor for testing
    constructor(firestoreInstance: FirebaseFirestore) {
        firestore = firestoreInstance
        applicationRef = firestore.collection("applications")
    }
    
    // Method to set a mock JobRepository for testing
    fun setJobRepositoryForTesting(repository: JobRepository) {
        jobRepositoryForTesting = repository
    }
    
    // Helper method for testing the saveAnalysisToFirestore method
    internal suspend fun testSaveAnalysisToFirestore(analysis: ApplicationAnalysis): Boolean {
        return saveAnalysisToFirestore(analysis)
    }
    
    companion object {
        // API configuration options - modify these as needed
        private const val USE_EMULATOR = false  // Set to false for physical device via USB debugging
        private const val API_PORT = 8000      // The port your API is running on
        // Mock response has been completely removed
        
        // API endpoints for different environments
        private const val API_ENDPOINT_EMULATOR = "http://10.0.2.2:$API_PORT/analyze" // Special IP for emulator to host localhost
        private const val API_ENDPOINT_DEVICE = "http://127.0.0.1:$API_PORT/analyze" // For USB debugging with port forwarding
        private const val API_ENDPOINT_PRODUCTION = "https://jayr-o1--resume-scorer-fastapi-app.modal.run/analyze" // Deployed API endpoint
        
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
                status = ApplicationStatus.INTERVIEW_SCHEDULED
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
     * @param criteriaWeights Weights for education, skills, and experience criteria
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
        jobTitle: String,
        displayName: String? = null,
        criteriaWeights: Map<String, Int>? = null
    ): ApplicationAnalysis = withContext(Dispatchers.IO) {
        try {
            // First download the PDF from the URL to a temporary file
            val tempFile = downloadFileInternal(context, resumeUrl)
            
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
                
            // Add criteria weights if available
            if (criteriaWeights != null) {
                criteriaWeights["education"]?.let { 
                    requestBody.addFormDataPart("education_weight", it.toString()) 
                }
                criteriaWeights["skills"]?.let { 
                    requestBody.addFormDataPart("skills_weight", it.toString()) 
                }
                criteriaWeights["experience"]?.let { 
                    requestBody.addFormDataPart("experience_weight", it.toString()) 
                }
            }
            
            val finalRequestBody = requestBody.build()
            
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
                if (criteriaWeights != null) {
                    Log.d("ApplicationRepository", "- weights: education=${criteriaWeights["education"]}, skills=${criteriaWeights["skills"]}, experience=${criteriaWeights["experience"]}")
                }
            }
            
            // Build the request
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/json")
                .post(finalRequestBody)
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
                            val minimalAnalysis = createMinimalAnalysisFromJson(responseBody, userId, jobId, jobTitle, resumeUrl, displayName)
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
                            analysisDate = Date(),
                            displayName = displayName
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
                displayName = displayName,
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
            // Save to analyses collection
            firestore.collection("analyses")
                .document(analysis.id)
                .set(analysis)
                .await()
            
            // Check the match percentage and recommendation to determine appropriate status
            val matchPercentageValue = analysis.matchPercentage.replace("%", "").toDoubleOrNull() ?: 0.0
            val hasInterviewRecommendation = analysis.recommendation.contains("interview", ignoreCase = true) ||
                                            analysis.recommendation.contains("hire", ignoreCase = true)
            val hasHighMatchPercentage = matchPercentageValue >= 80.0
            
            Log.d(TAG, "Analysis check - recommendation: ${analysis.recommendation}, match: ${analysis.matchPercentage}")
            Log.d(TAG, "Interview recommendation: $hasInterviewRecommendation, high match: $hasHighMatchPercentage, match percentage: $matchPercentageValue")
            
            // MODIFIED: Always set initial application status to PENDING regardless of analysis results
            val newStatus = JobApplicationStatus.PENDING
            
            // Update the application status in Firestore only if jobId is not empty
            if (analysis.jobId.isNotEmpty()) {
                // Update the application status in Firestore
                updateApplicationStatus(analysis.userId, analysis.jobId, newStatus)
                
                // Copy applicant data to university's job applicants collection regardless of status
                // Pass the new status instead of always using SUBMITTED
                updateApplicationStatusAndCopyToUniversity(analysis, newStatus)
            } else {
                Log.e(TAG, "Cannot update application status: jobId is empty")
            }
            
            Log.d(TAG, "Successfully saved analysis: ${analysis.id} with status: $newStatus")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Error saving analysis: ${e.message}")
            return@withContext false
        }
    }
    
    /**
     * Updates application status to INTERVIEW and copies the applicant data to university's job subcollection
     */
    private suspend fun updateApplicationStatusAndCopyToUniversity(analysis: ApplicationAnalysis, newStatus: JobApplicationStatus) = withContext(Dispatchers.IO) {
        try {
            // Format the user ID properly - handle both normal IDs and email IDs
            val sanitizedUserId = analysis.userId.replace("@", "-").replace(".", "-")
            
            // First get the applicant data from the user id
            val userSnapshot = firestore.collection("users")
                .document(sanitizedUserId)
                .get()
                .await()
            
            if (!userSnapshot.exists()) {
                Log.e(TAG, "User not found: ${analysis.userId} (sanitized: $sanitizedUserId)")
                
                // Try alternative lookup methods if direct ID fails
                val altUserSnapshot = findUserByEmail(analysis.userId)
                if (altUserSnapshot == null) {
                    Log.d(TAG, "Creating minimal profile since user document not found")
                    
                    // Create a minimal profile from the analysis info
                    createMinimalProfile(analysis, newStatus)
                } else {
                    // Continue with the alternative user snapshot
                    processUserAndCopyToUniversity(analysis, altUserSnapshot, newStatus)
                }
            } else {
                // Continue with the found user
                processUserAndCopyToUniversity(analysis, userSnapshot, newStatus)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating application status and copying to university: ${e.message}")
        }
    }
    
    /**
     * Creates a minimal profile when user document is not found
     */
    private suspend fun createMinimalProfile(analysis: ApplicationAnalysis, newStatus: JobApplicationStatus) = withContext(Dispatchers.IO) {
        try {
            // Get job details first to get university ID
            val jobRepository = jobRepositoryForTesting ?: JobRepository()
            val job = jobRepository.getJobById(analysis.jobId)
            if (job == null) {
                Log.e(TAG, "Job not found for ID: ${analysis.jobId}")
                return@withContext
            }
            
            val universityId = job.universityId
            val jobId = analysis.jobId
            
            // Use displayName if available, otherwise format name from email
            val name = analysis.displayName ?: formatNameFromEmail(analysis.userId)
            
            // Get formatted status text - always use "Pending" status for new applications
            val statusText = "Pending"
            
            // Create profile data
            val applicantProfile = mapOf(
                "name" to name,
                "email" to analysis.userId,
                "dateApplied" to Date(),
                "resumeUrl" to analysis.resumeUrl,
                "status" to statusText,
                "matchPercentage" to analysis.matchPercentage,
                "userId" to analysis.userId,
                "skillsMatch" to (analysis.skillsMatch.matchedSkills.take(3).joinToString(", ") + " (${analysis.skillsMatch.matchedSkills.size}/${analysis.skillsMatch.matchedSkills.size + analysis.skillsMatch.missingSkills.size})"),
                "experience" to "Required: ${analysis.experience.requiredYears} | Applicant: ${analysis.experience.applicantYears}",
                "education" to "${analysis.education.applicantEducation} (${analysis.education.assessment ?: "Meets Requirement"})",
                "recommendation" to analysis.recommendation,
                "isMinimalProfile" to true
            )
            
            // Determine document ID to use
            val documentId = analysis.userId.replace("@", "-").replace(".", "-")
            
            // Log the path where we're saving
            val applicantPath = "universities/$universityId/jobs/$jobId/applicants/$documentId"
            Log.d(TAG, "Saving minimal applicant profile to: $applicantPath")
            
            // Add to the university's job applicants subcollection
            firestore.collection("universities")
                .document(universityId)
                .collection("jobs")
                .document(jobId)
                .collection("applicants")
                .document(documentId)
                .set(applicantProfile)
                .await()
            
            Log.d(TAG, "Successfully saved minimal applicant profile with status: $statusText")
            
            // Update the application status in the applications collection as well
            if (jobId.isNotEmpty()) {
                updateApplicationStatus(userId = analysis.userId, jobId = jobId, newStatus = newStatus)
            } else {
                Log.e(TAG, "Cannot update application status: jobId is empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating minimal profile: ${e.message}")
        }
    }
    
    /**
     * Find a user by their email address
     */
    private suspend fun findUserByEmail(email: String): DocumentSnapshot? = withContext(Dispatchers.IO) {
        try {
            // Try to find the user document by querying the email field
            val querySnapshot = firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .await()
            
            if (!querySnapshot.isEmpty) {
                return@withContext querySnapshot.documents.first()
            }
            
            // If that fails, log the error and return null
            Log.e(TAG, "Could not find user with email: $email")
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "Error finding user by email: ${e.message}")
            return@withContext null
        }
    }
    
    /**
     * Process the user document and copy the data to the university's job applicants collection
     */
    private suspend fun processUserAndCopyToUniversity(analysis: ApplicationAnalysis, userSnapshot: DocumentSnapshot, newStatus: JobApplicationStatus) = withContext(Dispatchers.IO) {
        try {
            // Get job details to find the university ID
            val jobRepository = jobRepositoryForTesting ?: JobRepository()
            val job = jobRepository.getJobById(analysis.jobId)
            if (job == null) {
                Log.e(TAG, "Job not found: ${analysis.jobId}")
                return@withContext
            }
            
            val universityId = job.universityId
            val jobId = analysis.jobId
            
            // Extract user data from snapshot
            val name = analysis.displayName ?: userSnapshot.getString("displayName") ?: userSnapshot.getString("name") ?: formatNameFromEmail(analysis.userId)
            
            // Always use "Pending" status text for new applications
            val statusText = "Pending"
            
            // Create applicant profile
            val applicantProfile = mapOf(
                "name" to name,
                "email" to analysis.userId,
                "dateApplied" to Date(),
                "resumeUrl" to analysis.resumeUrl,
                "status" to statusText,
                "matchPercentage" to analysis.matchPercentage,
                "userId" to analysis.userId,
                "skillsMatch" to (analysis.skillsMatch.matchedSkills.take(3).joinToString(", ") + " (${analysis.skillsMatch.matchedSkills.size}/${analysis.skillsMatch.matchedSkills.size + analysis.skillsMatch.missingSkills.size})"),
                "experience" to "Required: ${analysis.experience.requiredYears} | Applicant: ${analysis.experience.applicantYears}",
                "education" to "${analysis.education.applicantEducation} (${analysis.education.assessment ?: "Meets Requirement"})",
                "recommendation" to analysis.recommendation,
                "isMinimalProfile" to true
            )
            
            // Determine document ID to use
            val documentId = analysis.userId.replace("@", "-").replace(".", "-")
            
            // Log the path where we're saving
            val applicantPath = "universities/$universityId/jobs/$jobId/applicants/$documentId"
            Log.d(TAG, "Saving applicant profile to: $applicantPath")
            
            // Add to the university's job applicants subcollection
            firestore.collection("universities")
                .document(universityId)
                .collection("jobs")
                .document(jobId)
                .collection("applicants")
                .document(documentId)
                .set(applicantProfile)
                .await()
            
            Log.d(TAG, "Successfully saved applicant profile with status: $statusText")
            
            // Update the application status in the applications collection as well
            if (jobId.isNotEmpty()) {
                updateApplicationStatus(userId = analysis.userId, jobId = jobId, newStatus = newStatus)
            } else {
                Log.e(TAG, "Cannot update application status: jobId is empty")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing user and copying to university: ${e.message}")
        }
    }

    // Get all applications for a specific user/applicant
    suspend fun getJobApplicationsForUser(userId: String): List<JobApplication> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Fetching applications for user: $userId")
            
            // Convert email to a format suitable for Firestore path if needed
            val sanitizedUserId = userId.replace("@", "-").replace(".", "-")
            
            // Get all applications for this user from different universities/jobs
            val applications = mutableListOf<JobApplication>()
            
            // Track job IDs to prevent duplicates
            val processedJobIds = mutableSetOf<String>()
            
            // Query the universities collection to find all applications
            val universitiesSnapshot = firestore.collection("universities").get().await()
            Log.d(TAG, "Found ${universitiesSnapshot.documents.size} universities to check")
            
            for (universityDoc in universitiesSnapshot.documents) {
                val universityId = universityDoc.id
                val universityName = universityDoc.getString("name") ?: "Unknown University"
                
                Log.d(TAG, "Processing university: $universityId ($universityName)")
                
                // Query jobs under this university
                val jobsSnapshot = universityDoc.reference.collection("jobs").get().await()
                Log.d(TAG, "Found ${jobsSnapshot.documents.size} jobs in university $universityId")
                
                for (jobDoc in jobsSnapshot.documents) {
                    val jobId = jobDoc.id
                    
                    // Skip jobs we've already processed
                    if (processedJobIds.contains(jobId)) {
                        Log.d(TAG, "Skipping already processed job: $jobId")
                        continue
                    }
                    
                    Log.d(TAG, "Processing job: $jobId in university: $universityId")
                    
                    // Check if there's an applicant document for this user
                    // Try with the sanitized userId first
                    val applicantDoc = try {
                        jobDoc.reference
                            .collection("applicants")
                            .document(sanitizedUserId)
                            .get()
                            .await()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting applicant doc: ${e.message}")
                        continue
                    }
                    
                    if (applicantDoc.exists()) {
                        Log.d(TAG, "Found application for job $jobId with applicant ID: ${applicantDoc.id}")
                        
                        // Log the full document data to debug
                        Log.d(TAG, "Full document data: ${applicantDoc.data}")
                        
                        // Get job details
                        val jobTitle = jobDoc.getString("title") ?: "Unnamed Job"
                        val company = universityName
                        
                        // Get application status - log the raw value to debug
                        val rawStatus = applicantDoc.getString("status")
                        Log.d(TAG, "Raw status from Firestore: $rawStatus")
                        
                        // Convert to JobApplicationStatus enum for UI display
                        val status = try {
                            // First try to match directly by enum name (case insensitive)
                            JobApplicationStatus.values().firstOrNull { 
                                it.name.equals(rawStatus, ignoreCase = true) 
                            }
                            // If that fails, try the fromString helper
                            ?: JobApplicationStatus.fromString(rawStatus)
                        } catch (e: Exception) {
                            // If all else fails, use PENDING
                            Log.e(TAG, "Error parsing status, using PENDING: ${e.message}")
                            JobApplicationStatus.PENDING
                        }
                        Log.d(TAG, "Final status enum for display: $status")
                        
                        // Create application object
                        val appliedDate = applicantDoc.getTimestamp("dateApplied")?.toDate() ?: Date()
                        val lastUpdated = applicantDoc.getTimestamp("lastUpdated")?.toDate()
                        
                        val application = JobApplication(
                            id = applicantDoc.id,
                            jobId = jobId,
                            jobTitle = jobTitle,
                            company = company,
                            location = "", // Remove location as requested
                            status = status,
                            appliedDate = appliedDate,
                            lastUpdated = lastUpdated,
                            rawStatus = rawStatus // Store the raw status string from Firestore
                        )
                        
                        applications.add(application)
                        processedJobIds.add(jobId)
                    }
                }
            }
            
            Log.d(TAG, "Found ${applications.size} applications for user $userId")
            return@withContext applications
            
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching applications: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    /**
     * Download a file from a URL to a temporary file
     * Public version for use by fragments like PdfViewerFragment
     * @param context The application context
     * @param fileUrl The URL of the file to download
     * @return The temporary file containing the downloaded content, or null if download failed
     */
    suspend fun downloadFileFromUrl(context: Context, fileUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            if (DEBUG) {
                Log.d(TAG, "Downloading file from URL: $fileUrl")
            }
            
            // Create a temporary file in the cache directory
            val tempFile = File.createTempFile("file_", ".pdf", context.cacheDir)
            
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
                    Log.d(TAG, "Downloaded file size: $total bytes")
                }
                
                return@withContext tempFile
            } finally {
                input?.close()
                output?.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Download a file from a URL to a temporary file
     * Internal version used by the ApplicationRepository
     * @param context The application context
     * @param fileUrl The URL of the file to download
     * @return The temporary file containing the downloaded content, or null if download failed
     */
    private suspend fun downloadFileInternal(context: Context, fileUrl: String): File? = withContext(Dispatchers.IO) {
        try {
            if (DEBUG) {
                Log.d(TAG, "Downloading file from URL: $fileUrl")
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
                    Log.d(TAG, "Downloaded file size: $total bytes")
                }
                
                return@withContext tempFile
            } finally {
                input?.close()
                output?.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file: ${e.message}", e)
            return@withContext null
        }
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
            Log.d(TAG, "API Server check failed: ${e.message}")
            return@withContext false
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
     * Create a minimal ApplicationAnalysis object from a JSON response
     * This is used when the standard parsing fails
     */
    private suspend fun createMinimalAnalysisFromJson(
        jsonResponse: String,
        userId: String,
        jobId: String,
        jobTitle: String,
        resumeUrl: String,
        displayName: String? = null
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
                Log.e(TAG, "Error parsing skills: ${e.message}")
            }
            
            // Create basic experience and education objects
            val experience = Experience()
            val education = Education()
            
            try {
                if (jsonObject.has("experience")) {
                    val expJson = jsonObject.getJSONObject("experience")
                    experience.applicantYears = expJson.optString("applicant_years", "0")
                    experience.requiredYears = expJson.optString("required_years", "Not specified")
                }
                
                if (jsonObject.has("education")) {
                    val eduJson = jsonObject.getJSONObject("education")
                    education.applicantEducation = eduJson.optString("applicant_education", "Not specified")
                    education.requirement = eduJson.optString("required_education", "Not specified")
                    education.assessment = eduJson.optString("assessment", "Does not meet requirement")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing experience/education: ${e.message}")
            }
            
            // Log the successful parsing
            Log.d(TAG, "Successfully created minimal analysis from JSON")
            
            // Create a minimal analysis object
            val analysis = ApplicationAnalysis(
                id = analysisId,
                userId = userId,
                jobId = jobId,
                jobTitle = jobTitle,
                resumeUrl = resumeUrl,
                analysisDate = Date(),
                displayName = displayName,
                matchPercentage = matchPercentage,
                recommendation = recommendation,
                skillsMatch = skillsMatch,
                experience = experience,
                education = education,
                benchmark = null,
                confidenceScores = null
            )
            
            // Save this minimal analysis to Firestore
            saveAnalysisToFirestore(analysis)
            
            return@withContext analysis
        } catch (e: Exception) {
            Log.e(TAG, "Error creating minimal analysis: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Update the status of an application
     */
    private suspend fun updateApplicationStatus(userId: String, jobId: String, newStatus: JobApplicationStatus): Unit = withContext(Dispatchers.IO) {
        try {
            // Convert enum to status string - use title case format
            val statusText = newStatus.formatForDisplay()
            Log.d(TAG, "Updating application status to: $statusText from enum $newStatus")
            
            // Try with the provided user ID first
            var applicationFound = false
            
            // Find the application by user ID and job ID
            val snapshot = firestore.collection("applications")
                .whereEqualTo("userId", userId)
                .whereEqualTo("jobId", jobId)
                .get()
                .await()
            
            if (!snapshot.isEmpty) {
                applicationFound = true
                // Update the status of all matching applications (should be only one)
                for (doc in snapshot.documents) {
                    firestore.collection("applications")
                        .document(doc.id)
                        .update("status", statusText) // Use formatted string instead of enum.toString()
                        .await()
                    Log.d(TAG, "Updated application ${doc.id} status to $statusText")
                }
            } else {
                // If application doesn't exist, create a new one with PENDING status
                val applicationId = UUID.randomUUID().toString()
                val application = hashMapOf(
                    "id" to applicationId,
                    "userId" to userId,
                    "jobId" to jobId,
                    "status" to statusText,
                    "applicationDate" to Date(),
                    "lastUpdated" to Date()
                )
                
                // Save the new application
                firestore.collection("applications")
                    .document(applicationId)
                    .set(application)
                    .await()
                
                Log.d(TAG, "Created new application $applicationId with status $statusText")
                applicationFound = true
            }
            
            if (!applicationFound) {
                Log.w(TAG, "No application found or created for userId: $userId, jobId: $jobId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating application status: ${e.message}")
        }
    }

    /**
     * Get all jobs across all universities
     */
    private suspend fun getJobsAcrossAllUniversities(): List<Job> = withContext(Dispatchers.IO) {
        try {
            // If a test repository is set, use it instead of creating a new one
            val jobRepository = jobRepositoryForTesting ?: JobRepository()
            return@withContext jobRepository.getJobs()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting jobs: ${e.message}")
            return@withContext emptyList()
        }
    }

    /**
     * Test API connection and return detailed information about the connection status
     * This is used for debugging purposes, especially when connectivity issues arise
     */
    suspend fun testApiConnection(): String = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Testing API connection")
            val apiUrl = getApiEndpoint()
            val pingResult = isInternetAvailable()
            val apiServerRunning = isApiServerRunning()
            
            // Build a detailed response
            val connectionReport = StringBuilder()
            connectionReport.append("Internet connectivity: $pingResult\n\n")
            connectionReport.append("API server running: ${if (apiServerRunning) "Yes" else "No"}\n\n")
            connectionReport.append("API endpoint: $apiUrl\n\n")
            
            // Try a simple HEAD request to the API
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .build()
                    
                val request = Request.Builder()
                    .url(apiUrl)
                    .head()  // Use HEAD to avoid transferring a response body
                    .build()
                    
                client.newCall(request).execute().use { response ->
                    connectionReport.append("API response code: ${response.code}\n")
                    connectionReport.append("API response message: ${response.message}\n\n")
                    
                    if (response.isSuccessful) {
                        connectionReport.append("✅ API connection successful!\n")
                        connectionReport.append("Headers: \n")
                        response.headers.forEach { (name, value) ->
                            connectionReport.append("  $name: $value\n")
                        }
                    } else {
                        connectionReport.append("❌ API connection failed with HTTP ${response.code}.\n")
                        connectionReport.append("This may indicate the API is not accepting HEAD requests.\n")
                        connectionReport.append("Try an application submission to test full functionality.\n")
                    }
                }
            } catch (e: Exception) {
                connectionReport.append("❌ API connection failed with error: ${e.message}\n")
                connectionReport.append("Error type: ${e.javaClass.simpleName}\n\n")
                
                // More detailed diagnostics based on error type
                when {
                    e.message?.contains("CLEARTEXT") == true -> {
                        connectionReport.append("This error indicates a network security policy issue.\n")
                        connectionReport.append("The app is trying to make an unencrypted HTTP connection.\n")
                        connectionReport.append("Check your network security configuration file and make sure the API URL is using HTTPS or is explicitly allowed in cleartext-traffic.\n")
                    }
                    e.message?.contains("SSL") == true || e.message?.contains("Trust anchor") == true -> {
                        connectionReport.append("This error indicates an SSL certificate issue.\n")
                        connectionReport.append("The app cannot establish a trusted HTTPS connection to the server.\n")
                        connectionReport.append("Check that the server has a valid SSL certificate or add network security exceptions as needed.\n")
                    }
                    e.message?.contains("timeout") == true -> {
                        connectionReport.append("The request timed out.\n")
                        connectionReport.append("The server may be slow to respond or unreachable.\n")
                        connectionReport.append("Check your internet connection and the server status.\n")
                    }
                    e.message?.contains("resolve host") == true || e.message?.contains("Unknown host") == true -> {
                        connectionReport.append("DNS resolution failure.\n")
                        connectionReport.append("The app cannot resolve the hostname in the API URL.\n")
                        connectionReport.append("Check that the URL is correct and your device has DNS access.\n")
                    }
                }
            }
            
            return@withContext connectionReport.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Error in test API connection: ${e.message}", e)
            return@withContext "Error testing API connection: ${e.message}"
        }
    }

    /**
     * Format a name from an email address
     */
    private fun formatNameFromEmail(email: String): String {
        if (!email.contains("@")) {
            return "Applicant"  // Default name if not an email
        }
        
        val localPart = email.split("@").firstOrNull() ?: ""
        
        // Identify format patterns and transform accordingly
        return when {
            // Handle names with dots (first.last@example.com)
            localPart.contains(".") -> {
                localPart.split(".")
                    .joinToString(" ") { part ->
                        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
            }
            
            // Handle camelCase (firstLast@example.com)
            localPart.contains(Regex("[a-z][A-Z]")) -> {
                val nameWithSpaces = localPart.replace(Regex("([a-z])([A-Z])"), "$1 $2")
                nameWithSpaces.split(" ").joinToString(" ") { part ->
                    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
            }
            
            // Handle names with numbers (john123@example.com) - remove numbers
            localPart.contains(Regex("[a-zA-Z]+[0-9]+")) -> {
                val nameOnly = localPart.replace(Regex("[0-9]+"), "")
                nameOnly.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
            
            // Handle underscore separation (first_last@example.com)
            localPart.contains("_") -> {
                localPart.split("_")
                    .joinToString(" ") { part ->
                        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
            }
            
            // Handle hyphen separation (first-last@example.com)
            localPart.contains("-") -> {
                localPart.split("-")
                    .joinToString(" ") { part ->
                        part.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                    }
            }
            
            // Default case - just capitalize the first letter
            else -> {
                localPart.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }
    }
} 