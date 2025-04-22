package com.capstone.unitechhr.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.OnboardingChecklistTask

class OnboardingChecklistAdapter(
    private val onTaskStatusChanged: (OnboardingChecklistTask, Boolean) -> Unit
) : ListAdapter<OnboardingChecklistTask, OnboardingChecklistAdapter.TaskViewHolder>(TaskDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_task, parent, false)
        return TaskViewHolder(view, onTaskStatusChanged)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TaskViewHolder(
        itemView: View,
        private val onTaskStatusChanged: (OnboardingChecklistTask, Boolean) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val taskCheckbox: CheckBox = itemView.findViewById(R.id.taskCheckbox)
        
        fun bind(task: OnboardingChecklistTask) {
            taskCheckbox.text = task.task
            taskCheckbox.isChecked = task.completed
            
            // Make the checkbox read-only by disabling clicks
            taskCheckbox.isEnabled = false
            
            // Remove the OnCheckedChangeListener since users shouldn't be able to change the status
            taskCheckbox.setOnCheckedChangeListener(null)
        }
    }
    
    class TaskDiffCallback : DiffUtil.ItemCallback<OnboardingChecklistTask>() {
        override fun areItemsTheSame(oldItem: OnboardingChecklistTask, newItem: OnboardingChecklistTask): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: OnboardingChecklistTask, newItem: OnboardingChecklistTask): Boolean {
            return oldItem == newItem
        }
    }
} 