package com.example.bikerepairapp.ui.mechanic

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bikerepairapp.LoginActivity
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MechanicProfileFragment : Fragment(R.layout.fragment_mechanic_profile) {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Currently selected vehicle prefs (toggled by buttons)
    private val selectedPrefs = mutableSetOf<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName    = view.findViewById<TextView>(R.id.tvMechanicName)
        val tvEmail   = view.findViewById<TextView>(R.id.tvMechanicEmail)
        val tvRating  = view.findViewById<TextView>(R.id.tvMechanicRating)
        val tvStats   = view.findViewById<TextView>(R.id.tvMechanicStats)

        val btnPrefBike  = view.findViewById<MaterialButton>(R.id.btnPrefBike)
        val btnPrefCar   = view.findViewById<MaterialButton>(R.id.btnPrefCar)
        val btnPrefMoto  = view.findViewById<MaterialButton>(R.id.btnPrefMoto)
        val btnSavePrefs = view.findViewById<MaterialButton>(R.id.btnSaveVehiclePrefs)
        val btnCompleted = view.findViewById<MaterialButton>(R.id.btnOpenCompleted)
        val btnLogout    = view.findViewById<MaterialButton>(R.id.btnMechanicLogout)

        val user = auth.currentUser
        if (user == null) { goToLogin(); return }

        // --- Load profile + saved prefs ---
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { snap ->
                tvName.text   = snap.getString("name") ?: "Mechanic"
                tvEmail.text  = snap.getString("email") ?: (user.email ?: "")
                val rating    = snap.getDouble("rating") ?: 0.0
                tvRating.text = if (rating <= 0.0) "Rating: –" else "Rating: ${"%.1f".format(rating)} ★"

                // Restore saved prefs (stored as list e.g. ["Bike","Car"])
                @Suppress("UNCHECKED_CAST")
                val saved = snap.get("preferredVehicles") as? List<String> ?: listOf("Bike", "Car", "Motorbike")
                selectedPrefs.clear()
                selectedPrefs.addAll(saved)
                applyPrefStyles(btnPrefBike, btnPrefCar, btnPrefMoto)
            }
            .addOnFailureListener {
                tvName.text   = "Mechanic"
                tvEmail.text  = user.email ?: ""
                tvRating.text = "Rating: –"
                // Default to all selected
                selectedPrefs.addAll(listOf("Bike", "Car", "Motorbike"))
                applyPrefStyles(btnPrefBike, btnPrefCar, btnPrefMoto)
            }

        loadStats(user.uid, tvStats)

        // --- Toggle buttons ---
        btnPrefBike.setOnClickListener {
            togglePref("Bike")
            applyPrefStyles(btnPrefBike, btnPrefCar, btnPrefMoto)
        }
        btnPrefCar.setOnClickListener {
            togglePref("Car")
            applyPrefStyles(btnPrefBike, btnPrefCar, btnPrefMoto)
        }
        btnPrefMoto.setOnClickListener {
            togglePref("Motorbike")
            applyPrefStyles(btnPrefBike, btnPrefCar, btnPrefMoto)
        }

        // --- Save to Firestore ---
        btnSavePrefs.setOnClickListener {
            if (selectedPrefs.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one vehicle type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            db.collection("users").document(user.uid)
                .update("preferredVehicles", selectedPrefs.toList())
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Preferences saved ✅", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Could not save: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnCompleted.setOnClickListener {
            findNavController().navigate(R.id.mechanicCompletedFragment)
        }

        btnLogout.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun togglePref(type: String) {
        if (selectedPrefs.contains(type)) {
            // Don't allow deselecting the last one
            if (selectedPrefs.size > 1) selectedPrefs.remove(type)
        } else {
            selectedPrefs.add(type)
        }
    }

    private fun applyPrefStyles(
        btnBike: MaterialButton,
        btnCar: MaterialButton,
        btnMoto: MaterialButton
    ) {
        val yellow = Color.parseColor("#FFFF00")
        val dark   = Color.parseColor("#202020")

        fun style(btn: MaterialButton, type: String) {
            val active = selectedPrefs.contains(type)
            btn.backgroundTintList = ColorStateList.valueOf(if (active) yellow else dark)
            btn.setTextColor(if (active) Color.BLACK else Color.WHITE)
            btn.strokeWidth = if (active) 0 else 2
            btn.strokeColor = ColorStateList.valueOf(yellow)
        }

        style(btnBike, "Bike")
        style(btnCar,  "Car")
        style(btnMoto, "Motorbike")
    }

    private fun loadStats(uid: String, tvStats: TextView) {
        db.collection("requests")
            .whereEqualTo("mechanicId", uid)
            .get()
            .addOnSuccessListener { snap ->
                var active = 0; var completed = 0
                for (doc in snap.documents) {
                    when (doc.getString("status")) {
                        "accepted" -> active++
                        "closed"   -> completed++
                    }
                }
                tvStats.text = "$active active • $completed completed"
            }
            .addOnFailureListener { tvStats.text = "–" }
    }

    private fun goToLogin() {
        startActivity(Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
    }
}
