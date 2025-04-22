package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.adapters.InterviewAdapter
import com.capstone.unitechhr.adapters.InterviewWithDetails
import com.capstone.unitechhr.models.InterviewStatus
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.capstone.unitechhr.viewmodels.InterviewViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import android.util.Log

class InterviewListFragment : Fragment() {
    
    private val viewModel: InterviewViewModel by viewModels()
    private val authViewModel: AuthViewModel by activityViewModels()
    private lateinit var adapter: InterviewAdapter
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateContainer: View
    private lateinit var scheduleFab: FloatingActionButton
    private lateinit var tabLayout: TabLayout
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_interview_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        recyclerView = view.findViewById(R.id.interviewsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        scheduleFab = view.findViewById(R.id.scheduleInterviewFab)
        tabLayout = view.findViewById(R.id.tabLayout)
        
        // Set up back button
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Setup RecyclerView adapter
        adapter = InterviewAdapter(
            onViewDetailsClick = { interview ->
                viewModel.selectInterview(interview)
                findNavController().navigate(R.id.action_interviewListFragment_to_interviewDetailFragment)
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        
        // Setup tab layout listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                filterInterviews(tab.position)
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            
            override fun onTabReselected(tab: TabLayout.Tab) {
                filterInterviews(tab.position)
            }
        })
        
        // Setup FAB click listener
        scheduleFab.setOnClickListener {
            findNavController().navigate(R.id.action_interviewListFragment_to_scheduleInterviewFragment)
        }
        
        // Observe ViewModel data
        viewModel.interviews.observe(viewLifecycleOwner) { interviews ->
            val currentTabPosition = tabLayout.selectedTabPosition
            filterInterviews(currentTabPosition)
        }
        
        viewModel.interviewApplicants.observe(viewLifecycleOwner) { applicantsMap ->
            // When applicants data changes, update the adapter
            updateAdapterData()
        }
        
        viewModel.interviewJobs.observe(viewLifecycleOwner) { jobsMap ->
            // When jobs data changes, update the adapter
            updateAdapterData()
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.operationStatus.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Toast.makeText(requireContext(), "Operation successful", Toast.LENGTH_SHORT).show()
                viewModel.clearOperationStatus()
            } else if (success == false) {
                Toast.makeText(requireContext(), "Operation failed", Toast.LENGTH_SHORT).show()
                viewModel.clearOperationStatus()
            }
        }
        
        // Load interviews
        loadInterviews()
    }
    
    override fun onResume() {
        super.onResume()
        loadInterviews()
    }
    
    private fun loadInterviews() {
        val userEmail = authViewModel.currentUser.value?.email
        if (userEmail != null) {
            Log.d("InterviewListFragment", "Loading interviews for user email: $userEmail")
            
            // First try to load from applicant subcollection (which seems to be where your interviews are stored)
            viewModel.loadInterviewsFromApplicantSubcollection(userEmail)
        } else {
            // Fallback to loading all interviews if no user email is available
            Log.d("InterviewListFragment", "No user email found, loading all interviews")
            viewModel.loadInterviews()
        }
    }
    
    private fun updateAdapterData() {
        val interviews = viewModel.interviews.value ?: emptyList()
        val applicantsMap = viewModel.interviewApplicants.value ?: emptyMap()
        val jobsMap = viewModel.interviewJobs.value ?: emptyMap()
        
        Log.d("InterviewListFragment", "Updating adapter with ${interviews.size} interviews")
        if (interviews.isEmpty()) {
            Log.d("InterviewListFragment", "No interviews found for the user")
            // Try to find the empty state text view to update message
            view?.findViewById<TextView>(R.id.emptyDescription)?.let { textView ->
                if (viewModel.errorMessage.value != null) {
                    textView.text = "Unable to load your scheduled interviews due to a format mismatch."
                } else {
                    textView.text = "You don't have any interviews scheduled."
                }
            }
        } else {
            Log.d("InterviewListFragment", "Found ${interviews.size} interviews with applicantIds: ${interviews.map { it.applicantId }}")
        }
        
        val interviewWithDetailsList = interviews.map { interview ->
            InterviewWithDetails(
                interview = interview,
                applicant = applicantsMap[interview.applicantId],
                job = jobsMap[interview.jobId]
            )
        }
        
        if (interviewWithDetailsList.isEmpty()) {
            emptyStateContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            Log.d("InterviewListFragment", "Empty state shown: No interviews to display")
        } else {
            emptyStateContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            Log.d("InterviewListFragment", "Showing ${interviewWithDetailsList.size} interviews in list")
        }
        
        val currentTabPosition = tabLayout.selectedTabPosition
        filterInterviews(currentTabPosition)
    }
    
    private fun filterInterviews(tabPosition: Int) {
        val interviews = viewModel.interviews.value ?: emptyList()
        val applicantsMap = viewModel.interviewApplicants.value ?: emptyMap()
        val jobsMap = viewModel.interviewJobs.value ?: emptyMap()
        
        val filteredInterviews = when (tabPosition) {
            0 -> interviews.filter { it.status == InterviewStatus.SCHEDULED || it.status == InterviewStatus.RESCHEDULED }
            1 -> interviews.filter { it.status == InterviewStatus.COMPLETED }
            else -> interviews
        }
        
        val interviewWithDetailsList = filteredInterviews.map { interview ->
            InterviewWithDetails(
                interview = interview,
                applicant = applicantsMap[interview.applicantId],
                job = jobsMap[interview.jobId]
            )
        }
        
        if (interviewWithDetailsList.isEmpty()) {
            emptyStateContainer.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateContainer.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
        }
        
        adapter.submitList(interviewWithDetailsList)
    }
} 