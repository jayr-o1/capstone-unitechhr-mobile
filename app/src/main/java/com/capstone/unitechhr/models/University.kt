package com.capstone.unitechhr.models

import java.util.Date

data class University(
    val id: String = "",
    val code: String = "",
    val name: String = "",
    val createdAt: Date = Date(),
    val createdBy: String = ""
) 