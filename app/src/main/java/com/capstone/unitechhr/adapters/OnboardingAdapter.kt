package com.capstone.unitechhr.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.Onboarding
import com.capstone.unitechhr.models.OnboardingStatus
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class OnboardingAdapter(
    private val onViewDetailsClick: (Onboarding) -> Unit
) : ListAdapter<Onboarding, OnboardingAdapter.OnboardingViewHolder>(OnboardingDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding, parent, false)
        return OnboardingViewHolder(view, onViewDetailsClick)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OnboardingViewHolder(
        itemView: View,
        private val onViewDetailsClick: (Onboarding) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val employeeNameTextView: TextView = itemView.findViewById(R.id.employeeNameTextView)
        private val statusTextView: TextView = itemView.findViewById(R.id.statusTextView)
        private val startDateTextView: TextView = itemView.findViewById(R.id.startDateTextView)
        private val deadlineTextView: TextView = itemView.findViewById(R.id.deadlineTextView)
        private val mentorTextView: TextView = itemView.findViewById(R.id.mentorTextView)
        private val taskProgressBar: ProgressBar = itemView.findViewById(R.id.taskProgressBar)
        private val progressTextView: TextView = itemView.findViewById(R.id.progressTextView)
        private val viewDetailsButton: MaterialButton = itemView.findViewById(R.id.viewDetailsButton)
        
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        fun bind(onboarding: Onboarding) {
            // Set employee name (in a real app, this would come from a user/employee object)
            employeeNameTextView.text = onboarding.employeeId
            
            // Set status with appropriate color
            statusTextView.text = onboarding.status.toString().replace("_", " ")
            val statusColor = when (onboarding.status) {
                OnboardingStatus.NOT_STARTED -> R.color.status_pending
                OnboardingStatus.IN_PROGRESS -> R.color.status_interview
                OnboardingStatus.COMPLETED -> R.color.status_hired
                OnboardingStatus.EXTENDED -> R.color.status_reviewing
            }
            statusTextView.setTextColor(itemView.context.getColor(statusColor))
            
            // Set dates
            startDateTextView.text = dateFormatter.format(onboarding.startDate)
            deadlineTextView.text = onboarding.completionDeadline?.let { 
                dateFormatter.format(it)
            } ?: "Not set"
            
            // Set mentor
            mentorTextView.text = onboarding.mentor
            
            // Calculate and set progress
            val completedTasks = onboarding.tasks.count { it.isCompleted }
            val totalTasks = onboarding.tasks.size
            
            if (totalTasks > 0) {
                val progressPercentage = (completedTasks * 100) / totalTasks
                taskProgressBar.progress = progressPercentage
                progressTextView.text = "$completedTasks/$totalTasks tasks completed"
            } else {
                taskProgressBar.progress = 0
                progressTextView.text = "No tasks assigned"
            }
            
            // Set click listener for details button
            viewDetailsButton.setOnClickListener {
                onViewDetailsClick(onboarding)
            }
        }
    }
    
    class OnboardingDiffCallback : DiffUtil.ItemCallback<Onboarding>() {
        override fun areItemsTheSame(oldItem: Onboarding, newItem: Onboarding): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Onboarding, newItem: Onboarding): Boolean {
            return oldItem == newItem
        }
    }
} 