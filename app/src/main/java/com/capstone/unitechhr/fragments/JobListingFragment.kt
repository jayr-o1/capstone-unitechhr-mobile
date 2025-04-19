package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.adapters.JobAdapter
import com.capstone.unitechhr.viewmodels.JobViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class JobListingFragment : Fragment() {
    
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var addJobFab: FloatingActionButton
    
    private val viewModel: JobViewModel by viewModels()
    private lateinit var adapter: JobAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_job_listing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        jobsRecyclerView = view.findViewById(R.id.jobsRecyclerView)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchIcon = view.findViewById(R.id.searchIcon)
        addJobFab = view.findViewById(R.id.addJobFab)
        
        // Set up adapter
        adapter = JobAdapter { job ->
            // Navigate to job details
            viewModel.selectJob(job)
            findNavController().navigate(R.id.action_jobListingFragment_to_jobDetailFragment)
        }
        
        // Set up RecyclerView
        jobsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        jobsRecyclerView.adapter = adapter
        
        // Observe view model data
        viewModel.jobs.observe(viewLifecycleOwner) { jobs ->
            adapter.submitList(jobs)
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        
        // Set up search functionality
        searchIcon.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchJobs(query)
            } else {
                viewModel.loadJobs()
            }
        }
        
        // Set up FAB for adding new jobs
        addJobFab.setOnClickListener {
            // TODO: Navigate to Add Job screen or show dialog
            Toast.makeText(requireContext(), "Add Job functionality coming soon", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onResume() {
        super.onResume()
        viewModel.loadJobs()
    }
} 