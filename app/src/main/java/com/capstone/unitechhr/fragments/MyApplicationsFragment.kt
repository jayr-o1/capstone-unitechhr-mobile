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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.adapters.ApplicationAdapter
import com.capstone.unitechhr.models.Application
import com.capstone.unitechhr.utils.NotificationUtils
import com.capstone.unitechhr.viewmodels.ApplicationViewModel
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator

class MyApplicationsFragment : Fragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val applicationViewModel: ApplicationViewModel by activityViewModels()
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var noDataView: View
    private lateinit var titleText: TextView
    private lateinit var adapter: ApplicationAdapter
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Reuse the applicant list layout but we'll customize it
        return inflater.inflate(R.layout.fragment_applicant_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        recyclerView = view.findViewById(R.id.applicants_recycler_view)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        noDataView = view.findViewById(R.id.no_data_view)
        titleText = view.findViewById(R.id.title)
        
        // Change the title
        titleText.text = "My Applications"
        
        // Set up back button
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Hide the search view
        view.findViewById<View>(R.id.search_view)?.visibility = View.GONE
        
        // Find and update empty state texts
        val emptyTitle = noDataView.findViewById<TextView>(R.id.empty_title)
        val emptyDescription = noDataView.findViewById<TextView>(R.id.empty_description)
        
        emptyTitle?.text = "No Applications Found"
        emptyDescription?.text = "You haven't applied to any jobs yet. Browse available jobs and apply to start tracking your applications here."
        
        // Get the FAB or create a new one for testing notifications
        val fab = view.findViewById<FloatingActionButton>(R.id.fab_add_applicant)
        if (fab != null) {
            // Repurpose the existing FAB for test notifications
            fab.setImageResource(android.R.drawable.ic_popup_reminder)
            fab.visibility = View.VISIBLE
            fab.setOnClickListener {
                sendTestNotification()
            }
        }
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Observe data changes
        applicationViewModel.myApplications.observe(viewLifecycleOwner) { applications ->
            adapter.submitList(applications)
            updateEmptyState(applications.isEmpty())
        }
        
        // Observe loading state
        applicationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe errors
        applicationViewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        }
        
        // Load user's applications
        loadUserApplications()
    }
    
    private fun setupRecyclerView() {
        adapter = ApplicationAdapter { application ->
            // Handle application click - view details
            applicationViewModel.selectApplication(application)
            // Get job details if needed (for future implementation)
            Toast.makeText(context, "Application for: ${application.jobTitle}", Toast.LENGTH_SHORT).show()
        }
        
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun loadUserApplications() {
        authViewModel.currentUser.value?.let { user ->
            val userId = user.email.replace("@", "-").replace(".", "-")
            applicationViewModel.loadUserApplications(userId)
        }
    }
    
    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerView.visibility = View.GONE
            noDataView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            noDataView.visibility = View.GONE
        }
    }
    
    /**
     * Send a test notification to demonstrate push notifications
     */
    private fun sendTestNotification() {
        context?.let { ctx ->
            NotificationUtils.showNotification(
                ctx,
                "UniTech HR Update",
                "This is a test notification. Your application status may have changed!"
            )
            Toast.makeText(ctx, "Test notification sent!", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        loadUserApplications()
    }
} 