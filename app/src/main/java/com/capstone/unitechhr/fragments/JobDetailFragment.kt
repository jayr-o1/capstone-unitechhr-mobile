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
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R
import com.capstone.unitechhr.viewmodels.JobViewModel
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class JobDetailFragment : Fragment() {

    private val TAG = "JobDetailFragment"
    private val viewModel: JobViewModel by activityViewModels()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private var jobInfoDialog: Dialog? = null
    
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
            // In a complete implementation, this would navigate to an application form
            // For now, just show a toast
            Toast.makeText(context, "Application functionality coming soon!", Toast.LENGTH_SHORT).show()
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
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Dismiss dialog if it's showing to prevent memory leaks
        jobInfoDialog?.dismiss()
        jobInfoDialog = null
    }
} 