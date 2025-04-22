package com.capstone.unitechhr.adapters

import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.Job
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class JobAdapter(private val onItemClick: (Job) -> Unit) : 
    ListAdapter<Job, JobAdapter.JobViewHolder>(JobDiffCallback()) {

    private val TAG = "JobAdapter"
    
    // Map to store university colors
    private val universityColors = mutableMapOf<String, Int>()
    
    // Predefined colors for universities
    private val colorOptions = listOf(
        "#E53935", // Red
        "#8E24AA", // Purple
        "#3949AB", // Indigo
        "#039BE5", // Light Blue
        "#00897B", // Teal
        "#7CB342", // Light Green
        "#FFB300", // Amber
        "#F57C00", // Orange
        "#5D4037", // Brown
        "#546E7A"  // Blue Grey
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view, onItemClick, universityColors, colorOptions)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        val job = getItem(position)
        Log.d(TAG, "Binding job at position $position: ${job.id} - ${job.title}")
        holder.bind(job)
    }

    override fun submitList(list: List<Job>?) {
        Log.d(TAG, "Submitting job list: ${list?.size ?: 0} items")
        
        // Assign colors to universities
        list?.forEach { job ->
            if (job.universityId.isNotEmpty() && !universityColors.containsKey(job.universityId)) {
                // Assign a random color from colorOptions
                val randomColor = colorOptions[universityColors.size % colorOptions.size]
                universityColors[job.universityId] = Color.parseColor(randomColor)
            }
        }
        
        if (list != null && list.isNotEmpty()) {
            // Log first job as an example
            val firstJob = list[0]
            Log.d(TAG, "First job in list: ${firstJob.id} - ${firstJob.title}")
            Log.d(TAG, "First job university: ${firstJob.universityId} - ${firstJob.universityName}")
            Log.d(TAG, "Job list types: ${list.map { it.jobType }.distinct()}")
        }
        super.submitList(list)
    }

    class JobViewHolder(
        itemView: View,
        private val onItemClick: (Job) -> Unit,
        private val universityColors: Map<String, Int>,
        private val colorOptions: List<String>
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val TAG = "JobViewHolder"
        private val jobTitleTextView: TextView = itemView.findViewById(R.id.jobTitleTextView)
        private val universityTextView: TextView = itemView.findViewById(R.id.universityTextView)
        private val companyTextView: TextView = itemView.findViewById(R.id.companyTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val salaryTextView: TextView = itemView.findViewById(R.id.salaryTextView)
        private val jobTypeTextView: TextView = itemView.findViewById(R.id.jobTypeTextView)
        private val postedDateTextView: TextView = itemView.findViewById(R.id.postedDateTextView)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)
        private val universityColorStrip: View = itemView.findViewById(R.id.universityColorStrip)
        
        fun bind(job: Job) {
            Log.d(TAG, "Binding job ${job.id} - ${job.title}")
            
            jobTitleTextView.text = job.title
            
            // Show university name if available
            if (job.universityName.isNotEmpty()) {
                universityTextView.text = job.universityName
                universityTextView.visibility = View.VISIBLE
                
                // Set university color strip
                if (job.universityId.isNotEmpty() && universityColors.containsKey(job.universityId)) {
                    universityColorStrip.setBackgroundColor(universityColors[job.universityId]!!)
                } else {
                    // Fallback to default blue if no color assigned
                    universityColorStrip.setBackgroundColor(Color.parseColor("#2A3990"))
                }
            } else {
                universityTextView.visibility = View.GONE
                // Default color
                universityColorStrip.setBackgroundColor(Color.parseColor("#2A3990"))
            }
            
            // Use department as company if available
            val companyText = job.department?.takeIf { it.isNotEmpty() } ?: job.company
            companyTextView.text = companyText
            
            // Use workSetup as location if available
            val locationText = job.workSetup?.takeIf { it.isNotEmpty() } ?: job.location
            locationTextView.text = locationText
            
            salaryTextView.text = job.salary
            
            // Use status as job type if available
            val jobTypeText = job.status?.takeIf { it.isNotEmpty() } ?: job.jobType
            jobTypeTextView.text = jobTypeText
            
            // Format posted date as relative time
            val now = System.currentTimeMillis()
            val postedTime = job.postedDate.time
            val diffInMillis = now - postedTime
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            
            postedDateTextView.text = when {
                diffInDays > 30 -> "Posted ${diffInDays / 30} months ago"
                diffInDays > 0 -> "Posted $diffInDays days ago"
                else -> "Posted today"
            }
            
            // Set favorite icon based on job's favorite status
            favoriteIcon.setImageResource(
                if (job.isFavorite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
            
            // Set item click listener
            itemView.setOnClickListener { 
                Log.d(TAG, "Job clicked: ${job.id} - ${job.title}")
                
                // Log if the job has the needed detailed information
                val hasKeyDuties = !job.keyDuties.isNullOrEmpty()
                val hasEssentialSkills = !job.essentialSkills.isNullOrEmpty() 
                val hasQualifications = !job.qualifications.isNullOrEmpty()
                
                Log.d(TAG, "Job has key duties: $hasKeyDuties")
                Log.d(TAG, "Job has essential skills: $hasEssentialSkills")
                Log.d(TAG, "Job has qualifications: $hasQualifications")
                
                onItemClick(job)
            }
        }
    }
    
    class JobDiffCallback : DiffUtil.ItemCallback<Job>() {
        override fun areItemsTheSame(oldItem: Job, newItem: Job): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: Job, newItem: Job): Boolean {
            return oldItem == newItem
        }
    }
} 