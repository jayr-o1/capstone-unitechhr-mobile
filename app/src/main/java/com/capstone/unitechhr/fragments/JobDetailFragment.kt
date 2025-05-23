package com.capstone.unitechhr.fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.ApplicationAnalysis
import com.capstone.unitechhr.models.CriteriaWeights
import com.capstone.unitechhr.models.Education as AnalysisEducation
import com.capstone.unitechhr.models.Experience
import com.capstone.unitechhr.models.SalaryEstimate
import com.capstone.unitechhr.models.SkillsMatch
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.capstone.unitechhr.viewmodels.JobViewModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale
import com.capstone.unitechhr.repositories.ApplicationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.capstone.unitechhr.models.JobApplicationStatus

class JobDetailFragment : Fragment() {

    private val TAG = "JobDetailFragment"
    private val viewModel: JobViewModel by activityViewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private val applicationRepository = ApplicationRepository()
    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private var jobInfoDialog: Dialog? = null
    private var applicationResultDialog: Dialog? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView called")
        return inflater.inflate(R.layout.fragment_job_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")
        
        // Initialize views
        val jobTitleTextView = view.findViewById<TextView>(R.id.jobTitle)
        
        // Job details views
        val summaryTextView = view.findViewById<TextView>(R.id.summaryText)
        val keyDutiesTextView = view.findViewById<TextView>(R.id.keyDutiesText)
        val essentialSkillsTextView = view.findViewById<TextView>(R.id.essentialSkillsText)
        val qualificationsTextView = view.findViewById<TextView>(R.id.qualificationsText)
        
        val applicationStatusTextView = view.findViewById<TextView>(R.id.applicationStatusText)
        val applyButton = view.findViewById<MaterialButton>(R.id.applyButton)
        
        // Check if we have a selected job already
        val currentJob = viewModel.selectedJob.value
        Log.d(TAG, "Current selected job at start: ${currentJob?.id ?: "None"}")
        
        // Set up back button
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Set up info button
        view.findViewById<View>(R.id.infoButton)?.setOnClickListener {
            showJobInfoDialog()
        }
        
        // Set up apply button
        applyButton.setOnClickListener {
            applyForJob()
        }
        
        // Add long press listener for API connection testing (debug feature)
        applyButton.setOnLongClickListener {
            testApiConnection()
            true
        }
        
        // Observe selected job
        viewModel.selectedJob.observe(viewLifecycleOwner) { job ->
            Log.d(TAG, "Job observer triggered: ${job != null}")
            
            if (job == null) {
                Log.e(TAG, "Received null job, navigating up")
                findNavController().navigateUp()
                return@observe
            }
            
            // Log detailed job info for debugging
            Log.d(TAG, "Job ID: ${job.id}")
            Log.d(TAG, "Job Title: ${job.title}")
            Log.d(TAG, "Job Department: ${job.department}")
            Log.d(TAG, "Job Summary: ${job.summary}")
            Log.d(TAG, "Job Salary: ${job.salary}")
            Log.d(TAG, "Job Status: ${job.status}")
            Log.d(TAG, "Job Work Setup: ${job.workSetup}")
            Log.d(TAG, "Job Available Slots: ${job.availableSlots}")
            Log.d(TAG, "Job Key Duties: ${job.keyDuties?.joinToString(", ") ?: "null"}")
            Log.d(TAG, "Job Essential Skills: ${job.essentialSkills?.joinToString(", ") ?: "null"}")
            Log.d(TAG, "Job Qualifications: ${job.qualifications?.joinToString(", ") ?: "null"}")
            
            try {
                // Populate views with job data
                jobTitleTextView.text = job.title
                
                // Set summary
                summaryTextView.text = job.summary ?: job.description.takeIf { it.isNotEmpty() } ?: "No summary provided"
                
                // Format and set Key Duties
                if (job.keyDuties?.isNotEmpty() == true) {
                    val formattedDuties = job.keyDuties.joinToString("\n") { "• $it" }
                    keyDutiesTextView.text = formattedDuties
                    Log.d(TAG, "Formatted duties: $formattedDuties")
                } else {
                    keyDutiesTextView.text = "• No specific duties listed"
                }
                
                // Format and set Essential Skills
                if (job.essentialSkills?.isNotEmpty() == true) {
                    val formattedSkills = job.essentialSkills.joinToString("\n") { "• $it" }
                    essentialSkillsTextView.text = formattedSkills
                    Log.d(TAG, "Formatted skills: $formattedSkills") 
                } else {
                    essentialSkillsTextView.text = "• No specific skills listed"
                }
                
                // Format and set Qualifications
                if (job.qualifications?.isNotEmpty() == true) {
                    val formattedQualifications = job.qualifications.joinToString("\n") { "• $it" }
                    qualificationsTextView.text = formattedQualifications
                    Log.d(TAG, "Formatted qualifications: $formattedQualifications")
                } else {
                    qualificationsTextView.text = "• No specific qualifications listed"
                }
                
                // Format and set Application Status
                // Check if user is logged in and has an application
                val currentUser = authViewModel.currentUser.value
                
                if (currentUser != null) {
                    // Check if there's an existing application for this job
                    lifecycleScope.launch {
                        checkForExistingApplication(job.universityId, job.id, currentUser.email, applicationStatusTextView)
                    }
                } else {
                    // Show a placeholder for application status
                    applicationStatusTextView.text = "No application submitted yet"
                }
                
                Log.d(TAG, "All data binding completed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error populating job details: ${e.message}", e)
            }
        }
    }
    
