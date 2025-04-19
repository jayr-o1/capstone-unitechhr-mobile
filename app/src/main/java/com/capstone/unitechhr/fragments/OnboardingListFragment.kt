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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.adapters.OnboardingAdapter
import com.capstone.unitechhr.models.OnboardingStatus
import com.capstone.unitechhr.viewmodels.OnboardingViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout

class OnboardingListFragment : Fragment() {
    
    private val viewModel: OnboardingViewModel by viewModels()
    private lateinit var adapter: OnboardingAdapter
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyText: TextView
    private lateinit var addOnboardingFab: FloatingActionButton
    private lateinit var tabLayout: TabLayout
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_onboarding_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        recyclerView = view.findViewById(R.id.onboardingRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
        addOnboardingFab = view.findViewById(R.id.addOnboardingFab)
        tabLayout = view.findViewById(R.id.tabLayout)
        
        // Setup RecyclerView adapter
        adapter = OnboardingAdapter { onboarding ->
            viewModel.selectOnboarding(onboarding)
            findNavController().navigate(R.id.action_onboardingListFragment_to_onboardingDetailFragment)
        }
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        
        // Setup tab layout listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                filterOnboarding(tab.position)
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            
            override fun onTabReselected(tab: TabLayout.Tab) {
                filterOnboarding(tab.position)
            }
        })
        
        // Setup FAB click listener
        addOnboardingFab.setOnClickListener {
            findNavController().navigate(R.id.action_onboardingListFragment_to_createOnboardingFragment)
        }
        
        // Observe ViewModel data
        viewModel.onboardingProcesses.observe(viewLifecycleOwner) { processes ->
            if (processes.isEmpty()) {
                emptyText.visibility = View.VISIBLE
            } else {
                emptyText.visibility = View.GONE
            }
            
            val currentTabPosition = tabLayout.selectedTabPosition
            filterOnboarding(currentTabPosition)
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }
        
        viewModel.operationSuccessful.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                Toast.makeText(requireContext(), "Operation successful", Toast.LENGTH_SHORT).show()
                viewModel.clearOperationStatus()
            } else if (success == false) {
                Toast.makeText(requireContext(), "Operation failed", Toast.LENGTH_SHORT).show()
                viewModel.clearOperationStatus()
            }
        }
        
        // Load onboarding processes
        loadOnboardingProcesses()
    }
    
    override fun onResume() {
        super.onResume()
        loadOnboardingProcesses()
    }
    
    private fun loadOnboardingProcesses() {
        viewModel.loadOnboardingProcesses()
    }
    
    private fun filterOnboarding(tabPosition: Int) {
        val processes = viewModel.onboardingProcesses.value ?: emptyList()
        
        val filteredProcesses = when (tabPosition) {
            0 -> processes.filter { 
                it.status == OnboardingStatus.NOT_STARTED || 
                it.status == OnboardingStatus.IN_PROGRESS || 
                it.status == OnboardingStatus.EXTENDED 
            }
            1 -> processes.filter { it.status == OnboardingStatus.COMPLETED }
            else -> processes
        }
        
        if (filteredProcesses.isEmpty()) {
            emptyText.visibility = View.VISIBLE
        } else {
            emptyText.visibility = View.GONE
        }
        
        adapter.submitList(filteredProcesses)
    }
} 