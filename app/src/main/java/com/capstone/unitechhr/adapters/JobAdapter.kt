package com.capstone.unitechhr.adapters

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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JobViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_job, parent, false)
        return JobViewHolder(view, onItemClick)
    }

    override fun onBindViewHolder(holder: JobViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class JobViewHolder(
        itemView: View,
        private val onItemClick: (Job) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val jobTitleTextView: TextView = itemView.findViewById(R.id.jobTitleTextView)
        private val universityTextView: TextView = itemView.findViewById(R.id.universityTextView)
        private val companyTextView: TextView = itemView.findViewById(R.id.companyTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val salaryTextView: TextView = itemView.findViewById(R.id.salaryTextView)
        private val jobTypeTextView: TextView = itemView.findViewById(R.id.jobTypeTextView)
        private val postedDateTextView: TextView = itemView.findViewById(R.id.postedDateTextView)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)
        
        fun bind(job: Job) {
            jobTitleTextView.text = job.title
            
            // Show university name if available
            if (job.universityName.isNotEmpty()) {
                universityTextView.text = job.universityName
                universityTextView.visibility = View.VISIBLE
            } else {
                universityTextView.visibility = View.GONE
            }
            
            companyTextView.text = job.company
            locationTextView.text = job.location
            salaryTextView.text = job.salary
            jobTypeTextView.text = job.jobType
            
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
            itemView.setOnClickListener { onItemClick(job) }
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