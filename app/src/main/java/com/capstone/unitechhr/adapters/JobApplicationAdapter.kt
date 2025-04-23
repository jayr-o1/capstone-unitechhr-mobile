package com.capstone.unitechhr.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.JobApplication
import com.capstone.unitechhr.models.JobApplicationStatus
import com.capstone.unitechhr.models.formatForDisplay
import com.capstone.unitechhr.models.getColorResourceId
import java.text.SimpleDateFormat
import java.util.Locale

class JobApplicationAdapter(
    private val onApplicationClick: (JobApplication) -> Unit
) : ListAdapter<JobApplication, JobApplicationAdapter.ApplicationViewHolder>(ApplicationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job_application, parent, false)
        return ApplicationViewHolder(view, onApplicationClick)
    }

    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ApplicationViewHolder(
        itemView: View,
        private val onApplicationClick: (JobApplication) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val jobTitleTextView: TextView = itemView.findViewById(R.id.jobTitleTextView)
        private val companyTextView: TextView = itemView.findViewById(R.id.companyTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(application: JobApplication) {
            jobTitleTextView.text = application.jobTitle
            companyTextView.text = application.company
            
            // Hide the location TextView instead of setting text
            locationTextView.visibility = View.GONE
            
            // Debug log the application status
            Log.d("JobApplicationAdapter", "Application status enum: ${application.status}")
            Log.d("JobApplicationAdapter", "Application raw status: ${application.rawStatus}")
            
            // Determine what status text to display - prioritize the raw status if available
            val statusDisplay = when {
                // If we have the raw status from Firestore, use it directly
                !application.rawStatus.isNullOrEmpty() -> {
                    Log.d("JobApplicationAdapter", "Using raw status from Firestore: ${application.rawStatus}")
                    application.rawStatus
                }
                // Otherwise use the enum's formatted display
                application.status != null -> {
                    val formatted = application.status.formatForDisplay()
                    Log.d("JobApplicationAdapter", "Using formatted status from enum: $formatted")
                    formatted
                }
                // As a last resort, use a default value
                else -> {
                    Log.d("JobApplicationAdapter", "Using default status")
                    "Pending"
                }
            }
            
            Log.d("JobApplicationAdapter", "Final status for display: $statusDisplay")
            statusTextView.text = statusDisplay
            
            // Set status color based on the displayed status
            val colorResId = when {
                application.rawStatus?.contains("Onboarding", ignoreCase = true) == true -> 
                    com.capstone.unitechhr.R.color.status_hired
                application.status == JobApplicationStatus.IN_ONBOARDING -> 
                    com.capstone.unitechhr.R.color.status_hired
                application.status != null -> 
                    application.status.getColorResourceId()
                else -> 
                    com.capstone.unitechhr.R.color.status_pending
            }
            
            statusTextView.setTextColor(itemView.context.getColor(colorResId))
            
            // Set application date
            dateTextView.text = "Applied: ${dateFormatter.format(application.appliedDate)}"
            
            // Set click listener
            itemView.setOnClickListener {
                onApplicationClick(application)
            }
        }
    }

    class ApplicationDiffCallback : DiffUtil.ItemCallback<JobApplication>() {
        override fun areItemsTheSame(oldItem: JobApplication, newItem: JobApplication): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: JobApplication, newItem: JobApplication): Boolean {
            return oldItem == newItem
        }
    }
} 