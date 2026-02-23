package com.example.bikerepairapp.ui.booking

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bikerepairapp.R
import com.google.android.material.card.MaterialCardView

class VehiclePickerFragment : Fragment(R.layout.fragment_vehicle_picker) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btnBackVehiclePicker).setOnClickListener {
            findNavController().navigateUp()
        }

        view.findViewById<MaterialCardView>(R.id.cardBike).setOnClickListener {
            // Bike goes to the existing BookingFormFragment (unchanged)
            findNavController().navigate(R.id.action_vehiclePickerFragment_to_bookingFormFragment)
        }

        view.findViewById<MaterialCardView>(R.id.cardCar).setOnClickListener {
            findNavController().navigate(
                R.id.action_vehiclePickerFragment_to_vehicleBookingFormFragment,
                bundleOf("vehicleType" to "Car")
            )
        }

        view.findViewById<MaterialCardView>(R.id.cardMotorbike).setOnClickListener {
            findNavController().navigate(
                R.id.action_vehiclePickerFragment_to_vehicleBookingFormFragment,
                bundleOf("vehicleType" to "Motorbike")
            )
        }
    }
}
