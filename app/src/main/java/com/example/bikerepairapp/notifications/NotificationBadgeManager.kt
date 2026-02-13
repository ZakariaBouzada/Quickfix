package com.example.bikerepairapp.notifications

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import com.example.bikerepairapp.R
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class NotificationBadgeManager(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    private var userListener: ListenerRegistration? = null
    private var reqListener: ListenerRegistration? = null

    /**
     * Bind bell include view to Firestore unread logic.
     *
     * includeRoot must be the <include ...> root view (FrameLayout from view_notification_bell.xml)
     * lifecycleOwner: fragment's viewLifecycleOwner
     * onBellClick: what you want to do when bell is tapped (open screen, etc.)
     */
    fun bind(
        includeRoot: View,
        lifecycleOwner: LifecycleOwner,
        onBellClick: (() -> Unit)? = null,
        markSeenOnClick: Boolean = false
    ) {
        val uid = auth.currentUser?.uid ?: run {
            // Not logged in => hide badge
            includeRoot.findViewById<TextView>(R.id.tvNotifBadge)?.visibility = View.GONE
            return
        }

        val btnBell = includeRoot.findViewById<ImageButton>(R.id.btnNotifications)
        val tvBadge = includeRoot.findViewById<TextView>(R.id.tvNotifBadge)

        btnBell.setOnClickListener {
            if (markSeenOnClick) {
                firestore.collection("users").document(uid)
                    .update("customerLastSeenNotificationsAt", FieldValue.serverTimestamp())
            }
            onBellClick?.invoke()
        }

        // Listen for lastSeen updates on user doc
        userListener?.remove()
        userListener = firestore.collection("users").document(uid)
            .addSnapshotListener { userSnap, _ ->
                if (userSnap == null) return@addSnapshotListener
                val lastSeen = userSnap.getTimestamp("customerLastSeenNotificationsAt")

                // Listen to requests for this customer and compute unread count
                reqListener?.remove()
                reqListener = firestore.collection("requests")
                    .whereEqualTo("customerId", uid)
                    .addSnapshotListener { snap, _ ->
                        if (snap == null) return@addSnapshotListener

                        val unread = snap.documents.count { d ->
                            isUnreadUpdate(d, lastSeen)
                        }

                        if (unread > 0) {
                            tvBadge.visibility = View.VISIBLE
                            tvBadge.text = if (unread > 99) "99+" else unread.toString()
                        } else {
                            tvBadge.visibility = View.GONE
                        }
                        println("Requests size: ${snap?.size()}")
                        println("Unread count: $unread")
                    }
            }

        // Auto-cleanup when lifecycle is destroyed
        // (still good practice to call unbind() in onDestroyView, but this helps)
        lifecycleOwner.lifecycle.addObserver(SimpleLifecycleObserver(onDestroy = { unbind() }))
    }

    fun unbind() {
        userListener?.remove()
        userListener = null
        reqListener?.remove()
        reqListener = null
    }

    private fun isUnreadUpdate(doc: DocumentSnapshot, lastSeen: Timestamp?): Boolean {
        val status = doc.getString("status") ?: return false

        return when (status) {
            "accepted" -> {
                val t = doc.getTimestamp("acceptedAt")
                lastSeen == null || (t != null && t > lastSeen)
            }
            "completed_pending" -> {
                val t = doc.getTimestamp("completedAt")
                lastSeen == null || (t != null && t > lastSeen)
            }
            else -> false
        }
    }
}
