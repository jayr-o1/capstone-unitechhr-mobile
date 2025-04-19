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

class JobDetailFragment : Fragment() {

    private val viewModel: JobViewModel by activityViewModels()
    
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
        val companyNameTextView = view.findViewById<TextView>(R.id.companyName)
        val locationTextView = view.findViewById<TextView>(R.id.location)
        val salaryValueTextView = view.findViewById<TextView>(R.id.salaryValue)
        val jobTypeValueTextView = view.findViewById<TextView>(R.id.jobTypeValue)
        val descriptionTextView = view.findViewById<TextView>(R.id.descriptionText)
        val requirementsTextView = view.findViewById<TextView>(R.id.requirementsText)
        val applyButton = view.findViewById<MaterialButton>(R.id.applyButton)
        
        // Set up back button
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Observe selected job
        viewModel.selectedJob.observe(viewLifecycleOwner) { job ->
            if (job == null) {
                findNavController().navigateUp()
                return@observe
            }
            
            // Populate views with job data
            jobTitleTextView.text = job.title
            companyNameTextView.text = job.company
            locationTextView.text = job.location
            salaryValueTextView.text = job.salary
            jobTypeValueTextView.text = job.jobType
            descriptionTextView.text = job.description
            requirementsTextView.text = job.requirements
        }
        
        // Set up apply button
        applyButton.setOnClickListener {
            // This would navigate to an application form in a real app
            Toast.makeText(requireContext(), "Apply functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }
} 