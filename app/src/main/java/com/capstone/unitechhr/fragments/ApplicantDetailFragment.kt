package com.capstone.unitechhr.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.ApplicationStatus
import com.capstone.unitechhr.utils.ApplicantNotificationManager
import com.capstone.unitechhr.viewmodels.ApplicantViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import java.text.SimpleDateFormat
import java.util.Locale

class ApplicantDetailFragment : Fragment() {
    
    private lateinit var viewModel: ApplicantViewModel
    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    // Permission request launcher
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted", Toast.LENGTH_SHORT).show()
            // Re-initialize FCM with permission granted
            viewModel.selectedApplicant.value?.let { 
                ApplicantNotificationManager.initializeApplicantFcm(requireContext(), it)
            }
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_applicant_detail, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        viewModel = ViewModelProvider(requireActivity())[ApplicantViewModel::class.java]
        
        // Set up toolbar
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Applicant Details"
            setDisplayHomeAsUpEnabled(true)
        }
        
        // Request notification permission if needed
        requestNotificationPermissionIfNeeded()
        
        viewModel.selectedApplicant.observe(viewLifecycleOwner) { applicant ->
            if (applicant == null) {
                findNavController().navigateUp()
                return@observe
            }
            
            // Initialize FCM for this applicant
            ApplicantNotificationManager.initializeApplicantFcm(requireContext(), applicant)
            
            // Populate views with applicant data
            view.findViewById<TextView>(R.id.applicant_name).text = 
                "${applicant.firstName} ${applicant.lastName}"
            view.findViewById<TextView>(R.id.applicant_position).text = applicant.appliedPosition
            view.findViewById<TextView>(R.id.applicant_email).text = applicant.email
            view.findViewById<TextView>(R.id.applicant_phone).text = applicant.phone
            view.findViewById<TextView>(R.id.application_date).text = 
                "Applied on: ${dateFormatter.format(applicant.applicationDate)}"
            view.findViewById<TextView>(R.id.application_status).text = 
                "Status: ${applicant.status.toString().replace("_", " ")}"
            
            // Set status color
            val statusColor = when (applicant.status) {
                ApplicationStatus.PENDING -> R.color.status_pending
                ApplicationStatus.REVIEWING -> R.color.status_reviewing
                ApplicationStatus.INTERVIEW_SCHEDULED -> R.color.status_interview
                ApplicationStatus.INTERVIEW_COMPLETED -> R.color.status_interview
                ApplicationStatus.HIRED -> R.color.status_hired
                ApplicationStatus.REJECTED -> R.color.status_rejected
            }
            view.findViewById<TextView>(R.id.application_status).setTextColor(
                requireContext().getColor(statusColor)
            )
            
            // Populate skills
            val skillsChipGroup = view.findViewById<ChipGroup>(R.id.skills_chip_group)
            skillsChipGroup.removeAllViews()
            
            if (applicant.skills.isEmpty()) {
                view.findViewById<TextView>(R.id.no_skills_text).visibility = View.VISIBLE
                skillsChipGroup.visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.no_skills_text).visibility = View.GONE
                skillsChipGroup.visibility = View.VISIBLE
                
                for (skill in applicant.skills) {
                    val chip = Chip(requireContext())
                    chip.text = skill
                    skillsChipGroup.addView(chip)
                }
            }
            
            // Setup education and experience sections
            setupEducationSection(view, applicant.education.isEmpty())
            setupExperienceSection(view, applicant.experience.isEmpty())
            
            // Setup notification switch
            setupNotificationSwitch(view, applicant.id, applicant.notificationsEnabled)
            
            // Setup buttons
            setupActionButtons(view, applicant.status)
        }
        
        // Handle edit button click
        view.findViewById<Button>(R.id.edit_button).setOnClickListener {
            findNavController().navigate(R.id.action_applicantDetailFragment_to_applicantFormFragment)
        }
    }
    
    private fun setupNotificationSwitch(view: View, applicantId: String, notificationsEnabled: Boolean) {
        // Find or create the notification switch
        val notificationSwitch = view.findViewById<Switch>(R.id.notification_switch)
        notificationSwitch?.let {
            // Set initial state
            it.isChecked = notificationsEnabled
            
            // Add change listener
            it.setOnCheckedChangeListener { _, isChecked ->
                // Update notification preferences
                ApplicantNotificationManager.setApplicantNotificationsEnabled(applicantId, isChecked)
                
                // Show toast feedback
                val message = if (isChecked) "Notifications enabled" else "Notifications disabled"
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Show explanation before requesting permission
                    AlertDialog.Builder(requireContext())
                        .setTitle("Notification Permission")
                        .setMessage("To receive updates about your application status, please allow notifications.")
                        .setPositiveButton("OK") { _, _ ->
                            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
                else -> {
                    // Request permission directly
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }
    
    private fun setupEducationSection(view: View, isEmpty: Boolean) {
        val educationSection = view.findViewById<View>(R.id.education_section)
        val noEducationText = view.findViewById<TextView>(R.id.no_education_text)
        
        if (isEmpty) {
            educationSection.visibility = View.GONE
            noEducationText.visibility = View.VISIBLE
        } else {
            educationSection.visibility = View.VISIBLE
            noEducationText.visibility = View.GONE
            
            // TODO: Populate education details
        }
    }
    
    private fun setupExperienceSection(view: View, isEmpty: Boolean) {
        val experienceSection = view.findViewById<View>(R.id.experience_section)
        val noExperienceText = view.findViewById<TextView>(R.id.no_experience_text)
        
        if (isEmpty) {
            experienceSection.visibility = View.GONE
            noExperienceText.visibility = View.VISIBLE
        } else {
            experienceSection.visibility = View.VISIBLE
            noExperienceText.visibility = View.GONE
            
            // TODO: Populate experience details
        }
    }
    
    private fun setupActionButtons(view: View, currentStatus: ApplicationStatus) {
        val statusActionsLayout = view.findViewById<ViewGroup>(R.id.status_actions_layout)
        statusActionsLayout.removeAllViews()
        
        when (currentStatus) {
            ApplicationStatus.PENDING -> {
                addActionButton(statusActionsLayout, "Start Review", ApplicationStatus.REVIEWING)
                addActionButton(statusActionsLayout, "Reject", ApplicationStatus.REJECTED)
            }
            ApplicationStatus.REVIEWING -> {
                addActionButton(statusActionsLayout, "Schedule Interview", ApplicationStatus.INTERVIEW_SCHEDULED)
                addActionButton(statusActionsLayout, "Reject", ApplicationStatus.REJECTED)
            }
            ApplicationStatus.INTERVIEW_SCHEDULED -> {
                addActionButton(statusActionsLayout, "Hire", ApplicationStatus.HIRED)
                addActionButton(statusActionsLayout, "Reject", ApplicationStatus.REJECTED)
            }
            ApplicationStatus.INTERVIEW_COMPLETED -> {
                addActionButton(statusActionsLayout, "Hire", ApplicationStatus.HIRED)
                addActionButton(statusActionsLayout, "Reject", ApplicationStatus.REJECTED)
            }
            ApplicationStatus.HIRED, ApplicationStatus.REJECTED -> {
                // No actions for final statuses
            }
        }
    }
    
    private fun addActionButton(container: ViewGroup, text: String, newStatus: ApplicationStatus) {
        val button = Button(requireContext())
        button.text = text
        
        val layoutParams = ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        layoutParams.marginEnd = resources.getDimensionPixelSize(R.dimen.margin_medium)
        button.layoutParams = layoutParams
        
        button.setOnClickListener {
            viewModel.selectedApplicant.value?.let { applicant ->
                viewModel.updateApplicantStatus(applicant, newStatus)
            }
        }
        
        container.addView(button)
    }
    
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.applicant_detail_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                true
            }
            android.R.id.home -> {
                findNavController().navigateUp()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Applicant")
            .setMessage("Are you sure you want to delete this applicant? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.selectedApplicant.value?.let { applicant ->
                    // Unsubscribe from topics before deleting
                    ApplicantNotificationManager.unsubscribeApplicantFromTopics(applicant)
                    viewModel.deleteApplicant(applicant.id)
                    findNavController().navigateUp()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
} 