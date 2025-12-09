package com.example.bikerepairapp.ui.booking

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton

class BookingFragment : Fragment(R.layout.fragment_booking) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateTimeInput = view.findViewById<EditText>(R.id.etDateTime)
        val locationInput = view.findViewById<EditText>(R.id.etLocation)
        val descInput = view.findViewById<EditText>(R.id.etDescription)
        val confirmButton = view.findViewById<MaterialButton>(R.id.btnConfirm)

        val statusTitle = view.findViewById<TextView>(R.id.tvStatusTitle)
        val statusDetails = view.findViewById<TextView>(R.id.tvStatusDetails)

        confirmButton?.setOnClickListener {
            val whenText = dateTimeInput.text.toString()
            val whereText = locationInput.text.toString()
            val issueText = descInput.text.toString()

            // Visa status-sektionen
            statusTitle.visibility = View.VISIBLE
            statusDetails.visibility = View.VISIBLE

            statusDetails.text = """
            Issue: $issueText
            Time: $whenText
            Location: $whereText
        
            Status: Requested
            Mechanic: Not assigned yet
    """.trimIndent()

            Toast.makeText(requireContext(), "Booking created ✅", Toast.LENGTH_SHORT).show()
        }

    }
}
