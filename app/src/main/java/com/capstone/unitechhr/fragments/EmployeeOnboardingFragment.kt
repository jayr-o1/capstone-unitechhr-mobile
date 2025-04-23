package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.adapters.OnboardingChecklistAdapter
import com.capstone.unitechhr.models.OnboardingChecklistTask
import com.capstone.unitechhr.viewmodels.EmployeeOnboardingViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EmployeeOnboardingFragment : Fragment() {
    
    private val viewModel: EmployeeOnboardingViewModel by viewModels()
    private lateinit var adapter: OnboardingChecklistAdapter
    private val TAG = "EmployeeOnboardingFrag"
    
    // UI components
    private lateinit var backButton: ImageView
    private lateinit var employeeNameTextView: TextView
    private lateinit var employeePositionTextView: TextView
    private lateinit var onboardingStatusTextView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressPercentTextView: TextView
    private lateinit var startDateTextView: TextView
    private lateinit var onboardingTasksRecyclerView: RecyclerView
    private lateinit var completeOnboardingButton: Button
    private lateinit var loadingOverlay: FrameLayout
    
    // Data
    private var employeeId: String = ""
    private var collectionPath: String = ""
    private var employeeName: String = ""
    private var employeePosition: String = ""
    private var startDate: Date = Date()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get arguments
        arguments?.let {
            employeeId = it.getString("employeeId", "")
            employeeName = it.getString("employeeName", "")
            employeePosition = it.getString("employeePosition", "")
            collectionPath = it.getString("collectionPath", "")
            
            Log.d(TAG, "Received collection path: $collectionPath")
            
            // Parse start date if provided
            it.getString("startDate")?.let { dateString ->
                try {
                    startDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).parse(dateString) ?: Date()
                } catch (e: Exception) {
                    startDate = Date()
                }
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_employee_onboarding, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        backButton = view.findViewById(R.id.backButton)
        employeeNameTextView = view.findViewById(R.id.employeeNameTextView)
        employeePositionTextView = view.findViewById(R.id.employeePositionTextView)
        onboardingStatusTextView = view.findViewById(R.id.onboardingStatusTextView)
        progressBar = view.findViewById(R.id.progressBar)
        progressPercentTextView = view.findViewById(R.id.progressPercentTextView)
        startDateTextView = view.findViewById(R.id.startDateTextView)
        onboardingTasksRecyclerView = view.findViewById(R.id.onboardingTasksRecyclerView)
        completeOnboardingButton = view.findViewById(R.id.completeOnboardingButton)
        loadingOverlay = view.findViewById(R.id.loadingOverlay)
        
        // Set up back button
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Set employee info
        employeeNameTextView.text = employeeName
        employeePositionTextView.text = employeePosition
        onboardingStatusTextView.text = "In Progress"
        startDateTextView.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(startDate)
        
        // Setup RecyclerView with a dummy listener (since checkboxes are now disabled)
        adapter = OnboardingChecklistAdapter { _, _ -> 
            // No-op: employees cannot update task status
        }
        
        onboardingTasksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        onboardingTasksRecyclerView.adapter = adapter
        
        // Hide the complete onboarding button since employees shouldn't be able to complete onboarding
        completeOnboardingButton.visibility = View.GONE
        
        // Observe ViewModel data
        viewModel.tasks.observe(viewLifecycleOwner) { tasks ->
            Log.d(TAG, "Received ${tasks.size} tasks from ViewModel")
            if (tasks.isEmpty()) {
                showToast("No onboarding tasks found. Please contact HR.")
            } else {
                adapter.submitList(tasks)
            }
        }
        
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            loadingOverlay.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            message?.let {
                Log.e(TAG, "Error: $it")
                
                // Check if this is a document not found error
                if (it.contains("Document not found") || it.contains("not found")) {
                    showToast("Onboarding not set up yet. Please contact HR to set up your onboarding.")
                } else {
                    showToast(it)
                }
            }
        }
        
        viewModel.progress.observe(viewLifecycleOwner) { progressPercent ->
            progressBar.progress = progressPercent
            progressPercentTextView.text = "$progressPercent% complete"
        }
        
        // Load onboarding tasks
        Log.d(TAG, "Loading onboarding tasks for path: $collectionPath")
        viewModel.loadOnboardingTasks(employeeId, collectionPath)
    }
    
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }
    
    companion object {
        @JvmStatic
        fun newInstance(
            employeeId: String,
            employeeName: String,
            employeePosition: String,
            collectionPath: String,
            startDate: String
        ) = EmployeeOnboardingFragment().apply {
            arguments = Bundle().apply {
                putString("employeeId", employeeId)
                putString("employeeName", employeeName)
                putString("employeePosition", employeePosition)
                putString("collectionPath", collectionPath)
                putString("startDate", startDate)
            }
        }
    }
} 