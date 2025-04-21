package com.capstone.unitechhr.repositories

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class FileRepository {
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    private val TAG = "FileRepository"

    /**
     * Upload a file to Firebase Storage
     * @param fileUri The URI of the file to upload
     * @param path The path in Firebase Storage where the file should be stored
     * @param fileName The name to give the file in Firebase Storage
     * @return The download URL of the uploaded file, or null if upload failed
     */
    suspend fun uploadFile(fileUri: Uri, path: String, fileName: String): String? = withContext(Dispatchers.IO) {
        try {
            // Create reference to the file location in Firebase Storage
            val fileRef = storageRef.child("$path/$fileName")
            
            // Upload file
            val uploadTask = fileRef.putFile(fileUri).await()
            
            // Get download URL
            val downloadUrl = fileRef.downloadUrl.await().toString()
            
            Log.d(TAG, "File uploaded successfully: $downloadUrl")
            return@withContext downloadUrl
        } catch (e: Exception) {
            Log.e(TAG, "Error uploading file: ${e.message}", e)
            return@withContext null
        }
    }

    /**
     * Check if a file is a PDF
     * @param uri The URI of the file to check
     * @param fileName The name of the file
     * @return True if the file is a PDF, false otherwise
     */
    fun isPdfFile(uri: Uri?, fileName: String?): Boolean {
        if (uri == null) return false
        
        // Check file extension
        val hasCorrectExtension = fileName?.lowercase()?.endsWith(".pdf") == true
        
        // If file extension check passes, we consider it valid for simplicity
        return hasCorrectExtension
    }

    /**
     * Check if a file is a PDF using more thorough content type checking
     * @param context The context
     * @param uri The URI of the file to check
     * @return True if the file is a PDF, false otherwise
     */
    fun isPdfFileByContentType(context: Context, uri: Uri?): Boolean {
        if (uri == null) return false
        
        // First check by file extension
        val fileExtension = getFileExtension(uri)?.lowercase()
        val hasPdfExtension = fileExtension == "pdf"
        
        // Then try to get MIME type from content resolver
        val mimeType = try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }
        
        // Check MIME type if available
        val hasPdfMimeType = mimeType?.equals("application/pdf", ignoreCase = true) == true
        
        // If either check passes, we consider it valid
        return hasPdfExtension || hasPdfMimeType
    }

    /**
     * Get the file extension from a URI
     * @param uri The URI of the file
     * @return The file extension
     */
    fun getFileExtension(uri: Uri): String? {
        val fileName = getFileName(uri)
        return fileName?.let {
            if (it.contains(".")) {
                it.substring(it.lastIndexOf(".") + 1)
            } else {
                null
            }
        }
    }

    /**
     * Get the file name from a URI
     * @param uri The URI of the file
     * @return The file name
     */
    fun getFileName(uri: Uri): String? {
        val result: String?
        val cursor = uri.path?.let { path ->
            if (path.contains("/")) {
                path.substring(path.lastIndexOf("/") + 1)
            } else {
                path
            }
        }
        
        return cursor
    }
    
    /**
     * Get the file name from a URI using content resolver
     * @param context The context
     * @param uri The URI of the file
     * @return The file name
     */
    fun getFileNameFromContentResolver(context: Context, uri: Uri): String? {
        // Try using content resolver for content URIs
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val displayNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        return it.getString(displayNameIndex)
                    }
                }
            }
        }
        
        // Fallback to extracting from URI path
        return getFileName(uri)
    }
} 