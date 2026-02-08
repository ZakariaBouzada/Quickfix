package com.example.bikerepairapp.ui.home

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.findNavController

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // These are your "button-cards" in fragment_home.xml
        val quickBook = view.findViewById<View>(R.id.cardQuickBook)
        val quickRepairs = view.findViewById<View>(R.id.cardQuickRepairs)
        val quickHelp = view.findViewById<View>(R.id.cardQuickHelp)

        // Grab bottom nav from activity_main.xml
        val bottomNav = requireActivity().findViewById<BottomNavigationView>(R.id.bottomNavigationView)

        // ✅ Switch tabs by selecting bottom nav item (NOT navController.navigate)
        quickBook.setOnClickListener {
            bottomNav.selectedItemId = R.id.bookingFragment
        }

        quickRepairs.setOnClickListener {
            // If "My repairs" is in your Booking tab flow, switch there.
            // If you later make Repairs its own bottom tab, change this to that tab id.
            bottomNav.selectedItemId = R.id.bookingFragment

            // Optional: if you want it to directly open the repairs screen inside booking:
            // findNavController().navigate(R.id.customerRepairsFragment)
            // BUT only do this if customerRepairsFragment is reachable from the current graph setup.
        }

        quickHelp.setOnClickListener {
            // If Help is its own fragment in nav_graph:
            findNavController().navigate(R.id.helpFragment)

            // If you haven’t created Help yet, do nothing or show a toast later.
            // For now I'll just switch to Profile as placeholder (remove if you want):
            // bottomNav.selectedItemId = R.id.profileFragment
        }
    }
}
