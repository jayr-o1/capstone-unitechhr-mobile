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
        private val companyTextView: TextView = itemView.findViewById(R.id.companyTextView)
        private val locationTextView: TextView = itemView.findViewById(R.id.locationTextView)
        private val salaryTextView: TextView = itemView.findViewById(R.id.salaryTextView)
        private val jobTypeTextView: TextView = itemView.findViewById(R.id.jobTypeTextView)
        private val postedDateTextView: TextView = itemView.findViewById(R.id.postedDateTextView)
        private val favoriteIcon: ImageView = itemView.findViewById(R.id.favoriteIcon)
        
        fun bind(job: Job) {
            jobTitleTextView.text = job.title
            companyTextView.text = job.company
            locationTextView.text = job.location
            salaryTextView.text = job.salary
            jobTypeTextView.text = job.jobType
            
            // Calculate days since posted
            val now = System.currentTimeMillis()
            val postedTime = job.postedDate.time
            val diffInMillis = now - postedTime
            val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)
            
            postedDateTextView.text = when {
                diffInDays == 0L -> "Posted today"
                diffInDays == 1L -> "Posted yesterday"
                diffInDays < 30L -> "Posted $diffInDays days ago"
                else -> {
                    val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    "Posted on ${formatter.format(job.postedDate)}"
                }
            }
            
            // Set favorite icon
            favoriteIcon.setImageResource(
                if (job.isFavorite) android.R.drawable.star_on 
                else android.R.drawable.star_off
            )
            
            // Set click listener for the entire item
            itemView.setOnClickListener {
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