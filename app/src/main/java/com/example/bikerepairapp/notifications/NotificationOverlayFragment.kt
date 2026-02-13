package com.example.bikerepairapp.notifications

import android.os.Bundle
import android.view.View
import android.view.ViewAnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.hypot

class NotificationOverlayFragment : androidx.fragment.app.DialogFragment(R.layout.fragment_notification_overlay) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: NotificationAdapter
    override fun onStart() {
        super.onStart()
        // This makes the dialog cover the whole screen so our dim background works
        dialog?.window?.setLayout(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val panel = view.findViewById<View>(R.id.notificationPanel)
        val rv = view.findViewById<RecyclerView>(R.id.rvNotifications)

        // SETUP ADAPTER ONCE WITH THE CLICK LOGIC
        adapter = NotificationAdapter { requestId ->
            val bundle = Bundle().apply {
                putString("highlightRequestId", requestId)
            }
            // Closes the dialog first, then navigates
            findNavController().popBackStack()
            findNavController().navigate(R.id.customerRepairsFragment, bundle)
        }

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        loadNotifications()

        // --- Circular Reveal Animation ---
        panel.visibility = View.INVISIBLE
        view.post {
            // Logic remains same as your snippet
            val finalWidth = panel.width
            val finalHeight = panel.height
            val anim = ViewAnimationUtils.createCircularReveal(
                panel, finalWidth, 0, 0f,
                hypot(finalWidth.toDouble(), finalHeight.toDouble()).toFloat()
            )
            panel.visibility = View.VISIBLE
            anim.duration = 400
            anim.start()
        }

        // Navigation fix for clicking outside (the dim area) to close
        view.setOnClickListener { findNavController().popBackStack() }
        panel.setOnClickListener { /* Do nothing to prevent clicks on panel closing it */ }
    }

    private fun loadNotifications() {
        val uid = auth.currentUser?.uid ?: return
        val twentyFourHoursAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)

        db.collection("requests")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snap, _ ->
                val notifs = snap?.documents?.mapNotNull { d ->
                    val status = d.getString("status")
                    val issue = d.getString("issue") ?: "Repair"

                    // 24-Hour Expiry Logic:
                    val timestamp = when (status) {
                        "accepted" -> d.getTimestamp("acceptedAt")
                        "completed_pending" -> d.getTimestamp("completedAt")
                        else -> null
                    }

                    // If notification is older than 24 hours, don't show it
                    if (timestamp != null && timestamp.toDate().time < twentyFourHoursAgo) {
                        return@mapNotNull null
                    }

                    when (status) {
                        "accepted" -> NotificationItem(d.id, "Accepted: $issue", "Mechanic is on it!")
                        "completed_pending" -> NotificationItem(d.id, "Finished: $issue", "Tap to confirm")
                        else -> null
                    }
                } ?: emptyList()
                adapter.submitList(notifs)
            }
    }
}