    private fun showJobInfoDialog() {
        try {
            Log.d(TAG, "Showing job info dialog")
            val job = viewModel.selectedJob.value ?: return
            
            // Create and configure the dialog
            context?.let { ctx ->
                jobInfoDialog = Dialog(ctx).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(R.layout.dialog_job_info)
                    
                    // Set dialog size and style
                    window?.apply {
                        setLayout(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        
                        // Add rounded corners to the dialog window
                        setBackgroundDrawableResource(android.R.color.transparent)
                        decorView.background = resources.getDrawable(
                            R.drawable.dialog_rounded_background, context.theme
                        )
                    }
                    
                    // Set up close button
                    findViewById<ImageView>(R.id.closeButton)?.setOnClickListener {
                        dismiss()
                    }
                    
                    // Populate dialog with job information
                    // Posted date
                    findViewById<TextView>(R.id.postedDateValueText)?.text = 
                        dateFormatter.format(job.postedDate)
                    
                    // University
                    findViewById<TextView>(R.id.universityValueText)?.text = 
                        job.universityName.takeIf { it.isNotEmpty() } ?: "Not specified"
                    
                    // Department
                    findViewById<TextView>(R.id.departmentValueText)?.text = 
                        job.department?.takeIf { it.isNotEmpty() } 
                        ?: job.company.takeIf { it.isNotEmpty() }
                        ?: "Not specified"
                    
                    // Salary
                    findViewById<TextView>(R.id.salaryValueText)?.text = 
                        job.salary?.takeIf { it.isNotEmpty() } ?: "Not specified"
                    
                    // Status
                    findViewById<TextView>(R.id.statusValueText)?.text = 
                        job.status?.takeIf { it.isNotEmpty() } 
                        ?: job.jobType?.takeIf { it.isNotEmpty() } 
                        ?: "Not specified"
                    
                    // Work Setup
                    findViewById<TextView>(R.id.workSetupValueText)?.text = 
                        job.workSetup?.takeIf { it.isNotEmpty() } ?: "Not specified"
                    
                    // Available Slots
                    findViewById<TextView>(R.id.availableSlotsValueText)?.text = 
                        job.availableSlots?.toString() ?: "Not specified"
                    
                    // Criteria Weights
                    if (job.criteriaWeights != null) {
                        findViewById<TextView>(R.id.educationWeightValueText)?.text = 
                            "${job.criteriaWeights.education}%"
                        
                        findViewById<TextView>(R.id.skillsWeightValueText)?.text = 
                            "${job.criteriaWeights.skills}%"
                        
                        findViewById<TextView>(R.id.experienceWeightValueText)?.text = 
                            "${job.criteriaWeights.experience}%"
                    } else {
                        // If criteria weights are not available, show default values
                        findViewById<TextView>(R.id.educationWeightValueText)?.text = "Not specified"
                        findViewById<TextView>(R.id.skillsWeightValueText)?.text = "Not specified"
                        findViewById<TextView>(R.id.experienceWeightValueText)?.text = "Not specified"
                    }
                    
                    // Show the dialog
                    show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing job info dialog: ${e.message}", e)
        }
    }
    
    private fun applyForJob() {
        // Check if user is logged in
        if (!authViewModel.isUserLoggedIn(requireContext())) {
            showLoginRequiredDialog()
            return
        }
        
        // Check if user has uploaded a resume
        val currentUser = authViewModel.currentUser.value
        if (currentUser == null || !currentUser.hasResume) {
            showResumeRequiredDialog()
            return
        }
        
        // Get the selected job details
        val selectedJob = viewModel.selectedJob.value ?: return
        
        // Extract job details for the API request
        val jobSummary = selectedJob.summary ?: selectedJob.description
        val keyDuties = selectedJob.keyDuties?.joinToString(", ") ?: ""
        val essentialSkills = selectedJob.essentialSkills?.joinToString(", ") ?: ""
        val qualifications = selectedJob.qualifications?.joinToString(", ") ?: ""
        
        // Log criteria weights if available
        selectedJob.criteriaWeights?.let {
            Log.d(TAG, "Job has criteria weights - Education: ${it.education}%, Skills: ${it.skills}%, Experience: ${it.experience}%")
        } ?: Log.d(TAG, "Job does not have criteria weights defined")
        
        // Show application confirmation dialog
        showApplicationConfirmationDialog(jobSummary, keyDuties, essentialSkills, qualifications, currentUser.resumeUrl, currentUser.email, selectedJob.id, selectedJob.title)
    }
    
    private fun showLoginRequiredDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Login Required")
            .setMessage("You need to be logged in to apply for this job. Would you like to sign in now?")
            .setPositiveButton("Sign In") { _, _ ->
                // Navigate to login screen
                findNavController().navigate(R.id.loginFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showResumeRequiredDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Resume Required")
            .setMessage("You need to upload your resume before applying for this job. Would you like to upload your resume now?")
            .setPositiveButton("Upload Resume") { _, _ ->
                // Navigate to the resume upload screen
                findNavController().navigate(R.id.action_jobDetailFragment_to_resumeUploadFragment)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun showApplicationConfirmationDialog(
        jobSummary: String,
        keyDuties: String,
        essentialSkills: String,
        qualifications: String,
        resumeUrl: String?,
        userId: String,
        jobId: String,
        jobTitle: String
    ) {
        if (resumeUrl == null) {
            Toast.makeText(
                context,
                "No resume found. Please upload your resume first.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Get the selected job to access criteria weights
        val selectedJob = viewModel.selectedJob.value
        
        AlertDialog.Builder(requireContext())
            .setTitle("Submit Application")
            .setMessage("Your application will be analyzed using your resume and the job details. Continue?")
            .setPositiveButton("Submit") { _, _ ->
                // Show processing toast
                Toast.makeText(
                    context,
                    "Please wait while our system is checking your eligibility for this position...",
                    Toast.LENGTH_LONG
                ).show()
                
                // Submit application to analysis API
                submitApplication(jobSummary, keyDuties, essentialSkills, qualifications, resumeUrl, userId, jobId, jobTitle, selectedJob?.criteriaWeights)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun submitApplication(
        jobSummary: String,
        keyDuties: String,
        essentialSkills: String,
        qualifications: String,
        resumeUrl: String,
        userId: String,
        jobId: String,
        jobTitle: String,
        criteriaWeights: CriteriaWeights?
    ) {
        // Use lifecycleScope to launch a coroutine
        lifecycleScope.launch {
            try {
                // Show loading indicator
                val loadingDialog = AlertDialog.Builder(requireContext())
                    .setTitle("Processing")
                    .setMessage("Please wait while our system is checking your eligibility for this position...")
                    .setCancelable(false)
                    .create()
                loadingDialog.show()
                
                // Log application submission details
                Log.d(TAG, "Submitting application for job: $jobTitle (ID: $jobId)")
                Log.d(TAG, "Using resume URL: $resumeUrl")
                
                // Convert CriteriaWeights to Map<String, Int> if available
                val weightsMap = criteriaWeights?.let {
                    mapOf(
                        "education" to it.education,
                        "skills" to it.skills,
                        "experience" to it.experience
                    )
                }
                
                // Log weights if available
                weightsMap?.let {
                    Log.d(TAG, "Using criteria weights: education=${it["education"]}, skills=${it["skills"]}, experience=${it["experience"]}")
                }
                
                // Get the user's displayName from AuthViewModel
                val currentUser = authViewModel.currentUser.value
                val displayName = currentUser?.displayName
                
                // Call the API in the background
                try {
                    val analysisResult = withContext(Dispatchers.IO) {
                        applicationRepository.submitApplicationForAnalysis(
                            requireContext(),
                            resumeUrl,
                            jobSummary,
                            keyDuties,
                            essentialSkills,
                            qualifications,
                            userId,
                            jobId,
                            jobTitle,
                            displayName,
                            weightsMap
                        )
                    }
                    
                    // Dismiss loading dialog
                    loadingDialog.dismiss()
                    
                    // Show result dialog
                    showApplicationResultDialog(analysisResult)
                } catch (e: Exception) {
                    // Dismiss loading dialog
                    loadingDialog.dismiss()
                    
                    Log.e(TAG, "API call error: ${e.message}", e)
                    
                    // Show a more user-friendly error message
                    val errorMessage = when {
                        e.message?.contains("Failed to connect") == true || 
                        e.message?.contains("Connection refused") == true -> 
                            "Could not connect to our analysis service. Please check your internet connection and try again."
                        
                        e.message?.contains("timeout") == true -> 
                            "The application analysis is taking longer than expected. Please try again later."
                            
                        e.message?.contains("parse") == true ->
                            "There was a problem processing your application. Our team has been notified."
                            
                        else -> "Error submitting application: ${e.message}"
                    }
                    
                    // Show error dialog
                    AlertDialog.Builder(requireContext())
                        .setTitle("Application Error")
                        .setMessage(errorMessage)
                        .setPositiveButton("OK", null)
                        .show()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error in application process: ${e.message}", e)
                
                // Show error message
                Toast.makeText(
                    context,
                    "Error submitting application: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun showApplicationResultDialog(analysis: ApplicationAnalysis) {
        try {
            // Log the raw analysis data for debugging
            Log.d(TAG, "Raw analysis data - Match %: ${analysis.matchPercentage}, Recommendation: ${analysis.recommendation}")
            Log.d(TAG, "Skills match - matched: ${analysis.skillsMatch.matchedSkills}, missing: ${analysis.skillsMatch.missingSkills}")
            Log.d(TAG, "Experience - applicant: ${analysis.experience.applicantYears}, required: ${analysis.experience.requiredYears}")
            Log.d(TAG, "Education - applicant: ${analysis.education.applicantEducation}, required: ${analysis.education.requirement}")
            
            context?.let { ctx ->
                applicationResultDialog = Dialog(ctx).apply {
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    setContentView(R.layout.dialog_application_result)
                    
                    // Set dialog size and style
                    window?.apply {
                        setLayout(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                        
                        // Add rounded corners to the dialog window
                        setBackgroundDrawableResource(android.R.color.transparent)
                    }
                    
                    // Set up close button
                    findViewById<ImageView>(R.id.closeButton)?.setOnClickListener {
                        dismiss()
                    }
                    
                    // Set up OK button
                    findViewById<MaterialButton>(R.id.okButton)?.setOnClickListener {
                        dismiss()
                    }
                    
                    // Format and populate the match percentage
                    val matchPercentage = analysis.matchPercentage.trim()
                    Log.d(TAG, "Formatting match percentage: '$matchPercentage'")
                    
                    val formattedMatchText = when {
                        matchPercentage.isEmpty() -> "0% Match"
                        matchPercentage.endsWith("%") -> "$matchPercentage Match" 
                        else -> "${matchPercentage}% Match"
                    }
                    Log.d(TAG, "Final formatted match text: '$formattedMatchText'")
                    findViewById<TextView>(R.id.matchPercentageText)?.text = formattedMatchText
                    
                    // Set recommendation with proper capitalization
                    val recommendation = try {
                        if (analysis.recommendation.isNullOrEmpty()) {
                            "Reject" // Default to reject if empty
                        } else {
                            analysis.recommendation.replaceFirstChar { 
                                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) 
                                else it.toString() 
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error formatting recommendation: ${e.message}", e)
                        "Reject" 
                    }
                    findViewById<TextView>(R.id.recommendationText)?.text = recommendation
                    
                    // Skills match
                    findViewById<TextView>(R.id.skillsMatchText)?.text = try {
                        val matchedSkills = analysis.skillsMatch.matchedSkills ?: emptyList()
                        val missingSkills = analysis.skillsMatch.missingSkills ?: emptyList()
                        
                        if (matchedSkills.isEmpty() && missingSkills.isEmpty()) {
                            "Skills information not available"
                        } else {
                            val matchedCount = matchedSkills.size
                            val totalSkills = matchedCount + missingSkills.size
                            if (totalSkills > 0) {
                                "$matchedCount/$totalSkills skills matched"
                            } else {
                                "Skills information not available"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting skills match text: ${e.message}", e)
                        "Skills information not available"
                    }
                    
                    // Experience
                    findViewById<TextView>(R.id.experienceText)?.text = try {
                        val experience = analysis.experience
                        
                        val applicantYears = experience.applicantYears.takeIf { 
                            !it.isNullOrEmpty() && it != "Not specified" 
                        } ?: "Unknown"
                        
                        val requiredYears = experience.requiredYears.takeIf { 
                            !it.isNullOrEmpty() && it != "Not specified" 
                        } ?: "Not specified"
                        
                        if (applicantYears == "Unknown" && requiredYears == "Not specified") {
                            "Experience information not available"
                        } else {
                            "$applicantYears years experience ($requiredYears required)"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting experience text: ${e.message}", e)
                        "Experience information not available"
                    }
                    
                    // Education
                    findViewById<TextView>(R.id.educationText)?.text = try {
                        val educationData = analysis.education
                        
                        val applicantEdu = educationData.applicantEducation.takeIf { 
                            !it.isNullOrEmpty() 
                        } ?: "Not specified"
                        
                        val requirement = educationData.requirement.takeIf { 
                            !it.isNullOrEmpty() 
                        } ?: "Not specified"
                        
                        if (applicantEdu == "Not specified" && requirement == "Not specified") {
                            "Not specified (Not specified required)"
                        } else {
                            "$applicantEdu ($requirement required)"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting education text: ${e.message}", e)
                        "Education information not available"
                    }
                    
                    // Improvement suggestions
                    val suggestions = buildSuggestionsList(analysis)
                    findViewById<TextView>(R.id.improvementSuggestionsText)?.text = suggestions
                    
                    // Salary estimate
                    findViewById<TextView>(R.id.salaryEstimateText)?.text = try {
                        val salaryEstimate = analysis.salaryEstimate
                        
                        if (salaryEstimate != null) {
                            val min = salaryEstimate.min.takeIf { it > 0 } ?: 0
                            val max = salaryEstimate.max.takeIf { it > 0 } ?: 0
                            val currency = salaryEstimate.currency.takeIf { 
                                !it.isNullOrEmpty() 
                            } ?: "USD"
                            
                            if (min > 0 || max > 0) {
                                "$${min.toFormattedString()} - $${max.toFormattedString()} $currency"
                            } else {
                                "Salary information not available"
                            }
                        } else {
                            "Salary information not available"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting salary estimate text: ${e.message}", e)
                        "Salary information not available"
                    }
                    
                    // Show the dialog
                    show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing application result dialog: ${e.message}", e)
            
            // Fallback to simple toast
            Toast.makeText(
                context,
                "Application Result: ${analysis.recommendation} (${analysis.matchPercentage} match)",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun buildSuggestionsList(analysis: ApplicationAnalysis): String {
        try {
            val suggestionsList = mutableListOf<String>()
            
            // Get improvementSuggestions safely
            val suggestions = analysis.improvementSuggestions
            if (suggestions != null) {
                // Add skills suggestions
                suggestions.skills?.let { suggestionsList.addAll(it) }
                
                // Add experience suggestions
                suggestions.experience?.let { suggestionsList.addAll(it) }
                
                // Add education suggestions
                suggestions.education?.let { suggestionsList.addAll(it) }
                
                // Add general suggestions
                suggestions.general?.let { suggestionsList.addAll(it) }
            }
            
            // Format the list with bullet points
            return if (suggestionsList.isNotEmpty()) {
                suggestionsList.joinToString("\n") { "• $it" }
            } else {
                "No specific improvements needed"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error building suggestions list: ${e.message}", e)
            return "No specific improvements needed"
        }
    }
    
    // Extension function to format numbers with commas
    private fun Int.toFormattedString(): String {
        return String.format(Locale.US, "%,d", this)
    }
    
    private fun testApiConnection() {
        lifecycleScope.launch {
            try {
                // Show loading indicator
                val loadingDialog = AlertDialog.Builder(requireContext())
                    .setTitle("Testing API Connection")
                    .setMessage("Please wait while we check the connection to the API server...")
                    .setCancelable(false)
                    .create()
                loadingDialog.show()
                
                // Test the connection
                val result = withContext(Dispatchers.IO) {
                    applicationRepository.testApiConnection()
                }
                
                // Dismiss loading dialog
                loadingDialog.dismiss()
                
                // Show result dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("API Connection Test")
                    .setMessage(result)
                    .setPositiveButton("OK", null)
                    .show()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error testing API connection: ${e.message}", e)
                
                // Show error dialog
                AlertDialog.Builder(requireContext())
                    .setTitle("Connection Test Error")
                    .setMessage("Failed to run connection test: ${e.message}")
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    /**
     * Checks if there's an existing application for the current user and job,
     * and updates the status view accordingly
     */
    private suspend fun checkForExistingApplication(
        universityId: String,
        jobId: String,
        userId: String,
        statusTextView: TextView
    ) {
        try {
            Log.d(TAG, "Checking for existing application for job $jobId with userId $userId")
            statusTextView.text = "Checking application status..."
            
            val db = FirebaseFirestore.getInstance()
            
            // Try the original userId format first (which might be an email)
            var applicantDoc = withContext(Dispatchers.IO) {
                db.collection("universities")
                    .document(universityId)
                    .collection("jobs")
                    .document(jobId)
                    .collection("applicants")
                    .document(userId)
                    .get()
                    .await()
            }
            
            // If userId contains @ (is an email), and the document doesn't exist,
            // try with the sanitized version of the email
            if (!applicantDoc.exists() && userId.contains("@")) {
                val sanitizedUserId = userId.replace("@", "-").replace(".", "-")
                Log.d(TAG, "No application found with email, trying sanitized userId: $sanitizedUserId")
                
                applicantDoc = withContext(Dispatchers.IO) {
                    db.collection("universities")
                        .document(universityId)
                        .collection("jobs")
                        .document(jobId)
                        .collection("applicants")
                        .document(sanitizedUserId)
                        .get()
                        .await()
                }
            }
            
            // If the email is already in sanitized format (contains - but not @),
            // try with the original email format
            if (!applicantDoc.exists() && !userId.contains("@") && userId.contains("-")) {
                // This is a more complex case - try to convert from sanitized back to original
                // This is an approximation and might not always work correctly
                Log.d(TAG, "Trying to un-sanitize the email format: $userId")
                
                // Find the position of the first - and replace it with @
                val parts = userId.split("-")
                if (parts.size >= 2) {
                    // Basic attempt to reconstruct the email
                    val possibleEmail = parts[0] + "@" + parts.subList(1, parts.size).joinToString(".")
                    Log.d(TAG, "Trying possible original email format: $possibleEmail")
                    
                    applicantDoc = withContext(Dispatchers.IO) {
                        db.collection("universities")
                            .document(universityId)
                            .collection("jobs")
                            .document(jobId)
                            .collection("applicants")
                            .document(possibleEmail)
                            .get()
                            .await()
                    }
                }
            }
            
            // If we still didn't find an application, try a more generic path format
            if (!applicantDoc.exists()) {
                Log.d(TAG, "Trying alternative document format")
                
                // Parse user's email to document ID format
                val emailAsDocId = userId.replace("@", "-").replace(".", "-")
                
                // Access using the actual job and university IDs
                applicantDoc = withContext(Dispatchers.IO) {
                    db.collection("universities")
                        .document(universityId)
                        .collection("jobs")
                        .document(jobId)
                        .collection("applicants")
                        .document(emailAsDocId)
                        .get()
                        .await()
                }
            }
            
            if (applicantDoc.exists()) {
                // Found existing application
                Log.d(TAG, "Found existing application with ID: ${applicantDoc.id}")
                
                // Build detailed status message
                val statusMessage = StringBuilder()
                
                // Get application status - Log all fields for debugging
                Log.d(TAG, "Application document fields: ${applicantDoc.data}")
                
                // Get the raw status string and log it
                val rawStatus = applicantDoc.getString("status")
                Log.d(TAG, "Raw status from Firestore: $rawStatus")
                
                // IMPORTANT - Simply display the raw status directly without any processing or conversion
                // This ensures we show exactly what's in Firestore
                val statusDisplay = rawStatus ?: "Pending"
                Log.d(TAG, "Using raw status for display: $statusDisplay")
                
                // Format the status for display - use the original string value from Firestore
                statusMessage.append("Application Status: $statusDisplay\n\n")
                
                // Get match percentage
                val matchPercentage = applicantDoc.getString("matchPercentage") ?: "N/A"
                statusMessage.append("Match Percentage: $matchPercentage\n\n")
                
                // Get skills match
                val skillsMatch = applicantDoc.getString("skillsMatch") ?: "N/A"
                statusMessage.append("Skills Match:\n$skillsMatch\n\n")
                
                // Get experience
                val experience = applicantDoc.getString("experience") ?: "N/A"
                statusMessage.append("Experience:\n$experience\n\n")
                
                // Get education
                val education = applicantDoc.getString("education") ?: "N/A"
                statusMessage.append("Education:\n$education\n\n")
                
                // Get recommendation
                val recommendation = applicantDoc.getString("recommendation") ?: "N/A"
                statusMessage.append("Recommendation:\n$recommendation")
                
                // Update the UI
                statusTextView.text = statusMessage.toString()
                
                // Add Schedule Interview button
                val applyButton = view?.findViewById<MaterialButton>(R.id.applyButton)
                applyButton?.let {
                    it.text = "Schedule Interview"
                    it.setOnClickListener {
                        // Navigate to schedule interview fragment with applicant data
                        val effectiveApplicantId = applicantDoc.id
                        val bundle = Bundle().apply {
                            putString("applicantId", effectiveApplicantId)
                            putString("jobId", jobId)
                            putString("universityId", universityId)
                        }
                        findNavController().navigate(R.id.action_jobDetailFragment_to_scheduleInterviewFragment, bundle)
                    }
                }
            } else {
                // No application found with any of the attempted formats
                Log.d(TAG, "No application found for user with any ID format")
                statusTextView.text = "No application submitted yet"
                
                // Reset apply button to original state
                val applyButton = view?.findViewById<MaterialButton>(R.id.applyButton)
                applyButton?.let {
                    it.text = "Apply Now"
                    it.setOnClickListener {
                        applyForJob()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for existing application: ${e.message}", e)
            statusTextView.text = "Error loading application status"
        }
    }
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Dismiss dialogs if they're showing to prevent memory leaks
        jobInfoDialog?.dismiss()
        jobInfoDialog = null
        applicationResultDialog?.dismiss()
        applicationResultDialog = null
    }
} 