package com.capstone.unitechhr.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.capstone.unitechhr.repositories.FileRepository
import kotlinx.coroutines.launch
import java.util.UUID

class FileViewModel : ViewModel() {
    private val fileRepository = FileRepository()
    private val TAG = "FileViewModel"
    
    private val _uploadProgress = MutableLiveData<Int>()
    val uploadProgress: LiveData<Int> = _uploadProgress
    
    private val _uploadResult = MutableLiveData<String?>()
    val uploadResult: LiveData<String?> = _uploadResult
    
    private val _uploadError = MutableLiveData<String?>()
    val uploadError: LiveData<String?> = _uploadError
    
    /**
     * Upload a resume file to Firebase Storage
     * @param context The application context
     * @param fileUri The URI of the file to upload
     * @param userId The ID of the user who owns the file
     */
    fun uploadResume(context: Context, fileUri: Uri, userId: String) {
        viewModelScope.launch {
            try {
                _uploadProgress.value = 0
                _uploadError.value = null
                
                // Get file name using content resolver for better reliability
                val fileName = fileRepository.getFileNameFromContentResolver(context, fileUri) 
                    ?: fileRepository.getFileName(fileUri)
                    ?: UUID.randomUUID().toString() + ".pdf"
                
                // Check if the file is a PDF using both methods
                val isPdfByExtension = fileRepository.isPdfFile(fileUri, fileName)
                val isPdfByContentType = fileRepository.isPdfFileByContentType(context, fileUri)
                
                if (!isPdfByExtension && !isPdfByContentType) {
                    _uploadError.value = "Only PDF files are allowed. Please select a PDF document."
                    _uploadProgress.value = 0
                    return@launch
                }
                
                Log.d(TAG, "Uploading PDF file: $fileName")
                
                // Generate a unique file name with UUID
                val uniqueFileName = "${UUID.randomUUID()}_$fileName"
                
                // Path in Firebase Storage: resumes/userId/uniqueFileName
                val path = "resumes/$userId"
                
                // Upload file and get download URL
                _uploadProgress.value = 20
                
                try {
                    val downloadUrl = fileRepository.uploadFile(fileUri, path, uniqueFileName)
                    
                    if (downloadUrl != null) {
                        _uploadProgress.value = 100
                        _uploadResult.value = downloadUrl
                        Log.d(TAG, "Resume uploaded successfully: $downloadUrl")
                    } else {
                        _uploadProgress.value = 0
                        _uploadError.value = "Failed to upload file. Please try again."
                        Log.e(TAG, "Failed to upload resume - null download URL")
                    }
                } catch (e: Exception) {
                    _uploadProgress.value = 0
                    _uploadError.value = "Upload failed: ${e.message ?: "Unknown error"}"
                    Log.e(TAG, "Error in upload process", e)
                }
            } catch (e: Exception) {
                _uploadProgress.value = 0
                _uploadError.value = "Error: ${e.message ?: "Unknown error"}"
                Log.e(TAG, "Error uploading resume", e)
            }
        }
    }
    
    /**
     * Reset upload state
     */
    fun resetUploadState() {
        _uploadProgress.value = 0
        _uploadResult.value = null
        _uploadError.value = null
    }
} 