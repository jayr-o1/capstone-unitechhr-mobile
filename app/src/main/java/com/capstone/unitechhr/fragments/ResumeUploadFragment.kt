package com.capstone.unitechhr.fragments

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R
import com.capstone.unitechhr.repositories.FileRepository
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.capstone.unitechhr.viewmodels.FileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import java.lang.ref.WeakReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ResumeUploadFragment : Fragment() {
    private val TAG = "ResumeUploadFragment"
    private val authViewModel: AuthViewModel by activityViewModels()
    private val fileViewModel: FileViewModel by activityViewModels()
    private val fileRepository = FileRepository()
    
    // UI Components
    private lateinit var selectFileButton: MaterialButton
    private lateinit var uploadButton: MaterialButton
    private lateinit var selectedFileText: TextView
    private lateinit var uploadStatusText: TextView
    private lateinit var uploadProgressBar: ProgressBar
    private lateinit var currentResumeLayout: LinearLayout
    private lateinit var noResumeLayout: LinearLayout
    private lateinit var resumeActionsLayout: LinearLayout
    private lateinit var currentResumeNameText: TextView
    private lateinit var changeResumeButton: MaterialButton
    private lateinit var deleteResumeButton: MaterialButton
    private lateinit var resumePreviewContainer: FrameLayout
    private lateinit var rootView: View
    private lateinit var changeProgressLayout: LinearLayout
    private lateinit var changeProgressBar: ProgressBar
    private lateinit var changeStatusText: TextView
    
    // State variables
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null
    private var isChangingResume: Boolean = false
    
    // Store a weak reference to context to avoid memory leaks
    private var weakContext: WeakReference<Context>? = null
    
    // Activity result launcher for file picking
    private val getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_resume_upload, container, false)
    }
    
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Store weak reference to context when fragment attaches
        weakContext = WeakReference(context)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        rootView = view
        
        // Initialize UI components
        selectFileButton = view.findViewById(R.id.selectFileButton)
        uploadButton = view.findViewById(R.id.uploadButton)
        selectedFileText = view.findViewById(R.id.selectedFileText)
        uploadStatusText = view.findViewById(R.id.uploadStatusText)
        uploadProgressBar = view.findViewById(R.id.uploadProgressBar)
        currentResumeLayout = view.findViewById(R.id.currentResumeLayout)
        noResumeLayout = view.findViewById(R.id.noResumeLayout)
        resumeActionsLayout = view.findViewById(R.id.resumeActionsLayout)
        currentResumeNameText = view.findViewById(R.id.currentResumeNameText)
        changeResumeButton = view.findViewById(R.id.changeResumeButton)
        deleteResumeButton = view.findViewById(R.id.deleteResumeButton)
        resumePreviewContainer = view.findViewById(R.id.resumePreviewContainer)
        changeProgressLayout = view.findViewById(R.id.changeProgressLayout)
        changeProgressBar = view.findViewById(R.id.changeProgressBar)
        changeStatusText = view.findViewById(R.id.changeStatusText)
        
        // Find the tap to view text view but don't set up a separate click handler
        val tapToViewText = view.findViewById<TextView>(R.id.tapToViewText)
        
        // Store context in weak reference for safety
        weakContext = WeakReference(requireContext())
        
        // Set up back button
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Set up file selection button
        selectFileButton.setOnClickListener {
            openFilePicker()
        }
        
        // Set up upload button
        uploadButton.setOnClickListener {
            uploadSelectedFile()
        }
        
        // Set up resume preview click to view - This is the only click handler we need
        resumePreviewContainer.setOnClickListener {
            val resumeUrl = authViewModel.currentUser.value?.resumeUrl
            if (!resumeUrl.isNullOrEmpty()) {
                try {
                    // Log the URL for debugging
                    Log.d(TAG, "Opening PDF viewer with URL: $resumeUrl")
                    navigateToPdfViewer(resumeUrl)
                } catch (e: Exception) {
                    Log.e(TAG, "Error navigating to PDF viewer", e)
                    showSnackbar("Error opening resume: ${e.message}")
                }
            } else {
                // Inform user when there's no resume to view
                showSnackbar("No resume available to view")
            }
        }
        
        // Set up resume actions
        changeResumeButton.setOnClickListener {
            isChangingResume = true
            openFilePicker()
        }
        
        deleteResumeButton.setOnClickListener {
            showDeleteConfirmationDialog()
        }
        
        // Observe current user data for resume info
        authViewModel.currentUser.observe(viewLifecycleOwner) { user ->
            updateResumeSectionVisibility(user?.resumeUrl)
        }
        
        // Update UI with current resume info if available
        updateCurrentResumeInfo()
        
        // Observe upload progress
        fileViewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            if (progress > 0) {
                if (isChangingResume) {
                    // Update change progress
                    changeProgressBar.visibility = View.VISIBLE
                    changeProgressBar.progress = progress
                    
                    if (progress == 100) {
                        changeStatusText.text = "Upload complete"
                    }
                } else {
                    // Update normal upload progress
                    uploadProgressBar.visibility = View.VISIBLE
                    uploadProgressBar.progress = progress
                    
                    if (progress == 100) {
                        uploadStatusText.text = "Upload complete"
                        uploadStatusText.visibility = View.VISIBLE
                    }
                }
            } else {
                uploadProgressBar.visibility = View.GONE
            }
        }
        
        // Observe upload result
        fileViewModel.uploadResult.observe(viewLifecycleOwner) { downloadUrl ->
            if (downloadUrl != null) {
                // Update user's resume URL in AuthViewModel
                authViewModel.updateUserResume(requireContext(), downloadUrl)
                
                // Update UI
                updateCurrentResumeInfo()
                
                // Reset selected file
                resetFileSelection()
                
                // Show success message and reset UI based on operation
                if (isChangingResume) {
                    showSnackbar("Resume changed successfully")
                    changeProgressLayout.visibility = View.GONE
                    changeResumeButton.isEnabled = true
                    deleteResumeButton.isEnabled = true
                    isChangingResume = false
                } else {
                    showSnackbar("Resume uploaded successfully")
                }
                
                // Clear the result to prevent duplicate notifications
                fileViewModel.clearUploadResult()
            }
        }
        
        // Observe upload error
        fileViewModel.uploadError.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                if (isChangingResume) {
                    changeStatusText.text = "Error: $errorMessage"
                    changeResumeButton.isEnabled = true
                    deleteResumeButton.isEnabled = true
                } else {
                    uploadStatusText.text = errorMessage
                    uploadStatusText.visibility = View.VISIBLE
                }
                showSnackbar(errorMessage)
                
                // Clear the error to prevent duplicate notifications
                fileViewModel.clearUploadError()
            } else {
                uploadStatusText.visibility = View.GONE
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Clear any pending upload results or errors to prevent duplicate notifications
        fileViewModel.clearUploadResult()
        fileViewModel.clearUploadError()
    }
    
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        
        try {
            getContent.launch(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening file picker", e)
            showSnackbar("Unable to open file picker")
        }
    }
    
    private fun handleSelectedFile(uri: Uri) {
        try {
            // Get file name using multiple methods for better reliability
            val fileName = fileRepository.getFileNameFromContentResolver(requireContext(), uri)
                ?: fileRepository.getFileName(uri)
                ?: uri.toString()
            
            // Validate file type using both extension and content type
            val isPdfByExtension = fileRepository.isPdfFile(uri, fileName)
            val isPdfByContentType = fileRepository.isPdfFileByContentType(requireContext(), uri)
            
            if (!isPdfByExtension && !isPdfByContentType) {
                showSnackbar("Please select a PDF file")
                uploadStatusText.text = "Error: Only PDF files are allowed"
                uploadStatusText.visibility = View.VISIBLE
                return
            }
            
            // Store selected file info
            selectedFileUri = uri
            selectedFileName = fileName
            
            // If we're changing an existing resume, upload it immediately
            if (isChangingResume) {
                uploadSelectedFile()
            } else {
                // Otherwise, show the file in the upload section
                selectedFileText.text = fileName
                selectedFileText.visibility = View.VISIBLE
                uploadButton.isEnabled = true
                
                // Reset status
                uploadStatusText.visibility = View.GONE
                uploadProgressBar.visibility = View.GONE
            }
            
            Log.d(TAG, "Selected PDF file: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling selected file", e)
            uploadStatusText.text = "Error: ${e.message ?: "Unknown error"}"
            uploadStatusText.visibility = View.VISIBLE
            showSnackbar("Error processing file: ${e.message}")
        }
    }
    
    private fun uploadSelectedFile() {
        val uri = selectedFileUri
        val email = authViewModel.currentUserEmail.value
        
        if (uri == null) {
            showSnackbar("Please select a file first")
            return
        }
        
        if (email == null) {
            showSnackbar("You must be logged in to upload a resume")
            return
        }
        
        // Show the appropriate progress indicators based on whether we're changing or uploading
        if (isChangingResume) {
            // Show change progress
            changeProgressLayout.visibility = View.VISIBLE
            changeResumeButton.isEnabled = false
            deleteResumeButton.isEnabled = false
        } else {
            // Disable upload button during upload
            uploadButton.isEnabled = false
        }
        
        // Use the email as the user ID for storage path
        val userId = email.replace("@", "-").replace(".", "-")
        
        // Start upload
        fileViewModel.uploadResume(requireContext(), uri, userId)
    }
    
    private fun updateCurrentResumeInfo() {
        // Get current resume URL from AuthViewModel
        val resumeUrl = authViewModel.currentUser.value?.resumeUrl
        updateResumeSectionVisibility(resumeUrl)
    }
    
    private fun updateResumeSectionVisibility(resumeUrl: String?) {
        if (!resumeUrl.isNullOrEmpty()) {
            // Extract file name from URL
            val fileName = resumeUrl.substringAfterLast('/').substringAfterLast('_')
            
            // Update UI
            currentResumeNameText.text = fileName
            noResumeLayout.visibility = View.GONE
            currentResumeLayout.visibility = View.VISIBLE
            resumeActionsLayout.visibility = View.VISIBLE
            
            // Hide the upload new resume card when a resume exists
            view?.findViewById<View>(R.id.uploadResumeCard)?.visibility = View.GONE
        } else {
            // Always show resume preview, hide icon view
            noResumeLayout.visibility = View.GONE
            currentResumeLayout.visibility = View.VISIBLE
            // Only hide action buttons when no resume is available
            resumeActionsLayout.visibility = View.GONE
            
            // Show the upload new resume card when no resume exists
            view?.findViewById<View>(R.id.uploadResumeCard)?.visibility = View.VISIBLE
        }
    }
    
    private fun resetFileSelection() {
        selectedFileUri = null
        selectedFileName = null
        selectedFileText.visibility = View.GONE
        uploadButton.isEnabled = false
        
        // Also reset change progress indicators if needed
        if (isChangingResume) {
            changeProgressLayout.visibility = View.GONE
            changeResumeButton.isEnabled = true
            deleteResumeButton.isEnabled = true
            isChangingResume = false
        }
    }
    
    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Resume")
            .setMessage("Are you sure you want to delete your resume? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteResume()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun deleteResume() {
        val email = authViewModel.currentUserEmail.value
        
        if (email == null) {
            showSnackbar("You must be logged in to delete your resume")
            return
        }
        
        // Delete from Firebase Storage and update user profile
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Here you would also delete from Firebase Storage if needed
                // For now, just update the user profile
                authViewModel.updateUserResume(requireContext(), null)
                
                // Update UI
                updateCurrentResumeInfo()
                
                // Show success message
                showSnackbar("Resume deleted successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting resume", e)
                showSnackbar("Error deleting resume: ${e.message}")
            }
        }
    }
    
    private fun navigateToPdfViewer(pdfUrl: String) {
        try {
            // Create navigation action with the PDF URL and title
            val action = ResumeUploadFragmentDirections.actionResumeUploadFragmentToPdfViewerFragment(
                pdfUrl = pdfUrl,
                pdfTitle = "My Resume"
            )
            
            // Log navigation attempt
            Log.d(TAG, "Navigating to PDF viewer with URL: $pdfUrl")
            
            // Perform navigation
            findNavController().navigate(action)
        } catch (e: Exception) {
            // Log error and show user-friendly message
            Log.e(TAG, "Failed to navigate to PDF viewer", e)
            showSnackbar("Unable to open resume viewer. Please try again.")
        }
    }
    
    private fun showSnackbar(message: String) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    }
}