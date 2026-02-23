package com.example.bikerepairapp.ui.booking

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bikerepairapp.R
import com.example.bikerepairapp.notifications.NotificationBadgeManager
import com.google.android.material.button.MaterialButton
import com.example.bikerepairapp.ui.notifications.NotificationsBottomSheet


class BookingFragment : Fragment(R.layout.fragment_booking) {

    private val notifBadge = NotificationBadgeManager()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnOpenBookingForm = view.findViewById<MaterialButton>(R.id.btnOpenBookingForm)
        val btnOpenMyRepairs = view.findViewById<MaterialButton>(R.id.btnOpenMyRepairs)

        // Bell include (from fragment_booking.xml)

        val header = view.findViewById<View>(R.id.includeHeaderActions)

        header.findViewById<View>(R.id.btnMessages).setOnClickListener {
            com.example.bikerepairapp.ui.messages.MessagesBottomSheet()
                .show(parentFragmentManager, "messages")
        }


        val bellIncludeRoot = header.findViewById<View>(R.id.includeNotifBell) // only if you gave it an id
        notifBadge.bind(
            includeRoot = bellIncludeRoot,
            lifecycleOwner = viewLifecycleOwner,
            onBellClick = {
                NotificationsBottomSheet().show(parentFragmentManager, "notifications")
            },
            // IMPORTANT: don't clear badge until sheet actually opens
            markSeenOnClick = false
        )

        btnOpenBookingForm.setOnClickListener {
            findNavController().navigate(R.id.action_bookingFragment_to_vehiclePickerFragment)
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
