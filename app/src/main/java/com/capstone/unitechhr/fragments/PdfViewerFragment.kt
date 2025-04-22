package com.capstone.unitechhr.fragments

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.capstone.unitechhr.R
import com.capstone.unitechhr.repositories.ApplicationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URLEncoder

class PdfViewerFragment : Fragment() {
    private val TAG = "PdfViewerFragment"
    private val args: PdfViewerFragmentArgs by navArgs()
    
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var errorText: TextView
    private lateinit var titleText: TextView
    private lateinit var backButton: ImageView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pdf_viewer, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        webView = view.findViewById(R.id.pdfWebView)
        progressBar = view.findViewById(R.id.pdfProgressBar)
        errorText = view.findViewById(R.id.pdfErrorText)
        titleText = view.findViewById(R.id.pdfTitleText)
        backButton = view.findViewById(R.id.backButton)
        
        // Set up back button
        backButton.setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Get PDF URL from arguments
        val pdfUrl = args.pdfUrl
        val pdfTitle = args.pdfTitle ?: "Resume"
        
        titleText.text = pdfTitle
        
        if (pdfUrl.isNullOrEmpty()) {
            showError("No PDF URL provided")
            return
        }
        
        // Load PDF
        loadPdf(pdfUrl)
    }
    
    private fun loadPdf(pdfUrl: String) {
        try {
            progressBar.visibility = View.VISIBLE
            errorText.visibility = View.GONE
            
            // Configure WebView
            webView.settings.javaScriptEnabled = true
            webView.settings.builtInZoomControls = true
            webView.settings.displayZoomControls = false
            webView.settings.useWideViewPort = true
            webView.settings.loadWithOverviewMode = true
            
            webView.webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    progressBar.visibility = View.VISIBLE
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    progressBar.visibility = View.GONE
                }
                
                override fun onReceivedError(
                    view: WebView?,
                    errorCode: Int,
                    description: String?,
                    failingUrl: String?
                ) {
                    progressBar.visibility = View.GONE
                    showError("Error loading PDF: $description")
                }
            }
            
            // Check if URL is a direct PDF link or a Firebase URL
            if (pdfUrl.startsWith("http") && (pdfUrl.endsWith(".pdf", ignoreCase = true) || pdfUrl.contains("firebasestorage"))) {
                // For Firebase or direct PDF URLs, use Google Docs Viewer
                val encodedUrl = URLEncoder.encode(pdfUrl, "UTF-8")
                val googleDocsUrl = "https://docs.google.com/viewer?url=$encodedUrl&embedded=true"
                webView.loadUrl(googleDocsUrl)
                
                Log.d(TAG, "Loading PDF with Google Docs Viewer: $googleDocsUrl")
            } else if (pdfUrl.startsWith("content://") || pdfUrl.startsWith("file://")) {
                // For local file or content URI
                webView.loadUrl(pdfUrl)
                Log.d(TAG, "Loading local PDF: $pdfUrl")
            } else {
                // If it's just a path, try loading it directly
                webView.loadUrl("file://$pdfUrl")
                Log.d(TAG, "Loading file path PDF: $pdfUrl")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading PDF", e)
            showError("Error loading PDF: ${e.message}")
        }
    }
    
    /**
     * Download the PDF file from URL first and then show it
     * This is an alternative approach if the above method doesn't work
     */
    private fun downloadAndShowPdf(pdfUrl: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                progressBar.visibility = View.VISIBLE
                errorText.visibility = View.GONE
                
                // Download the file
                val applicationRepository = ApplicationRepository()
                val file = withContext(Dispatchers.IO) {
                    applicationRepository.downloadFileFromUrl(requireContext(), pdfUrl)
                }
                
                if (file != null) {
                    // Load the downloaded file
                    val fileUri = Uri.fromFile(file)
                    webView.loadUrl("file://${file.absolutePath}")
                    Log.d(TAG, "PDF downloaded and loaded from: ${file.absolutePath}")
                } else {
                    showError("Failed to download PDF file")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error downloading PDF", e)
                showError("Error downloading PDF: ${e.message}")
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }
    
    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        progressBar.visibility = View.GONE
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
} 