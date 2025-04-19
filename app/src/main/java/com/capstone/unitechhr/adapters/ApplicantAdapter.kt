package com.capstone.unitechhr.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.Applicant
import java.text.SimpleDateFormat
import java.util.Locale

class ApplicantAdapter(private val onItemClick: (Applicant) -> Unit) : 
    ListAdapter<Applicant, ApplicantAdapter.ApplicantViewHolder>(ApplicantDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApplicantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_applicant, parent, false)
        return ApplicantViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: ApplicantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ApplicantViewHolder(
        itemView: View,
        private val onItemClick: (Applicant) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val nameTextView: TextView = itemView.findViewById(R.id.applicant_name)
        private val positionTextView: TextView = itemView.findViewById(R.id.applicant_position)
        private val statusTextView: TextView = itemView.findViewById(R.id.applicant_status)
        private val dateTextView: TextView = itemView.findViewById(R.id.applicant_date)
        
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        
        fun bind(applicant: Applicant) {
            val fullName = "${applicant.firstName} ${applicant.lastName}"
            nameTextView.text = fullName
            positionTextView.text = applicant.appliedPosition
            statusTextView.text = applicant.status.toString().replace("_", " ")
            dateTextView.text = dateFormatter.format(applicant.applicationDate)
            
            // Set status color based on application status
            val statusColor = when (applicant.status) {
                com.capstone.unitechhr.models.ApplicationStatus.PENDING -> R.color.status_pending
                com.capstone.unitechhr.models.ApplicationStatus.REVIEWING -> R.color.status_reviewing
                com.capstone.unitechhr.models.ApplicationStatus.INTERVIEW_SCHEDULED -> R.color.status_interview
                com.capstone.unitechhr.models.ApplicationStatus.HIRED -> R.color.status_hired
                com.capstone.unitechhr.models.ApplicationStatus.REJECTED -> R.color.status_rejected
            }
            statusTextView.setTextColor(itemView.context.getColor(statusColor))
            
            itemView.setOnClickListener {
                onItemClick(applicant)
            }
        }
    }

    class ApplicantDiffCallback : DiffUtil.ItemCallback<Applicant>() {
        override fun areItemsTheSame(oldItem: Applicant, newItem: Applicant): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Applicant, newItem: Applicant): Boolean {
            return oldItem == newItem
        }
    }
} 