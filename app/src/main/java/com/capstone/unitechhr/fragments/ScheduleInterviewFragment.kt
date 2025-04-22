package com.capstone.unitechhr.fragments

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.capstone.unitechhr.R
import com.capstone.unitechhr.viewmodels.InterviewViewModel
import com.capstone.unitechhr.viewmodels.JobViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ScheduleInterviewFragment : Fragment() {

    private val TAG = "ScheduleInterview"
    private val jobViewModel: JobViewModel by activityViewModels()
    private val interviewViewModel: InterviewViewModel by activityViewModels()
    private val firestore = FirebaseFirestore.getInstance()
    
    // UI components
    private lateinit var applicantNameText: TextView
    private lateinit var applicantEmailText: TextView
    private lateinit var matchPercentageText: TextView
    private lateinit var recommendationText: TextView
    private lateinit var educationText: TextView
    private lateinit var experienceText: TextView
    private lateinit var jobTitleText: TextView
    private lateinit var jobDepartmentText: TextView
    private lateinit var jobUniversityText: TextView
    private lateinit var datePickerButton: Button
    private lateinit var timePickerButton: Button
    private lateinit var durationSpinner: Spinner
    private lateinit var locationRadioGroup: RadioGroup
    private lateinit var virtualRadioButton: RadioButton
    private lateinit var inPersonRadioButton: RadioButton
    private lateinit var virtualMeetingLayout: LinearLayout
    private lateinit var physicalLocationLayout: LinearLayout
    private lateinit var meetingLinkEditText: TextInputEditText
    private lateinit var addressEditText: TextInputEditText
    private lateinit var interviewerEditText: TextInputEditText
    private lateinit var notesEditText: TextInputEditText
    private lateinit var scheduleButton: Button
    
    // State variables
    private var selectedDate: Date? = null
    private var selectedApplicantId: String? = null
    private var selectedJobId: String? = null
    private var selectedUniversityId: String? = null
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_schedule_interview, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize UI components
        applicantNameText = view.findViewById(R.id.applicantNameText)
        applicantEmailText = view.findViewById(R.id.applicantEmailText)
        matchPercentageText = view.findViewById(R.id.matchPercentageText)
        recommendationText = view.findViewById(R.id.recommendationText)
        educationText = view.findViewById(R.id.educationText)
        experienceText = view.findViewById(R.id.experienceText)
        jobTitleText = view.findViewById(R.id.jobTitleText)
        jobDepartmentText = view.findViewById(R.id.jobDepartmentText)
        jobUniversityText = view.findViewById(R.id.jobUniversityText)
        datePickerButton = view.findViewById(R.id.datePickerButton)
        timePickerButton = view.findViewById(R.id.timePickerButton)
        durationSpinner = view.findViewById(R.id.durationSpinner)
        locationRadioGroup = view.findViewById(R.id.locationRadioGroup)
        virtualRadioButton = view.findViewById(R.id.virtualRadioButton)
        inPersonRadioButton = view.findViewById(R.id.inPersonRadioButton)
        virtualMeetingLayout = view.findViewById(R.id.virtualMeetingLayout)
        physicalLocationLayout = view.findViewById(R.id.physicalLocationLayout)
        meetingLinkEditText = view.findViewById(R.id.meetingLinkEditText)
        addressEditText = view.findViewById(R.id.addressEditText)
        interviewerEditText = view.findViewById(R.id.interviewerEditText)
        notesEditText = view.findViewById(R.id.notesEditText)
        scheduleButton = view.findViewById(R.id.scheduleButton)
        
        // Set up back button
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            findNavController().navigateUp()
        }
        
        // Set up duration spinner
        setupDurationSpinner()
        
        // Set up date and time pickers
        setupDateTimePickers()
        
        // Set up location radio buttons
        setupLocationRadioGroup()
        
        // Set up schedule button
        setupScheduleButton()
        
        // Load job data
        loadJobData()
        
        // Try to get arguments (if coming from a specific job or applicant)
        handleArguments()
    }
    
    private fun setupDurationSpinner() {
        // Create duration options (in minutes)
        val durations = listOf(15, 30, 45, 60, 90, 120)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            durations.map { "$it minutes" }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        durationSpinner.adapter = adapter
        
        // Set default to 60 minutes (index 3)
        durationSpinner.setSelection(3)
    }
    
    private fun setupDateTimePickers() {
        // Initialize with current date/time + 1 day
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1) // Default to tomorrow
        selectedDate = calendar.time
        
        // Format for display
        updateDateTimeButtons(calendar)
        
        // Date picker
        datePickerButton.setOnClickListener {
            showDatePicker(calendar)
        }
        
        // Time picker
        timePickerButton.setOnClickListener {
            showTimePicker(calendar)
        }
    }
    
    private fun updateDateTimeButtons(calendar: Calendar) {
        val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        
        datePickerButton.text = dateFormat.format(calendar.time)
        timePickerButton.text = timeFormat.format(calendar.time)
        selectedDate = calendar.time
    }
    
    private fun showDatePicker(calendar: Calendar) {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        
        DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            calendar.set(selectedYear, selectedMonth, selectedDay)
            updateDateTimeButtons(calendar)
        }, year, month, day).show()
    }
    
    private fun showTimePicker(calendar: Calendar) {
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        
        TimePickerDialog(requireContext(), { _, selectedHour, selectedMinute ->
            calendar.set(Calendar.HOUR_OF_DAY, selectedHour)
            calendar.set(Calendar.MINUTE, selectedMinute)
            updateDateTimeButtons(calendar)
        }, hour, minute, false).show()
    }
    
    private fun setupLocationRadioGroup() {
        locationRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.virtualRadioButton -> {
                    virtualMeetingLayout.visibility = View.VISIBLE
                    physicalLocationLayout.visibility = View.GONE
                }
                R.id.inPersonRadioButton -> {
                    virtualMeetingLayout.visibility = View.GONE
                    physicalLocationLayout.visibility = View.VISIBLE
                }
            }
        }
        
        // Set initial state
        virtualRadioButton.isChecked = true
        virtualMeetingLayout.visibility = View.VISIBLE
        physicalLocationLayout.visibility = View.GONE
    }
    
    private fun setupScheduleButton() {
        scheduleButton.setOnClickListener {
            if (validateForm()) {
                scheduleInterview()
            }
        }
    }
    
    private fun validateForm(): Boolean {
        // Validate that all required fields are filled
        if (selectedDate == null) {
            Toast.makeText(context, "Please select a date and time", Toast.LENGTH_SHORT).show()
            return false
        }
        
        if (selectedApplicantId == null || selectedJobId == null) {
            Toast.makeText(context, "Missing applicant or job information", Toast.LENGTH_SHORT).show()
            return false
        }
        
        val interviewer = interviewerEditText.text.toString().trim()
        if (interviewer.isEmpty()) {
            interviewerEditText.error = "Please enter an interviewer name"
            return false
        }
        
        // Check location-specific fields
        if (virtualRadioButton.isChecked) {
            val meetingLink = meetingLinkEditText.text.toString().trim()
            if (meetingLink.isEmpty()) {
                meetingLinkEditText.error = "Please enter a meeting link"
                return false
            }
        } else {
            val address = addressEditText.text.toString().trim()
            if (address.isEmpty()) {
                addressEditText.error = "Please enter an address"
                return false
            }
        }
        
        return true
    }
    
    private fun scheduleInterview() {
        val duration = getDurationValue()
        val isVirtual = virtualRadioButton.isChecked
        val location = if (isVirtual) "Virtual" else addressEditText.text.toString().trim()
        val meetingLink = if (isVirtual) meetingLinkEditText.text.toString().trim() else ""
        val interviewer = interviewerEditText.text.toString().trim()
        val notes = notesEditText.text.toString().trim()
        
        // Schedule the interview using ViewModel
        interviewViewModel.scheduleInterview(
            applicantId = selectedApplicantId!!,
            jobId = selectedJobId!!,
            interviewerIds = listOf(interviewer), // Store the interviewer name as an ID for simplicity
            scheduledDate = selectedDate!!,
            duration = duration,
            location = location,
            meetingLink = meetingLink,
            notes = notes
        )
        
        // Show success message
        Toast.makeText(context, "Interview scheduled successfully", Toast.LENGTH_SHORT).show()
        
        // Navigate back
        findNavController().navigateUp()
    }
    
    private fun getDurationValue(): Int {
        val durationText = durationSpinner.selectedItem.toString()
        return durationText.split(" ")[0].toInt() // Extract number from "XX minutes"
    }
    
    private fun loadJobData() {
        // Observe selected job from ViewModel
        jobViewModel.selectedJob.observe(viewLifecycleOwner) { job ->
            if (job != null) {
                jobTitleText.text = job.title
                jobDepartmentText.text = job.department ?: "Not specified"
                jobUniversityText.text = job.universityName
                
                selectedJobId = job.id
                selectedUniversityId = job.universityId
                
                // If we have an applicant ID, load their data
                if (selectedApplicantId != null) {
                    loadApplicantData(job.universityId, job.id, selectedApplicantId!!)
                }
            }
        }
    }
    
    private fun handleArguments() {
        // Try to get applicant ID from arguments
        arguments?.getString("applicantId")?.let { applicantId ->
            selectedApplicantId = applicantId
            Log.d(TAG, "Received applicantId from arguments: $applicantId")
            
            val jobId = arguments?.getString("jobId")
            val universityId = arguments?.getString("universityId")
            
            if (jobId != null && universityId != null) {
                selectedJobId = jobId
                selectedUniversityId = universityId
                
                // Load applicant data with the provided IDs
                loadApplicantData(universityId, jobId, applicantId)
            }
        }
    }
    
    private fun loadApplicantData(universityId: String, jobId: String, applicantId: String) {
        // Use a coroutine to fetch the applicant data from Firestore
        CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "Attempting to load applicant data with ID: $applicantId")
                val applicantDoc = getApplicantDocument(universityId, jobId, applicantId)
                
                if (applicantDoc != null) {
                    // Extract data from the document
                    val name = applicantDoc.getString("name") ?: "Unknown Applicant"
                    val email = applicantDoc.getString("email") ?: ""
                    val matchPercentage = applicantDoc.getString("matchPercentage") ?: "N/A"
                    val recommendation = applicantDoc.getString("recommendation") ?: "N/A"
                    val education = applicantDoc.getString("education") ?: "Not specified"
                    val experience = applicantDoc.getString("experience") ?: "Not specified"
                    
                    Log.d(TAG, "Found applicant: $name, email: $email")
                    
                    // Update UI with applicant data
                    applicantNameText.text = name
                    applicantEmailText.text = email
                    matchPercentageText.text = matchPercentage
                    recommendationText.text = recommendation
                    educationText.text = education
                    experienceText.text = experience
                    
                    // Store the applicant ID
                    selectedApplicantId = applicantDoc.id // Use the actual ID from the document
                    Log.d(TAG, "Using document ID as applicantId: ${applicantDoc.id}")
                } else {
                    // Handle case when applicant document not found
                    Log.e(TAG, "Applicant data not found with ID: $applicantId")
                    Toast.makeText(context, "Applicant data not found", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading applicant data: ${e.message}", e)
                Toast.makeText(context, "Error loading applicant data", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private suspend fun getApplicantDocument(universityId: String, jobId: String, applicantId: String): DocumentSnapshot? {
        return withContext(Dispatchers.IO) {
            try {
                // Try with the provided applicant ID
                var document = firestore.collection("universities")
                    .document(universityId)
                    .collection("jobs")
                    .document(jobId)
                    .collection("applicants")
                    .document(applicantId)
                    .get()
                    .await()
                
                // If document doesn't exist, it might be because the ID format is different
                if (!document.exists() && applicantId.contains("@")) {
                    // Try with sanitized email format (replace @ and . with -)
                    val sanitizedId = applicantId.replace("@", "-").replace(".", "-")
                    document = firestore.collection("universities")
                        .document(universityId)
                        .collection("jobs")
                        .document(jobId)
                        .collection("applicants")
                        .document(sanitizedId)
                        .get()
                        .await()
                }
                
                // If still not found and applicant ID has dashes, it might be a sanitized email
                if (!document.exists() && applicantId.contains("-") && !applicantId.contains("@")) {
                    // Try to convert from sanitized to original format
                    val parts = applicantId.split("-")
                    if (parts.size >= 2) {
                        val possibleEmail = parts[0] + "@" + parts.subList(1, parts.size).joinToString(".")
                        document = firestore.collection("universities")
                            .document(universityId)
                            .collection("jobs")
                            .document(jobId)
                            .collection("applicants")
                            .document(possibleEmail)
                            .get()
                            .await()
                    }
                }
                
                if (document.exists()) {
                    return@withContext document
                }
                
                return@withContext null
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching applicant document: ${e.message}", e)
                return@withContext null
            }
        }
    }
    
    companion object {
        fun newInstance(applicantId: String, jobId: String, universityId: String): ScheduleInterviewFragment {
            val fragment = ScheduleInterviewFragment()
            val args = Bundle()
            args.putString("applicantId", applicantId)
            args.putString("jobId", jobId)
            args.putString("universityId", universityId)
            fragment.arguments = args
            return fragment
        }
    }
} 