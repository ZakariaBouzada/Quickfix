package com.example.bikerepairapp.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.bikerepairapp.LoginActivity
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private var userListener: ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val nameView = view.findViewById<TextView>(R.id.tvUserName)
        val emailView = view.findViewById<TextView>(R.id.tvUserEmail)
        val repairsView = view.findViewById<TextView>(R.id.tvCompletedRepairs)
        val logoutBtn = view.findViewById<MaterialButton>(R.id.btnLogout)
        val avatarView = view.findViewById<ImageView>(R.id.ivAvatar)

        val user = auth.currentUser
        if (user == null) {
            goToLogin()
            return
        }
        val uid = user.uid

        // Settings icon (top-right)
        view.findViewById<View?>(R.id.btnOpenSettings)?.setOnClickListener {
            findNavController().navigate(R.id.settingsFragment)
        }

        // 1) Live profile updates from Firestore users/{uid}
        // This will automatically refresh when you update photoUrl in Settings.
        userListener?.remove()
        userListener = db.collection("users").document(uid)
            .addSnapshotListener { snap, e ->
                if (e != null) {
                    // Don’t spam toasts every time; only show fallback values
                    nameView.text = user.displayName ?: "Customer"
                    emailView.text = user.email ?: ""
                    avatarView.setImageResource(R.drawable.ic_person)
                    return@addSnapshotListener
                }

                if (snap == null || !snap.exists()) {
                    nameView.text = user.displayName ?: "Customer"
                    emailView.text = user.email ?: ""
                    avatarView.setImageResource(R.drawable.ic_person)
                    return@addSnapshotListener
                }

                val name = snap.getString("name") ?: user.displayName ?: "Customer"
                val email = snap.getString("email") ?: user.email ?: ""
                nameView.text = name
                emailView.text = email

                val photoUrl = snap.getString("photoUrl")
                if (!photoUrl.isNullOrBlank()) {
                    Glide.with(this)
                        .load(photoUrl)
                        .circleCrop()
                        .into(avatarView)
                } else {
                    avatarView.setImageResource(R.drawable.ic_person)
                }
            }

        // 2) Count requests (simple MVP)
        db.collection("requests")
            .whereEqualTo("customerId", uid)
            .get()
            .addOnSuccessListener { snap ->
                repairsView.text = "Your repairs: ${snap.size()}"
            }
            .addOnFailureListener {
                // ignore quietly
            }

        // 3) Logout
        logoutBtn.setOnClickListener {
            auth.signOut()
            goToLogin()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        userListener?.remove()
        userListener = null
    }

    private fun goToLogin() {
        Toast.makeText(requireContext(), "Please log in again", Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
    }
}
