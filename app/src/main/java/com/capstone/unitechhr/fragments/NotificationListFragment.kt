package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.capstone.unitechhr.R
import com.capstone.unitechhr.adapters.NotificationAdapter
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.capstone.unitechhr.viewmodels.NotificationViewModel

class NotificationListFragment : Fragment() {
    
    private val authViewModel: AuthViewModel by activityViewModels()
    private val notificationViewModel: NotificationViewModel by viewModels()
    
    private lateinit var adapter: NotificationAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyStateContainer: LinearLayout
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notification_list, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        recyclerView = view.findViewById(R.id.notificationsRecyclerView)
        progressBar = view.findViewById(R.id.progressBar)
        emptyStateContainer = view.findViewById(R.id.emptyStateContainer)
        
        // Set up back button
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Setup adapter
        adapter = NotificationAdapter(
            // Handle notification click (mark as read)
            onItemClick = { notification ->
                // Mark notification as read when clicked
                authViewModel.currentUser.value?.let { user ->
                    val applicantId = user.email.replace("@", "-").replace(".", "-")
                    notificationViewModel.markAsRead(applicantId, notification.id)
                    Toast.makeText(context, notification.title, Toast.LENGTH_SHORT).show()
                }
            },
            // Handle dismiss button click
            onDismissClick = { notification ->
                // Dismiss notification when the dismiss button is clicked
                authViewModel.currentUser.value?.let { user ->
                    val applicantId = user.email.replace("@", "-").replace(".", "-")
                    notificationViewModel.dismissNotification(applicantId, notification.id)
                }
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        
        // Observe ViewModel data
        notificationViewModel.notifications.observe(viewLifecycleOwner) { notifications ->
            if (notifications.isEmpty()) {
                emptyStateContainer.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateContainer.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                adapter.updateNotifications(notifications)
            }
        }
        
        notificationViewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
        
        // Load notifications for current user
        loadNotifications()
    }
    
    override fun onResume() {
        super.onResume()
        loadNotifications()
    }
    
    private fun loadNotifications() {
        authViewModel.currentUser.value?.let { user ->
            val applicantId = user.email.replace("@", "-").replace(".", "-")
            notificationViewModel.loadNotifications(applicantId)
        }
    }
} 