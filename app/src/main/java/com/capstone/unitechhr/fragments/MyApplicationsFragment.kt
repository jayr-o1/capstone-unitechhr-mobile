package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.adapters.JobApplicationAdapter
import com.capstone.unitechhr.models.Job
import com.capstone.unitechhr.repositories.ApplicationRepository
import com.capstone.unitechhr.repositories.JobRepository
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.capstone.unitechhr.viewmodels.JobViewModel
import com.capstone.unitechhr.viewmodels.MyApplicationsViewModel
import kotlinx.coroutines.launch

class MyApplicationsFragment : Fragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val applicationsViewModel: MyApplicationsViewModel by viewModels()
    private val jobViewModel: JobViewModel by activityViewModels()
    private val TAG = "MyApplicationsFragment"
    
    // UI components
    private lateinit var backButton: ImageView
    private lateinit var applicationsRecyclerView: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var loadingOverlay: FrameLayout
    private lateinit var infoMessageTextView: TextView
    
    private lateinit var adapter: JobApplicationAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_applications, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        backButton = view.findViewById(R.id.backButton)
        applicationsRecyclerView = view.findViewById(R.id.applicationsRecyclerView)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        loadingOverlay = view.findViewById(R.id.loadingOverlay)
        infoMessageTextView = view.findViewById(R.id.infoMessageTextView)
        
        // Set up back button
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Make sure Jobs are loaded
        if (jobViewModel.jobs.value.isNullOrEmpty()) {
            Log.d(TAG, "Jobs not loaded yet, loading them now")
            jobViewModel.loadJobs()
        }
        
        // Setup RecyclerView
        adapter = JobApplicationAdapter { application ->
            // Handle application click - Navigate to job details
            Log.d(TAG, "Application clicked: ${application.jobTitle}, job ID: ${application.jobId}")
            
            // Store the selected application in ViewModel
            applicationsViewModel.selectApplication(application)
            
            // Get the job ID and find the job details
            val jobId = application.jobId
            
            // Clear any previous job first
            jobViewModel.clearSelectedJob()
            
            // Try to get the job by ID directly from all jobs first
            val allJobs = jobViewModel.jobs.value ?: emptyList()
            val selectedJob = allJobs.find { it.id == jobId }
            
            if (selectedJob != null) {
                // We found the job, select it and navigate to details
                Log.d(TAG, "Found job in loaded jobs: ${selectedJob.title}")
                jobViewModel.selectJob(selectedJob)
                findNavController().navigate(R.id.action_myApplicationsFragment_to_jobDetailFragment)
            } else {
                // We didn't find the job in the current list, try to load it from repository
                Log.d(TAG, "Job not found in loaded jobs, trying to fetch it")
                
                lifecycleScope.launch {
                    loadingOverlay.visibility = View.VISIBLE
                    
                    try {
                        // Try to find the job in any university
                        val jobRepository = JobRepository()
                        
                        // First try to get all jobs to see if we can find it
                        val allRepoJobs = jobRepository.getJobs()
                        val job = allRepoJobs.find { it.id == jobId }
                        
                        if (job != null) {
                            // Found the job
                            Log.d(TAG, "Found job from repository: ${job.title}")
                            jobViewModel.selectJob(job)
                            findNavController().navigate(R.id.action_myApplicationsFragment_to_jobDetailFragment)
                        } else {
                            // Show error message without hardcoded sample path
                            Toast.makeText(
                                requireContext(),
                                "Job details not found. Please try again later.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error fetching job: ${e.message}", e)
                        Toast.makeText(
                            requireContext(),
                            "Error loading job details: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    } finally {
                        loadingOverlay.visibility = View.GONE
                    }
                }
            }
        }
        
        applicationsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        applicationsRecyclerView.adapter = adapter
        
        // Observe ViewModel data
        applicationsViewModel.applications.observe(viewLifecycleOwner) { applications ->
            Log.d(TAG, "Received ${applications.size} applications")
            
            if (applications.isEmpty()) {
                emptyStateContainer.visibility = View.VISIBLE
                applicationsRecyclerView.visibility = View.GONE
            } else {
                emptyStateContainer.visibility = View.GONE
                applicationsRecyclerView.visibility = View.VISIBLE
                adapter.submitList(applications)
            }
        }
        
        applicationsViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        applicationsViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Log.e(TAG, "Error: $it")
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        
        // Load applications for current user
        authViewModel.currentUser.observe(viewLifecycleOwner) { userData ->
            userData?.let {
                loadApplications(it.email)
            }
        }
    }
    
    private fun loadApplications(userEmail: String) {
        Log.d(TAG, "Loading applications for user: $userEmail")
        applicationsViewModel.loadApplicationsForUser(userEmail)
    }
} 