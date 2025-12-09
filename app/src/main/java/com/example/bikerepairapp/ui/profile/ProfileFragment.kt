package com.example.bikerepairapp.ui.profile

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R

// Enkel modell som kan matchas mot backend senare
data class UserProfile(
    val name: String,
    val email: String,
    val completedRepairs: Int,
    val rating: Double,
    val lastActivity: String,
    val settingsSummary: String
)

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // TODO: senare ersätts detta med data från backend / ViewModel
        val demoProfile = UserProfile(
            name = "Levi Lähtinen",
            email = "levi@example.com",
            completedRepairs = 3,
            rating = 4.8,
            lastActivity = "Flat tire repair at Yliopistonkatu 4, Turku",
            settingsSummary = "Notifications: ON · Location sharing: ON"
        )

        view.findViewById<TextView>(R.id.tvUserName).text = demoProfile.name
        view.findViewById<TextView>(R.id.tvUserEmail).text = demoProfile.email
        view.findViewById<TextView>(R.id.tvCompletedRepairs).text =
            "Repairs: ${demoProfile.completedRepairs}"
        view.findViewById<TextView>(R.id.tvRating).text =
            "Rating: ${demoProfile.rating}★"
        view.findViewById<TextView>(R.id.tvActivity).text = demoProfile.lastActivity
        view.findViewById<TextView>(R.id.tvSettingsHint).text = demoProfile.settingsSummary
    }
}
