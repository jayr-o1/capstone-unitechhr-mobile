package com.capstone.unitechhr.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R
import com.capstone.unitechhr.databinding.FragmentInterviewDetailBinding
import com.capstone.unitechhr.models.Interview
import com.capstone.unitechhr.models.InterviewStatus
import com.capstone.unitechhr.viewmodels.InterviewViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Fragment for interview actions
 */
class InterviewDetailFragment : Fragment() {
    
    private var _binding: FragmentInterviewDetailBinding? = null
    private val binding get() = _binding!!
    
    private val interviewViewModel: InterviewViewModel by activityViewModels()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentInterviewDetailBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Observe the selected interview
        interviewViewModel.selectedInterview.observe(viewLifecycleOwner) { interview ->
            interview?.let {
                setupActionButtons(it)
            }
        }
    }
    
    private fun setupActionButtons(interview: Interview) {
        // Enable or disable buttons based on interview status
        val isActive = interview.status == InterviewStatus.SCHEDULED
        
        // Join Meeting Button
        binding.joinMeetingButton.setOnClickListener {
            val link = interview.meetingLink
            if (link.isNotEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                startActivity(intent)
            }
        }
        
        // Show/hide join meeting button based on availability
        if (interview.location == "Virtual" && interview.meetingLink.isNotEmpty()) {
            binding.joinMeetingButton.visibility = View.VISIBLE
        } else {
            binding.joinMeetingButton.visibility = View.GONE
        }
        
        // Reschedule Button
        binding.rescheduleButton.apply {
            isEnabled = isActive
            alpha = if (isActive) 1.0f else 0.5f
            
            setOnClickListener {
                if (isActive) {
                    // Extract university ID and job ID from the job ID
                    var universityId = ""
                    var jobId = interview.jobId
                    
                    // Check if the job ID has the format "universityId:jobId"
                    if (interview.jobId.contains(":")) {
                        val parts = interview.jobId.split(":")
                        if (parts.size == 2) {
                            universityId = parts[0]
                            jobId = parts[1]
                        }
                    }
                    
                    // Navigate to the schedule interview fragment with the necessary information
                    findNavController().navigate(
                        R.id.action_interviewDetailFragment_to_scheduleInterviewFragment,
                        Bundle().apply {
                            putString("applicantId", interview.applicantId)
                            putString("jobId", jobId)
                            putString("universityId", universityId)
                        }
                    )
                }
            }
        }
        
        // Cancel Button
        binding.cancelButton.apply {
            isEnabled = isActive
            alpha = if (isActive) 1.0f else 0.5f
            
            setOnClickListener {
                if (isActive) {
                    showCancelConfirmationDialog(interview)
                }
            }
        }
    }
    
    private fun showCancelConfirmationDialog(interview: Interview) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Cancel Interview")
            .setMessage("Are you sure you want to cancel this interview?")
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .setPositiveButton("Yes") { _, _ ->
                cancelInterview(interview)
            }
            .show()
    }
    
    private fun cancelInterview(interview: Interview) {
        // Update interview status to "cancelled"
        interviewViewModel.cancelInterview(interview.id)
        
        // Show success message
        binding.root.post {
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 