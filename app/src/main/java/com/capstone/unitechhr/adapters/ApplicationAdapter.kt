package com.capstone.unitechhr.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.Application
import com.capstone.unitechhr.models.ApplicationStatus
import com.google.android.material.chip.Chip
import java.text.SimpleDateFormat
import java.util.Locale

class ApplicationAdapter(
    private val onItemClick: (Application) -> Unit
) : ListAdapter<Application, ApplicationAdapter.ApplicationViewHolder>(ApplicationDiffCallback()) {
    
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_applicant, parent, false)
        return ApplicationViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ApplicationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    inner class ApplicationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.applicant_name)
        private val positionTextView: TextView = itemView.findViewById(R.id.applicant_position)
        private val dateTextView: TextView = itemView.findViewById(R.id.applicant_date)
        private val statusChip: Chip = itemView.findViewById(R.id.applicant_status)
        
        fun bind(application: Application) {
            nameTextView.text = application.companyName
            positionTextView.text = application.jobTitle
            dateTextView.text = "Applied: ${dateFormat.format(application.applicationDate)}"
            
            // Set status chip style based on application status
            statusChip.text = application.status.toString().replace("_", " ")
            
            // Set chip color based on application status
            val status = application.status.toString()
            when (application.status) {
                ApplicationStatus.PENDING -> {
                    statusChip.setChipBackgroundColorResource(R.color.status_pending)
                }
                ApplicationStatus.REVIEWING -> {
                    statusChip.setChipBackgroundColorResource(R.color.status_reviewing)
                }
                ApplicationStatus.INTERVIEW_SCHEDULED -> {
                    statusChip.setChipBackgroundColorResource(R.color.status_interview)
                }
                else -> {
                    // Handle other status values
                    when (status) {
                        "INTERVIEW" -> statusChip.setChipBackgroundColorResource(R.color.status_interview)
                        "HIRED" -> statusChip.setChipBackgroundColorResource(R.color.status_hired)
                        "REJECTED" -> statusChip.setChipBackgroundColorResource(R.color.status_rejected)
                        else -> statusChip.setChipBackgroundColorResource(R.color.status_pending)
                    }
                }
            }
            
            // Set click listener
            itemView.setOnClickListener {
                onItemClick(application)
            }
        }
    }
    
    class ApplicationDiffCallback : DiffUtil.ItemCallback<Application>() {
        override fun areItemsTheSame(oldItem: Application, newItem: Application): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Application, newItem: Application): Boolean {
            return oldItem == newItem
        }
    }
} 