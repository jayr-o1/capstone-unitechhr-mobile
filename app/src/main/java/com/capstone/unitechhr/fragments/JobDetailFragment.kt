package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private val viewModel: JobViewModel by activityViewModels()
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_job_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        val jobTitleTextView = view.findViewById<TextView>(R.id.jobTitle)
        val postedDateTextView = view.findViewById<TextView>(R.id.postedDate)
        val universityNameTextView = view.findViewById<TextView>(R.id.universityName)
        val companyNameTextView = view.findViewById<TextView>(R.id.companyName)
        val locationTextView = view.findViewById<TextView>(R.id.location)
        val salaryValueTextView = view.findViewById<TextView>(R.id.salaryValue)
        val jobTypeValueTextView = view.findViewById<TextView>(R.id.jobTypeValue)
        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionText)
        val requirementsTextView = view.findViewById<TextView>(R.id.requirementsText)
        val applicationStatusTextView = view.findViewById<TextView>(R.id.applicationStatusText)
        val applyButton = view.findViewById<MaterialButton>(R.id.applyButton)
        
        // Set up back button
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Set up apply button
        applyButton.setOnClickListener {
            // In a complete implementation, this would navigate to an application form
            // For now, just show a toast
            Toast.makeText(context, "Application functionality coming soon!", Toast.LENGTH_SHORT).show()
        }
        
        // Observe selected job
        viewModel.selectedJob.observe(viewLifecycleOwner) { job ->
            if (job == null) {
                findNavController().navigateUp()
                return@observe
            }
            
            // Populate views with job data
            jobTitleTextView.text = job.title
            
            // Format and display posted date
            postedDateTextView.text = "Posted on: ${dateFormatter.format(job.postedDate)}"
            
            // Show university name if available
            if (job.universityName.isNotEmpty()) {
                universityNameTextView.text = job.universityName
                universityNameTextView.visibility = View.VISIBLE
            } else {
                universityNameTextView.visibility = View.GONE
            }
            
            // Set department as company name with fallback
            companyNameTextView.text = job.company.takeIf { it.isNotEmpty() } 
                ?: "University Department"
            
            // Set work setup as location with fallback
            locationTextView.text = job.location.takeIf { it.isNotEmpty() } 
                ?: "Location not specified"
            
            // Set salary with fallback
            salaryValueTextView.text = job.salary.takeIf { it.isNotEmpty() } 
                ?: "Not specified"
            
            // Set job status as job type with fallback
            jobTypeValueTextView.text = job.jobType.takeIf { it.isNotEmpty() } 
                ?: "Full-time" // Default to full-time if not specified
                
            // Set description with proper formatting and fallback
            descriptionTextView.text = job.description.takeIf { it.isNotEmpty() } 
                ?: "No description provided."
                
            // Set requirements with proper formatting and fallback
            requirementsTextView.text = job.requirements.takeIf { it.isNotEmpty() } 
                ?: "• No specific requirements listed."
            
            // For now, show a placeholder for application status
            // This would be replaced with real status check in a full implementation
            applicationStatusTextView.text = "No application submitted yet"
        }
    }
} 