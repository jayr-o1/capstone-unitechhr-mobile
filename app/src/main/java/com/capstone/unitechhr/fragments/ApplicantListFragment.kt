package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.adapters.ApplicantAdapter
import com.capstone.unitechhr.models.Applicant
import com.capstone.unitechhr.viewmodels.ApplicantViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.snackbar.Snackbar

class ApplicantListFragment : Fragment() {
    
    private lateinit var viewModel: ApplicantViewModel
    private lateinit var adapter: ApplicantAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var progressIndicator: CircularProgressIndicator
    private lateinit var noDataView: View
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_applicant_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        recyclerView = view.findViewById(R.id.applicants_recycler_view)
        searchView = view.findViewById(R.id.search_view)
        progressIndicator = view.findViewById(R.id.progress_indicator)
        noDataView = view.findViewById(R.id.no_data_view)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[ApplicantViewModel::class.java]
        
        // Setup RecyclerView
        setupRecyclerView()
        
        // Setup search
        setupSearch()
        
        // Setup FAB for adding new applicants
        view.findViewById<FloatingActionButton>(R.id.fab_add_applicant).setOnClickListener {
            navigateToAddApplicant()
        }
        
        // Observe applicants data
        viewModel.applicants.observe(viewLifecycleOwner) { applicants ->
            adapter.submitList(applicants)
            updateEmptyState(applicants.isEmpty())
        }
        
        // Observe loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Observe error messages
        viewModel.errorMessage.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(view, it, Snackbar.LENGTH_LONG).show()
            }
        }
        
        // Load applicants
        viewModel.loadApplicants()
    }
    
    private fun setupRecyclerView() {
        adapter = ApplicantAdapter { applicant ->
            onApplicantClick(applicant)
        }
        
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }
    
    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { viewModel.searchApplicants(it) }
                return true
            }
            
            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrEmpty()) {
                    viewModel.loadApplicants()
                }
                return true
            }
        })
    }
    
    private fun onApplicantClick(applicant: Applicant) {
        viewModel.selectApplicant(applicant)
        findNavController().navigate(R.id.action_applicantListFragment_to_applicantDetailFragment)
    }
    
    private fun navigateToAddApplicant() {
        viewModel.clearSelectedApplicant()
        findNavController().navigate(R.id.action_applicantListFragment_to_applicantFormFragment)
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
} 