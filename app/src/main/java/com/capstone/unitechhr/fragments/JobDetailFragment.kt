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
                
                // For now, show a placeholder for application status
                applicationStatusTextView.text = "No application submitted yet"
                
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
                submitApplication(jobSummary, keyDuties, essentialSkills, qualifications, resumeUrl, userId, jobId, jobTitle)
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
        jobTitle: String
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
                            jobTitle
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
                    
                    // Populate dialog with analysis results
                    findViewById<TextView>(R.id.matchPercentageText)?.text = "${analysis.matchPercentage}% Match"
                    findViewById<TextView>(R.id.recommendationText)?.text = analysis.recommendation
                    
                    // Skills match
                    findViewById<TextView>(R.id.skillsMatchText)?.text = try {
                        val skillsMatch = analysis.skillsMatch ?: SkillsMatch()
                        val matchRatio = try {
                            val ratioValue = skillsMatch.matchRatio
                            if (ratioValue != null && ratioValue.isNotEmpty()) ratioValue else "0"
                        } catch (e: Exception) {
                            "0"
                        }
                        "$matchRatio skills matched"
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting skills match text: ${e.message}", e)
                        "Skills requirement met"
                    }
                    
                    // Experience
                    findViewById<TextView>(R.id.experienceText)?.text = try {
                        val experience = analysis.experience ?: Experience()
                        val applicantYears = try { 
                            val yearsValue = experience.applicantYears
                            if (yearsValue != null && yearsValue.isNotEmpty()) yearsValue else "0" 
                        } catch (e: Exception) { 
                            "0" 
                        }
                        val requiredYears = try { 
                            val reqYearsValue = experience.requiredYears
                            if (reqYearsValue != null && reqYearsValue.isNotEmpty()) reqYearsValue else "0" 
                        } catch (e: Exception) { 
                            "0" 
                        }
                        "$applicantYears years experience ($requiredYears required)"
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting experience text: ${e.message}", e)
                        // Fallback if property access causes problems
                        "Experience requirement met"
                    }
                    
                    // Education
                    findViewById<TextView>(R.id.educationText)?.text = try {
                        val educationData = analysis.education ?: AnalysisEducation()
                        if (educationData != null) {
                            val applicantEdu = try { educationData.applicantEducation } catch (e: Exception) { null }
                            val requirement = try { educationData.requirement } catch (e: Exception) { null }
                            
                            val applicantEduDisplay = if (!applicantEdu.isNullOrEmpty()) applicantEdu else "Not specified"
                            val requirementDisplay = if (!requirement.isNullOrEmpty()) requirement else "Not specified"
                            
                            "$applicantEduDisplay ($requirementDisplay required)"
                        } else {
                            "Education details not available"
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting education text: ${e.message}", e)
                        "Education requirement met"
                    }
                    
                    // Improvement suggestions
                    val suggestions = buildSuggestionsList(analysis)
                    findViewById<TextView>(R.id.improvementSuggestionsText)?.text = suggestions
                    
                    // Salary estimate
                    findViewById<TextView>(R.id.salaryEstimateText)?.text = try {
                        val salaryEstimate = analysis.salaryEstimate ?: SalaryEstimate()
                        val min = try {
                            salaryEstimate.min
                        } catch (e: Exception) {
                            0
                        }
                        val max = try {
                            salaryEstimate.max
                        } catch (e: Exception) {
                            0
                        }
                        val currency = try {
                            val currencyValue = salaryEstimate.currency
                            if (currencyValue != null && currencyValue.isNotEmpty()) currencyValue else "USD"
                        } catch (e: Exception) {
                            "USD"
                        }
                        "$${min.toFormattedString()} - $${max.toFormattedString()} $currency"
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
                "Application Result: ${analysis.recommendation} (${analysis.matchPercentage}% match)",
                Toast.LENGTH_LONG
            ).show()
        }
    }
    
    private fun buildSuggestionsList(analysis: ApplicationAnalysis): String {
        val suggestionsList = mutableListOf<String>()
        
        // Add skills suggestions
        suggestionsList.addAll(analysis.improvementSuggestions.skills)
        
        // Add experience suggestions
        suggestionsList.addAll(analysis.improvementSuggestions.experience)
        
        // Add education suggestions
        suggestionsList.addAll(analysis.improvementSuggestions.education)
        
        // Add general suggestions
        suggestionsList.addAll(analysis.improvementSuggestions.general)
        
        // Format the list with bullet points
        return if (suggestionsList.isNotEmpty()) {
            suggestionsList.joinToString("\n") { "• $it" }
        } else {
            "No specific improvements needed"
        }
    }
    
    // Extension function to format numbers with commas
    private fun Int.toFormattedString(): String {
        return String.format(Locale.US, "%,d", this)
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