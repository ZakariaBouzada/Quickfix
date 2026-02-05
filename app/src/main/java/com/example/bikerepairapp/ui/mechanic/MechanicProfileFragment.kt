package com.example.bikerepairapp.ui.mechanic

import android.content.Intent
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvName = view.findViewById<TextView>(R.id.tvMechanicName)
        val tvEmail = view.findViewById<TextView>(R.id.tvMechanicEmail)
        val tvRating = view.findViewById<TextView>(R.id.tvMechanicRating)
        val tvStats = view.findViewById<TextView>(R.id.tvMechanicStats)

        val btnCompleted = view.findViewById<MaterialButton>(R.id.btnOpenCompleted)
        val btnLogout = view.findViewById<MaterialButton>(R.id.btnMechanicLogout)

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        // 1) Load mechanic profile fields (name/email/rating) from users/{uid}
        db.collection("users").document(user.uid).get()
            .addOnSuccessListener { snap ->
                val name = snap.getString("name") ?: "Mechanic"
                val email = snap.getString("email") ?: (user.email ?: "")
                val rating = snap.getDouble("rating") ?: 0.0

                tvName.text = name
                tvEmail.text = email
                tvRating.text = if (rating <= 0.0) "Rating: –" else "Rating: ${"%.1f".format(rating)} ★"
            }
            .addOnFailureListener {
                tvName.text = "Mechanic"
                tvEmail.text = user.email ?: ""
                tvRating.text = "Rating: –"
                Toast.makeText(requireContext(), "Could not load profile", Toast.LENGTH_SHORT).show()
            }

        // 2) UI stats (active + completed) - read-only display
        loadStats(user.uid, tvStats)

        // Navigate to completed list
        btnCompleted.setOnClickListener {
            // if you added an action, you can use it; otherwise this ID works too
            findNavController().navigate(R.id.mechanicCompletedFragment)
        }

        // Logout
        btnLogout.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun loadStats(uid: String, tvStats: TextView) {
        // active = accepted, completed = closed (based on your current status naming)
        db.collection("requests")
            .whereEqualTo("mechanicId", uid)
            .get()
            .addOnSuccessListener { snap ->
                var active = 0
                var completed = 0

                for (doc in snap.documents) {
                    when (doc.getString("status")) {
                        "accepted" -> active++
                        "closed" -> completed++
                    }
                }

                tvStats.text = "$active active • $completed completed"
            }
            .addOnFailureListener {
                tvStats.text = "–"
            }
    }

    private fun goToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
