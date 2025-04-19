package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Setup notification icon
        val notificationIcon = view.findViewById<ImageView>(R.id.notificationIcon)
        notificationIcon.setOnClickListener {
            Toast.makeText(context, "Notifications", Toast.LENGTH_SHORT).show()
            // You can navigate to notifications screen if needed
            // findNavController().navigate(R.id.notificationsFragment)
        }
        
        // Setup navigation cards
        setupInterviewCard(
            view.findViewById(R.id.interviewsCard),
            "View and manage your interview schedule",
            R.id.interviewScheduleFragment
        )
        
        setupOnboardingCard(
            view.findViewById(R.id.onboardingCard),
            "Complete your onboarding tasks",
            R.id.onboardingChecklistFragment
        )
        
        setupApplicationCard(
            view.findViewById(R.id.applicationsCard),
            "Track your application status",
            R.id.applicantListFragment
        )
    }
    
    private fun setupInterviewCard(card: CardView, description: String, destinationId: Int) {
        // Set description text
        card.findViewById<TextView>(R.id.interviewCardDescription).text = description
        
        // Set click listener
        card.setOnClickListener {
            findNavController().navigate(destinationId)
        }
    }
    
    private fun setupOnboardingCard(card: CardView, description: String, destinationId: Int) {
        // Set description text
        card.findViewById<TextView>(R.id.onboardingCardDescription).text = description
        
        // Set click listener
        card.setOnClickListener {
            findNavController().navigate(destinationId)
        }
    }
    
    private fun setupApplicationCard(card: CardView, description: String, destinationId: Int) {
        // Set description text
        card.findViewById<TextView>(R.id.applicationCardDescription).text = description
        
        // Set click listener
        card.setOnClickListener {
            findNavController().navigate(destinationId)
        }
    }
} 