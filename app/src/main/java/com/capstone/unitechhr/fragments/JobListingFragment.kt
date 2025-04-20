package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.adapters.JobAdapter
import com.capstone.unitechhr.adapters.UniversitySpinnerAdapter
import com.capstone.unitechhr.models.University
import com.capstone.unitechhr.viewmodels.JobViewModel
import com.capstone.unitechhr.viewmodels.UniversityViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

class JobListingFragment : Fragment() {
    
    private lateinit var jobsRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var addJobFab: FloatingActionButton
    private lateinit var universitySpinner: Spinner
    private lateinit var progressBar: ProgressBar
    
    private val jobViewModel: JobViewModel by viewModels()
    private val universityViewModel: UniversityViewModel by viewModels()
    private lateinit var jobAdapter: JobAdapter
    private lateinit var universityAdapter: UniversitySpinnerAdapter
    
    // Add a flag to prevent spinner callback during initialization
    private var isSpinnerInitialized = false

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
        universitySpinner = view.findViewById(R.id.universitySpinner)
        progressBar = view.findViewById(R.id.progressBar)
        
        // Set up job adapter
        jobAdapter = JobAdapter { job ->
            // Navigate to job details
            jobViewModel.selectJob(job)
            findNavController().navigate(R.id.action_jobListingFragment_to_jobDetailFragment)
        }
        
        // Set up RecyclerView
        jobsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        jobsRecyclerView.adapter = jobAdapter
        
        // Set up university spinner with a dummy adapter initially
        val dummyList = listOf(University(name = "Loading universities..."))
        universityAdapter = UniversitySpinnerAdapter(requireContext(), dummyList)
        universitySpinner.adapter = universityAdapter
        
        // Observe university data
        universityViewModel.universities.observe(viewLifecycleOwner) { universities ->
            // Create a mutable list with "All Universities" option at position 0
            val allUniversities = mutableListOf(University(id = "", name = "All Universities"))
            allUniversities.addAll(universities)
            
            // Update the spinner with the new data
            universityAdapter = UniversitySpinnerAdapter(requireContext(), allUniversities)
            universitySpinner.adapter = universityAdapter
            
            // Set the spinner to the previously selected university (if any)
            jobViewModel.selectedUniversityId.value?.let { selectedId ->
                if (selectedId.isNotEmpty()) {
                    val position = universityAdapter.getPositionById(selectedId)
                    universitySpinner.setSelection(position)
                }
            }
            
            isSpinnerInitialized = true
        }
        
        // Set up spinner listener
        universitySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (!isSpinnerInitialized) return
                
                val selectedUniversity = universityAdapter.getItem(position)
                selectedUniversity?.let {
                    // If position 0 (All Universities), clear the filter
                    if (position == 0) {
                        jobViewModel.clearUniversityFilter()
                    } else {
                        jobViewModel.setSelectedUniversity(it.id)
                    }
                }
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        
        // Observe job loading state
        jobViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe job data
        jobViewModel.jobs.observe(viewLifecycleOwner) { jobs ->
            jobAdapter.submitList(jobs)
        }
        
        // Observe job errors
        jobViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        
        // Observe university loading state
        universityViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                progressBar.visibility = View.VISIBLE
            }
        }
        
        // Observe university errors
        universityViewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
            }
        }
        
        // Set up search functionality
        searchIcon.setOnClickListener {
            val query = searchEditText.text.toString().trim()
            if (query.isNotEmpty()) {
                jobViewModel.searchJobs(query)
            } else {
                jobViewModel.loadJobs()
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
        universityViewModel.loadUniversities()
        jobViewModel.loadJobs()
    }
} 