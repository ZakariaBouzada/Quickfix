package com.example.bikerepairapp.ui.booking

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton
import com.example.bikerepairapp.data.database.AppDatabase
import com.example.bikerepairapp.data.model.BookingTicket
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch




class BookingFragment : Fragment(R.layout.fragment_booking) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dateTimeInput = view.findViewById<EditText>(R.id.etDateTime)
        val locationInput = view.findViewById<EditText>(R.id.etLocation)
        val descInput = view.findViewById<EditText>(R.id.etDescription)
        val confirmButton = view.findViewById<MaterialButton>(R.id.btnConfirm)

        val myBookingHeader = view.findViewById<TextView>(R.id.tvMyBookingHeader)
        val statusTitle = view.findViewById<TextView>(R.id.tvStatusTitle)
        val statusDetails = view.findViewById<TextView>(R.id.tvStatusDetails)

        //NEW req 1.1.2 bookingticket...
        val db = AppDatabase.getDatabase(requireContext())
        val ticketDao = db.ticketDao()

        // LOAD LAST BOOKING WHEN FRAGMENT OPENS
        lifecycleScope.launch {
            val latestTicket = ticketDao.getLatestTicket()
            if (latestTicket != null) {
                showStatus(myBookingHeader, statusTitle, statusDetails, latestTicket)
            }
        }

        confirmButton.setOnClickListener {
            val whenText = dateTimeInput.text.toString()
            val whereText = locationInput.text.toString()
            val issueText = descInput.text.toString()


            // Optional: light validation (does not change UX)
            if (whenText.isBlank() || whereText.isBlank() || issueText.isBlank()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // NEW: create ticket object
            val ticket = BookingTicket(
                id = 0,
                issue = issueText,
                date = whenText,
                location = whereText,
                status = "Requested"
            )

            // NEW: save to database
            lifecycleScope.launch {
                db.ticketDao().insertTicket(ticket)
                showStatus(myBookingHeader, statusTitle, statusDetails, ticket)
                //val allTickets = db.ticketDao().getAllTickets()
                // Log.d("BookingFragment", "All tickets: $allTickets")
            }
           /* // Visa status-sektionen
            statusTitle.visibility = View.VISIBLE
            statusDetails.visibility = View.VISIBLE

            statusDetails.text = """
            Issue: $issueText
            Time: $whenText
            Location: $whereText

            Status: Requested
            Mechanic: Not assigned yet
    """.trimIndent()*/


            Toast.makeText(requireContext(), "Booking created ✅", Toast.LENGTH_SHORT).show()

        }

    }
    private fun showStatus(
        header: TextView,
        title: TextView,
        details: TextView,
        ticket: BookingTicket
    ) {
        header.visibility = View.VISIBLE
        title.visibility = View.VISIBLE
        details.visibility = View.VISIBLE

        details.text = """
            id: ${ticket.id}
            Issue: ${ticket.issue}
            Time: ${ticket.date}
            Location: ${ticket.location}

            Status: ${ticket.status}
            Mechanic: Not assigned yet
        """.trimIndent()
    }
}
