package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
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
        
        // Setup navigation cards
        setupNavigationCard(
            view.findViewById(R.id.jobsCard),
            "Find and apply for job opportunities",
            R.id.jobListingFragment
        )
        
        setupNavigationCard(
            view.findViewById(R.id.interviewsCard),
            "View and manage your interview schedule",
            R.id.interviewScheduleFragment
        )
        
        setupNavigationCard(
            view.findViewById(R.id.onboardingCard),
            "Complete your onboarding tasks",
            R.id.onboardingChecklistFragment
        )
        
        setupNavigationCard(
            view.findViewById(R.id.applicationsCard),
            "Track your application status",
            R.id.applicantListFragment
        )
        
        setupNavigationCard(
            view.findViewById(R.id.profileCard),
            "Update your profile information",
            R.id.profileFragment
        )
    }
    
    private fun setupNavigationCard(card: CardView, description: String, destinationId: Int) {
        // Set description text
        card.findViewById<TextView>(R.id.cardDescription).text = description
        
        // Set click listener
        card.setOnClickListener {
            findNavController().navigate(destinationId)
        }
    }
} 