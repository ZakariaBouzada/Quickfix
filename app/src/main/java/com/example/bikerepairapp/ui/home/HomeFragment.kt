package com.example.bikerepairapp.ui.home

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookButton = view.findViewById<MaterialButton>(R.id.btnBook)

        bookButton?.setOnClickListener {
            // ✅ For now, just show a message instead of navigating
            Toast.makeText(
                requireContext(),
                "Booking feature coming soon 🚲",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
