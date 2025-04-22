package com.capstone.unitechhr.utils

import java.util.Locale

/**
 * Capitalizes the first letter of a string
 * This replaces the deprecated String.capitalize() function
 */
fun String.capitalize(): String {
    if (isEmpty()) return this
    return this[0].uppercaseChar() + substring(1).lowercase(Locale.getDefault())
} 