package com.capstone.unitechhr.adapters

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.Applicant
import com.capstone.unitechhr.models.Interview
import com.capstone.unitechhr.models.InterviewStatus
import com.capstone.unitechhr.models.Job
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Locale

class InterviewAdapter(
    private val onViewDetailsClick: (Interview) -> Unit,
    private val onRescheduleClick: (Interview) -> Unit,
    private val onCompleteClick: (Interview) -> Unit
) : ListAdapter<InterviewWithDetails, InterviewAdapter.InterviewViewHolder>(InterviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InterviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_interview, parent, false)
        return InterviewViewHolder(view, onViewDetailsClick, onRescheduleClick, onCompleteClick)
    }

    override fun onBindViewHolder(holder: InterviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InterviewViewHolder(
        itemView: View,
        private val onViewDetailsClick: (Interview) -> Unit,
        private val onRescheduleClick: (Interview) -> Unit,
        private val onCompleteClick: (Interview) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val interviewDateTextView: TextView = itemView.findViewById(R.id.interviewDateTextView)
        private val interviewStatusTextView: TextView = itemView.findViewById(R.id.interviewStatusTextView)
        private val interviewDurationTextView: TextView = itemView.findViewById(R.id.interviewDurationTextView)
        private val applicantNameTextView: TextView = itemView.findViewById(R.id.applicantNameTextView)
        private val jobTitleTextView: TextView = itemView.findViewById(R.id.jobTitleTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val viewDetailsButton: MaterialButton = itemView.findViewById(R.id.viewDetailsButton)
        private val rescheduleButton: MaterialButton = itemView.findViewById(R.id.rescheduleButton)
        private val completeButton: MaterialButton = itemView.findViewById(R.id.completeButton)
        
        private val dateTimeFormatter = SimpleDateFormat("MMM dd, yyyy - h:mm a", Locale.getDefault())
        
        fun bind(interviewWithDetails: InterviewWithDetails) {
            val interview = interviewWithDetails.interview
            val applicant = interviewWithDetails.applicant
            val job = interviewWithDetails.job
            
            // Format date and time
            interviewDateTextView.text = dateTimeFormatter.format(interview.scheduledDate)
            
            // Set status with appropriate color
            interviewStatusTextView.text = interview.status.toString()
            val statusColor = when (interview.status) {
                InterviewStatus.SCHEDULED -> R.color.status_pending
                InterviewStatus.COMPLETED -> R.color.status_hired
                InterviewStatus.CANCELLED -> R.color.status_rejected
                InterviewStatus.RESCHEDULED -> R.color.status_interview
            }
            interviewStatusTextView.setTextColor(itemView.context.getColor(statusColor))
            
            // Set duration
            interviewDurationTextView.text = "${interview.duration} min"
            
            // Set applicant name
            applicantNameTextView.text = if (applicant != null) {
                "${applicant.firstName} ${applicant.lastName}"
            } else {
                "Unknown Applicant"
            }
            
            // Set job title
            jobTitleTextView.text = job?.title ?: "Unknown Position"
            
            // Set location
            locationTextView.text = "Location: ${interview.location}"
            
            // Configure buttons based on status
            when (interview.status) {
                InterviewStatus.SCHEDULED -> {
                    rescheduleButton.visibility = View.VISIBLE
                    completeButton.visibility = View.VISIBLE
                }
                InterviewStatus.COMPLETED, InterviewStatus.CANCELLED -> {
                    rescheduleButton.visibility = View.GONE
                    completeButton.visibility = View.GONE
                }
                InterviewStatus.RESCHEDULED -> {
                    rescheduleButton.visibility = View.VISIBLE
                    completeButton.visibility = View.VISIBLE
                }
            }
            
            // Set click listeners
            viewDetailsButton.setOnClickListener { onViewDetailsClick(interview) }
            rescheduleButton.setOnClickListener { onRescheduleClick(interview) }
            completeButton.setOnClickListener { onCompleteClick(interview) }
        }
    }
    
    class InterviewDiffCallback : DiffUtil.ItemCallback<InterviewWithDetails>() {
        override fun areItemsTheSame(oldItem: InterviewWithDetails, newItem: InterviewWithDetails): Boolean {
            return oldItem.interview.id == newItem.interview.id
        }
        
        override fun areContentsTheSame(oldItem: InterviewWithDetails, newItem: InterviewWithDetails): Boolean {
            return oldItem == newItem
        }
    }
}

data class InterviewWithDetails(
    val interview: Interview,
    val applicant: Applicant? = null,
    val job: Job? = null
) 