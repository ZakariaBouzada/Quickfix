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
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.Locale

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Find all views from our new XML
        val nameView = view.findViewById<TextView>(R.id.tvUserName)
        val emailView = view.findViewById<TextView>(R.id.tvUserEmail)
        val repairsView = view.findViewById<TextView>(R.id.tvCompletedRepairs)
        val ratingView = view.findViewById<TextView>(R.id.tvRating)
        val activityView = view.findViewById<TextView>(R.id.tvActivity)
        val avatarIcon = view.findViewById<TextView>(R.id.tvAvatarIcon)
        val memberSinceView = view.findViewById<TextView>(R.id.tvMemberSince)
        val btnSettings = view.findViewById<MaterialButton>(R.id.btnSettings)
        val logoutBtn = view.findViewById<MaterialButton>(R.id.btnLogout)

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }

        val uid = user.uid

        // 1) Fetch User Profile (Role, Name, Member Since)
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snap ->
                val name = snap.getString("name") ?: user.displayName ?: "Customer"
                val email = snap.getString("email") ?: user.email ?: ""
                val rating = snap.getDouble("rating") ?: 0.0
                val role = snap.getString("role") ?: "customer"

                // Set Profile Info
                nameView.text = name
                emailView.text = email
                ratingView.text = if (rating == 0.0) "–" else "${String.format("%.1f", rating)}★"

                // Set Avatar based on role
                avatarIcon.text = if (role == "mechanic") "🔧" else "🚲"

                // Set Member Since (using Firestore 'createdAt' timestamp)
                val timestamp = snap.getTimestamp("createdAt")
                if (timestamp != null) {
                    val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                    memberSinceView.text = "Joined ${sdf.format(timestamp.toDate())}"
                }
            }

        // 2) Fetch Last Booking (Manual sort)
        db.collection("requests")
            .whereEqualTo("customerId", uid)
            .get()
            .addOnSuccessListener { snap ->
                if (!snap.isEmpty) {
                    // Use 'createdAt' because that matches your Firestore field name
                    val lastBooking = snap.documents
                        .filter { it.getTimestamp("createdAt") != null }
                        .maxByOrNull { it.getTimestamp("createdAt")!! }

                    if (lastBooking != null) {
                        val issue = lastBooking.getString("issue") ?: "No description"
                        val status = lastBooking.getString("status") ?: "pending"

                        // Use 'createdAt' for the date display
                        val timestamp = lastBooking.getTimestamp("createdAt")
                        val dateStr = if (timestamp != null) {
                            SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(timestamp.toDate())
                        } else {
                            "Recently"
                        }

                        activityView.text = "Last request: $issue\nDate: $dateStr\nStatus: ${status.uppercase()}"
                    } else {
                        activityView.text = "No recent activity"
                    }
                } else {
                    activityView.text = "No bookings found yet"
                }
            }
            .addOnFailureListener { e ->
                // Cleaned up to show the actual error if one occurs
                android.util.Log.e("ProfileFragment", "Firestore Error", e)
                activityView.text = "Could not load activity"
            }
        repairsView.text = "0" // Default value while loading

        // 3) Update Total Repairs count
        db.collection("requests")
            .whereEqualTo("customerId", uid)
            // Note: In your example, the status was "closed".
            // If you want to count finished jobs, make sure this matches your logic (e.g., "closed" or "completed")
            .whereIn("status", listOf("completed", "closed"))
            .get()
            .addOnSuccessListener { snap ->
                repairsView.text = snap.size().toString()
            }

        // 4) Button Logic
        btnSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Settings coming soon!", Toast.LENGTH_SHORT).show()
        }

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