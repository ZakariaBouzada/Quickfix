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

    fun bind(
        includeRoot: View,
        lifecycleOwner: LifecycleOwner,
        onBellClick: (() -> Unit)? = null,
        markSeenOnClick: Boolean = true
    ) {
        val uid = auth.currentUser?.uid ?: run {
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

        userListener?.remove()
        userListener = firestore.collection("users").document(uid)
            .addSnapshotListener { userSnap, _ ->
                if (userSnap == null) return@addSnapshotListener

                val lastSeen = userSnap.getTimestamp("customerLastSeenNotificationsAt")
                val dismissed = (userSnap.get("customerDismissedNotifications") as? List<*>)
                    ?.mapNotNull { it as? String }
                    ?.toSet()
                    ?: emptySet()

                reqListener?.remove()
                reqListener = firestore.collection("requests")
                    .whereEqualTo("customerId", uid)
                    .addSnapshotListener { snap, _ ->
                        if (snap == null) return@addSnapshotListener

                        val unread = snap.documents.sumOf { d ->
                            countUnreadEvents(d, lastSeen, dismissed)
                        }

                        if (unread > 0) {
                            tvBadge.visibility = View.VISIBLE
                            tvBadge.text = if (unread > 99) "99+" else unread.toString()
                        } else {
                            tvBadge.visibility = View.GONE
                        }
                    }
            }

        lifecycleOwner.lifecycle.addObserver(SimpleLifecycleObserver(onDestroy = { unbind() }))
    }

    fun unbind() {
        userListener?.remove()
        userListener = null
        reqListener?.remove()
        reqListener = null
    }

    private fun countUnreadEvents(
        doc: DocumentSnapshot,
        lastSeen: Timestamp?,
        dismissed: Set<String>
    ): Int {
        val status = doc.getString("status") ?: return 0
        val requestId = doc.id

        fun isUnread(t: Timestamp?): Boolean {
            return lastSeen == null || (t != null && t > lastSeen)
        }

        var c = 0

        // accepted event
        if (status == "accepted" || status == "completed_pending" || status == "closed") {
            val t = doc.getTimestamp("acceptedAt")
            val notifId = "$requestId:accepted"
            if (notifId !in dismissed && isUnread(t)) c++
        }

        // completed_pending event
        if (status == "completed_pending" || status == "closed") {
            val t = doc.getTimestamp("completedAt")
            val notifId = "$requestId:completed"
            if (notifId !in dismissed && isUnread(t)) c++
        }

        return c
    }
}
