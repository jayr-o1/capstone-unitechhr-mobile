package com.capstone.unitechhr.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for formatting date and time strings
 */
object TimeFormatter {
    
    private val dateTimeFormatter = SimpleDateFormat("MMMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
    private val dateFormatter = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    
    /**
     * Format a date with time
     * @param date The date to format
     * @return A formatted string like "April 23, 2023 at 10:00 AM"
     */
    fun formatDateWithTime(date: Date): String {
        return dateTimeFormatter.format(date)
    }
    
    /**
     * Format only the date part
     * @param date The date to format
     * @return A formatted string like "April 23, 2023"
     */
    fun formatDate(date: Date): String {
        return dateFormatter.format(date)
    }
    
    /**
     * Format only the time part
     * @param date The date to format
     * @return A formatted string like "10:00 AM"
     */
    fun formatTime(date: Date): String {
        return timeFormatter.format(date)
    }
} 