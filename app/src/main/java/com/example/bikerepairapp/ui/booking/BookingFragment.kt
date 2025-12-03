package com.example.bikerepairapp.ui.booking

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton

class BookingFragment : Fragment(R.layout.fragment_booking) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val confirmButton = view.findViewById<MaterialButton>(R.id.btnConfirm)

        confirmButton?.setOnClickListener {
            Toast.makeText(
                requireContext(),
                "Your bike repair is booked! ✅",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
