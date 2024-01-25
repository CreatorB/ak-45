package com.system.internet

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.graphics.Color

class ColorArrayAdapter(
    context: Context,
    private val resource: Int,
    private val items: List<String>
) : ArrayAdapter<String>(context, resource, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View = super.getView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)

        // Check if the position is a multiple of 5 (5, 10, 15, ...)
        if ((position + 1) % 1000 == 0) {
            textView.setTextColor(Color.RED) // Set text color to red
        }

        return view
    }
}