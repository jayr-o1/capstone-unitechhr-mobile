package com.capstone.unitechhr.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.capstone.unitechhr.models.University

class UniversitySpinnerAdapter(
    context: Context,
    private val universities: List<University>
) : ArrayAdapter<University>(context, android.R.layout.simple_spinner_item, universities) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_item, parent, false)
        
        bindData(position, view)
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(android.R.layout.simple_spinner_dropdown_item, parent, false)
        
        bindData(position, view)
        return view
    }

    private fun bindData(position: Int, view: View) {
        val textView = view.findViewById<TextView>(android.R.id.text1)
        val university = getItem(position)
        textView.text = university?.name ?: "All Universities"
    }

    // Add a method to find the position of a university by ID
    fun getPositionById(universityId: String): Int {
        return universities.indexOfFirst { it.id == universityId }.takeIf { it >= 0 } ?: 0
    }
} 