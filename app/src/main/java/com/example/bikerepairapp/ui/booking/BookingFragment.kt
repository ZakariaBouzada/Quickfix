package com.example.bikerepairapp.ui.booking

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bikerepairapp.R
import com.example.bikerepairapp.notifications.NotificationBadgeManager
import com.google.android.material.button.MaterialButton

class BookingFragment : Fragment(R.layout.fragment_booking) {

    private val notifBadge = NotificationBadgeManager()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnOpenBookingForm = view.findViewById<MaterialButton>(R.id.btnOpenBookingForm)
        val btnOpenMyRepairs = view.findViewById<MaterialButton>(R.id.btnOpenMyRepairs)

        // Bell include (from fragment_booking.xml)
        val bellInclude = view.findViewById<View>(R.id.includeNotifBell)
        notifBadge.bind(
            includeRoot = bellInclude,
            lifecycleOwner = viewLifecycleOwner,
            onBellClick = {
                // Later: navigate to a Notifications screen
                // For now: markSeenOnClick=true will clear the badge when tapped.
            },
            markSeenOnClick = true
        )

        btnOpenBookingForm.setOnClickListener {
            findNavController().navigate(R.id.action_bookingFragment_to_bookingFormFragment)
        }

        btnOpenMyRepairs.setOnClickListener {
            findNavController().navigate(R.id.action_bookingFragment_to_customerRepairsFragment)
        }
    }

    override fun onDestroyView() {
        notifBadge.unbind()
        super.onDestroyView()
    }
}
