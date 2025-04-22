package com.capstone.unitechhr.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.capstone.unitechhr.R
import com.capstone.unitechhr.models.Applicant
import com.capstone.unitechhr.models.ApplicationStatus
import com.capstone.unitechhr.viewmodels.ApplicantViewModel
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import java.util.Date

class ApplicantFormFragment : Fragment() {
    
    private lateinit var viewModel: ApplicantViewModel
    
    // Form fields
    private lateinit var firstNameInput: TextInputEditText
    private lateinit var lastNameInput: TextInputEditText
    private lateinit var emailInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var positionInput: TextInputEditText
    private lateinit var skillsInput: TextInputEditText
    private lateinit var skillsChipGroup: ChipGroup
    private lateinit var saveButton: Button
    
    private val skills = mutableListOf<String>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_applicant_form, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[ApplicantViewModel::class.java]
        
        // Set up toolbar
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = viewModel.selectedApplicant.value?.let { "Edit Applicant" } ?: "Add Applicant"
            setDisplayHomeAsUpEnabled(true)
        }
        
        // Initialize views
        initializeViews(view)
        
        // Set up listeners
        setupListeners()
        
        // Populate form if editing an existing applicant
        viewModel.selectedApplicant.value?.let { applicant ->
            populateForm(applicant)
        }
    }
    
    private fun initializeViews(view: View) {
        firstNameInput = view.findViewById(R.id.first_name_input)
        lastNameInput = view.findViewById(R.id.last_name_input)
        emailInput = view.findViewById(R.id.email_input)
        phoneInput = view.findViewById(R.id.phone_input)
        positionInput = view.findViewById(R.id.position_input)
        skillsInput = view.findViewById(R.id.skills_input)
        skillsChipGroup = view.findViewById(R.id.skills_chip_group)
        saveButton = view.findViewById(R.id.save_button)
    }
    
    private fun setupListeners() {
        // Add skill chip when user enters a skill
        view?.findViewById<Button>(R.id.add_skill_button)?.setOnClickListener {
            val skillText = skillsInput.text.toString().trim()
            if (skillText.isNotEmpty()) {
                addSkillChip(skillText)
                skillsInput.text?.clear()
            }
        }
        
        // Save applicant
        saveButton.setOnClickListener {
            saveApplicant()
        }
    }
    
    private fun addSkillChip(skill: String) {
        if (!skills.contains(skill)) {
            skills.add(skill)
            
            val chip = Chip(requireContext()).apply {
                text = skill
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    skillsChipGroup.removeView(this)
                    skills.remove(skill)
                }
            }
            
            skillsChipGroup.addView(chip)
        }
    }
    
    private fun populateForm(applicant: Applicant) {
        firstNameInput.setText(applicant.firstName)
        lastNameInput.setText(applicant.lastName)
        emailInput.setText(applicant.email)
        phoneInput.setText(applicant.phone)
        positionInput.setText(applicant.appliedPosition)
        
        skills.clear()
        skillsChipGroup.removeAllViews()
        applicant.skills.forEach { skill ->
            addSkillChip(skill)
        }
    }
    
    private fun saveApplicant() {
        // Validate form
        if (!validateForm()) {
            return
        }
        
        // Get form data
        val firstName = firstNameInput.text.toString().trim()
        val lastName = lastNameInput.text.toString().trim()
        val email = emailInput.text.toString().trim()
        val phone = phoneInput.text.toString().trim()
        val position = positionInput.text.toString().trim()
        
        // Create or update applicant
        val applicant = viewModel.selectedApplicant.value?.copy(
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            appliedPosition = position,
            skills = skills.toList(),
            education = emptyList(),
            experience = emptyList(),
            applicationDate = Date(),
            status = ApplicationStatus.PENDING,
            resume = ""
        ) ?: Applicant(
            id = "",
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            appliedPosition = position,
            skills = skills.toList(),
            education = emptyList(),
            experience = emptyList(),
            applicationDate = Date(),
            status = ApplicationStatus.PENDING,
            resume = ""
        )
        
        // Save applicant
        viewModel.saveApplicant(applicant)
        
        // Show success message and navigate back
        Toast.makeText(requireContext(), "Applicant saved successfully", Toast.LENGTH_SHORT).show()
        findNavController().navigateUp()
    }
    
    private fun validateForm(): Boolean {
        var isValid = true
        
        if (firstNameInput.text.isNullOrBlank()) {
            firstNameInput.error = "First name is required"
            isValid = false
        }
        
        if (lastNameInput.text.isNullOrBlank()) {
            lastNameInput.error = "Last name is required"
            isValid = false
        }
        
        if (emailInput.text.isNullOrBlank()) {
            emailInput.error = "Email is required"
            isValid = false
        }
        
        if (positionInput.text.isNullOrBlank()) {
            positionInput.error = "Position is required"
            isValid = false
        }
        
        return isValid
    }
} 