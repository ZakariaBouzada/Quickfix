package com.example.bikerepairapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.LoginActivity
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameView = view.findViewById<TextView>(R.id.tvUserName)
        val emailView = view.findViewById<TextView>(R.id.tvUserEmail)
        val repairsView = view.findViewById<TextView>(R.id.tvCompletedRepairs)
        val ratingView = view.findViewById<TextView>(R.id.tvRating)
        val activityView = view.findViewById<TextView>(R.id.tvActivity)
        val logoutBtn = view.findViewById<MaterialButton>(R.id.btnLogout)

        val user = auth.currentUser
        if (user == null) {
            // ingen inloggad -> tillbaka till login
            goToLogin()
            return
        }

        val uid = user.uid

        // 1) Hämta kundprofilen (name, email, ev. rating) från Firestore
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val name = snap.getString("name") ?: user.displayName ?: "Customer"
                val email = snap.getString("email") ?: user.email ?: ""
                val rating = snap.getDouble("rating") ?: 0.0

                nameView.text = name
                emailView.text = email
                ratingView.text = "Customer rating: ${if (rating == 0.0) "–" else "$rating★"}"
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Could not load profile", Toast.LENGTH_SHORT).show()
            }

        // 2) Hämta kundens bookings från Firestore (collection 'bookings')
        db.collection("bookings")
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { snap ->
                val count = snap.size()
                repairsView.text = "Your repairs: $count"

                val last = snap.documents.firstOrNull()
                if (last != null) {
                    val issue = last.getString("issue") ?: "Unknown issue"
                    val location = last.getString("location") ?: "Unknown location"
                    val status = last.getString("status") ?: "Unknown status"

                    activityView.text = "Last booking: $issue at $location\nStatus: $status"
                } else {
                    activityView.text = "No bookings yet"
                }
            }
            .addOnFailureListener {
                activityView.text = "Could not load booking history"
            }

        // 3) Logout-knapp
        logoutBtn.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    private fun goToLogin() {
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
