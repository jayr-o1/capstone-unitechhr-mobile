package com.capstone.unitechhr.utils

import androidx.core.os.bundleOf
import androidx.navigation.NavController
import com.capstone.unitechhr.R

/**
 * Helper class for navigating to the employee onboarding screen
 */
object OnboardingNavigator {
    
    /**
     * Navigate from the onboarding list to the employee onboarding screen
     */
    fun navigateToEmployeeOnboarding(
        navController: NavController,
        employeeId: String,
        employeeName: String,
        employeePosition: String,
        collectionPath: String,
        startDate: String
    ) {
        val action = when {
            navController.currentDestination?.id == R.id.onboardingListFragment -> {
                R.id.action_onboardingListFragment_to_employeeOnboardingFragment
            }
            navController.currentDestination?.id == R.id.onboardingDetailFragment -> {
                R.id.action_onboardingDetailFragment_to_employeeOnboardingFragment
            }
            else -> null
        }
        
        // Create bundle of arguments
        val args = bundleOf(
            "employeeId" to employeeId,
            "employeeName" to employeeName,
            "employeePosition" to employeePosition,
            "collectionPath" to collectionPath,
            "startDate" to startDate
        )
        
        action?.let {
            navController.navigate(it, args)
        }
    }
} 