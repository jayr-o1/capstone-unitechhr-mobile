package com.capstone.unitechhr.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R
import com.capstone.unitechhr.repositories.FileRepository
import com.capstone.unitechhr.viewmodels.AuthViewModel
import com.capstone.unitechhr.viewmodels.FileViewModel
import com.google.android.material.button.MaterialButton
import java.io.File
import java.lang.ref.WeakReference

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
    private lateinit var currentResumeNameText: TextView
    
    // State variables
    private var selectedFileUri: Uri? = null
    private var selectedFileName: String? = null
    
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
        
        // Initialize UI components
        selectFileButton = view.findViewById(R.id.selectFileButton)
        uploadButton = view.findViewById(R.id.uploadButton)
        selectedFileText = view.findViewById(R.id.selectedFileText)
        uploadStatusText = view.findViewById(R.id.uploadStatusText)
        uploadProgressBar = view.findViewById(R.id.uploadProgressBar)
        currentResumeLayout = view.findViewById(R.id.currentResumeLayout)
        currentResumeNameText = view.findViewById(R.id.currentResumeNameText)
        
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
        
        // Update UI with current resume info if available
        updateCurrentResumeInfo()
        
        // Observe upload progress
        fileViewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            if (progress > 0) {
                uploadProgressBar.visibility = View.VISIBLE
                uploadProgressBar.progress = progress
                
                if (progress == 100) {
                    uploadStatusText.text = "Upload complete"
                    uploadStatusText.visibility = View.VISIBLE
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
                
                // Show success message
                Toast.makeText(context, "Resume uploaded successfully", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Observe upload error
        fileViewModel.uploadError.observe(viewLifecycleOwner) { errorMessage ->
            if (!errorMessage.isNullOrEmpty()) {
                uploadStatusText.text = errorMessage
                uploadStatusText.visibility = View.VISIBLE
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            } else {
                uploadStatusText.visibility = View.GONE
            }
        }
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
            Toast.makeText(context, "Unable to open file picker", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(context, "Please select a PDF file", Toast.LENGTH_SHORT).show()
                uploadStatusText.text = "Error: Only PDF files are allowed"
                uploadStatusText.visibility = View.VISIBLE
                return
            }
            
            // Store selected file info
            selectedFileUri = uri
            selectedFileName = fileName
            
            // Update UI
            selectedFileText.text = fileName
            selectedFileText.visibility = View.VISIBLE
            uploadButton.isEnabled = true
            
            // Reset status
            uploadStatusText.visibility = View.GONE
            uploadProgressBar.visibility = View.GONE
            
            Log.d(TAG, "Selected PDF file: $fileName")
        } catch (e: Exception) {
            Log.e(TAG, "Error handling selected file", e)
            uploadStatusText.text = "Error: ${e.message ?: "Unknown error"}"
            uploadStatusText.visibility = View.VISIBLE
            Toast.makeText(context, "Error processing file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun uploadSelectedFile() {
        val uri = selectedFileUri
        val email = authViewModel.currentUserEmail.value
        
        if (uri == null) {
            Toast.makeText(context, "Please select a file first", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (email == null) {
            Toast.makeText(context, "You must be logged in to upload a resume", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Disable upload button during upload
        uploadButton.isEnabled = false
        
        // Use the email as the user ID for storage path
        val userId = email.replace("@", "-").replace(".", "-")
        
        // Start upload
        fileViewModel.uploadResume(requireContext(), uri, userId)
    }
    
    private fun updateCurrentResumeInfo() {
        // Get current resume URL from AuthViewModel
        val resumeUrl = authViewModel.currentUser.value?.resumeUrl
        
        if (!resumeUrl.isNullOrEmpty()) {
            // Extract file name from URL
            val fileName = resumeUrl.substringAfterLast('/').substringAfterLast('_')
            
            // Update UI
            currentResumeNameText.text = fileName
            currentResumeLayout.visibility = View.VISIBLE
        } else {
            currentResumeLayout.visibility = View.GONE
        }
    }
    
    private fun resetFileSelection() {
        selectedFileUri = null
        selectedFileName = null
        selectedFileText.visibility = View.GONE
        uploadButton.isEnabled = true
    }
} 