package com.example.bikerepairapp.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bookNowButton =
            view.findViewById<MaterialButton>(R.id.btnHomeBookNow)

        bookNowButton.setOnClickListener {
            // navigate to booking tab/fragment
            findNavController().navigate(R.id.bookingFragment)
        }
    }
}
