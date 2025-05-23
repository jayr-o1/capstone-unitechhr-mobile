package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.Notification
import com.capstone.unitechhr.models.NotificationType
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.capstone.unitechhr.viewmodels.NotificationViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeFragment : Fragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    
    private lateinit var notificationIcon: ImageView
    private lateinit var notificationBadge: View
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup notification icon and badge
        notificationIcon = view.findViewById(R.id.notificationIcon)
        notificationBadge = view.findViewById(R.id.notificationBadge)
        
        notificationIcon.setOnClickListener {
            navigateToNotifications()
        }
        
        // Setup welcome message with user's name
        val welcomeText = view.findViewById<TextView>(R.id.welcomeText)
        
        authViewModel.currentUser.observe(viewLifecycleOwner) { userData ->
            userData?.let {
                // Get the first name only
                val firstName = it.displayName.split(" ").firstOrNull() ?: "User"
                welcomeText.text = "Welcome, $firstName"
                
                // Now that we have the user, check for notifications
                checkForNotifications(it.email)
            }
        }
        
        // Observe notification status
        notificationViewModel.hasUnreadNotifications.observe(viewLifecycleOwner) { hasUnread ->
            notificationBadge.visibility = if (hasUnread) View.VISIBLE else View.GONE
        }
        
        // Setup navigation cards
        setupInterviewCard(
            view.findViewById(R.id.interviewsCard),
            "View your upcoming interview schedule",
            R.id.action_homeFragment_to_interviewListFragment
        )
        
        // Setup the onboarding card to navigate to the employee onboarding screen directly
        val onboardingCard = view.findViewById<CardView>(R.id.onboardingCard)
        onboardingCard.findViewById<TextView>(R.id.onboardingCardDescription).text = 
            "Complete your onboarding checklist"
        
        onboardingCard.setOnClickListener {
            // For employees, go directly to the onboarding checklist
            val userData = authViewModel.currentUser.value
            userData?.let {
                val employeeName = it.displayName
                val position = it.jobTitle ?: "New Employee"
                val email = it.email
                
                // Create sanitized user ID for path
                val sanitizedEmail = email.replace("@", "-").replace(".", "-")
                
                // Dynamically create the path - try to use the same pattern as job list page
                // First attempt with university and job IDs from user data
                val universityId = it.universityId
                val jobId = it.jobId
                
                // Construct path dynamically based on available information
                val collectionPath = if (!universityId.isNullOrEmpty() && !jobId.isNullOrEmpty()) {
                    // If we have both university and job IDs, use them
                    "universities/$universityId/jobs/$jobId/applicants/$sanitizedEmail"
                } else {
                    // Otherwise, try to use the university_322305 pattern (for compatibility with existing data)
                    "universities/university_322305/jobs/bSpb3DxJCw6FbRKj58KT/applicants/$sanitizedEmail"
                }
                
                Log.d("HomeFragment", "Navigating to onboarding screen with path: $collectionPath")
                
                // Get current date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                val currentDate = dateFormat.format(Date())
                
                // Navigate to the employee onboarding fragment
                val bundle = Bundle().apply {
                    putString("employeeId", email)
                    putString("employeeName", employeeName)
                    putString("employeePosition", position)
                    putString("collectionPath", collectionPath)
                    putString("startDate", currentDate)
                }
                
                findNavController().navigate(R.id.action_homeFragment_to_employeeOnboardingFragment, bundle)
            }
        }
        
        setupApplicationCard(
            view.findViewById(R.id.applicationsCard),
            "Track your job application status",
            R.id.action_homeFragment_to_myApplicationsFragment
        )
    }
    
    override fun onResume() {
        super.onResume()
        // Check for notifications again when fragment resumes
        authViewModel.currentUser.value?.let {
            checkForNotifications(it.email)
        }
    }
    
    private fun checkForNotifications(email: String) {
        // Convert email to applicant ID format (same as in AuthRepository.emailToCollectionId)
        val applicantId = email.replace("@", "-").replace(".", "-")
        Log.d("HomeFragment", "Checking for notifications for applicantId: $applicantId")
        notificationViewModel.checkUnreadNotifications(applicantId)
    }
    
    private fun navigateToNotifications() {
        // Navigate to the notification list fragment
        findNavController().navigate(R.id.action_homeFragment_to_notificationListFragment)
    }
    
    private fun setupInterviewCard(card: CardView, description: String, actionId: Int) {
        // Set description text
        card.findViewById<TextView>(R.id.interviewCardDescription).text = description
        
        // Set click listener
        card.setOnClickListener {
            safeNavigate(actionId)
        }
    }
    
    private fun setupApplicationCard(card: CardView, description: String, actionId: Int) {
        // Set description text
        card.findViewById<TextView>(R.id.applicationCardDescription).text = description
        
        // Set click listener
        card.setOnClickListener {
            Log.d("HomeFragment", "Navigating to My Applications screen")
            safeNavigate(R.id.action_homeFragment_to_myApplicationsFragment)
        }
    }
    
    private fun safeNavigate(actionId: Int) {
        try {
            // Navigate without custom animations
            findNavController().navigate(actionId)
        } catch (e: Exception) {
            // Handle navigation errors
            Toast.makeText(context, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
} 