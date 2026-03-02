package com.example.bikerepairapp.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Locale

class NotificationsBottomSheet : BottomSheetDialogFragment() {

    override fun getTheme(): Int = R.style.ThemeOverlay_QuickFix_BottomSheet

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var userListener: ListenerRegistration? = null
    private var reqListener: ListenerRegistration? = null

    private lateinit var rv: RecyclerView
    private lateinit var tvEmpty: TextView

    private val adapter = NotificationsAdapter()

    override fun onDestroyView() {
        userListener?.remove()
        userListener = null
        reqListener?.remove()
        reqListener = null
        super.onDestroyView()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottomsheet_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rv = view.findViewById(R.id.rvNotifications)
        tvEmpty = view.findViewById(R.id.tvEmptyNotifications)

        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = adapter

        attachSwipeToDismiss()

        val uid = auth.currentUser?.uid ?: run {
            showEmpty("Not logged in.")
            return
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

                        val items = snap.documents.flatMap { d ->
                            buildNotificationItemsFromRequest(d.id, d, lastSeen, dismissed)
                        }.sortedByDescending { it.sortTimeMillis }

                        if (items.isEmpty()) showEmpty("No notifications yet.")
                        else {
                            tvEmpty.visibility = View.GONE
                            rv.visibility = View.VISIBLE
                            adapter.submit(items)
                        }
                    }
            }
    }

    private fun attachSwipeToDismiss() {
        val cb = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                if (pos == RecyclerView.NO_POSITION) return

                val item = adapter.getItem(pos)
                adapter.removeAt(pos)

                // Persist dismissal so it stays gone after refresh
                dismissNotification(item.id)

                // If list becomes empty, show empty state
                if (adapter.itemCount == 0) showEmpty("No notifications yet.")
            }
        }

        ItemTouchHelper(cb).attachToRecyclerView(rv)
    }

    private fun dismissNotification(notificationId: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("customerDismissedNotifications", FieldValue.arrayUnion(notificationId))
    }

    private fun showEmpty(msg: String) {
        rv.visibility = View.GONE
        tvEmpty.visibility = View.VISIBLE
        tvEmpty.text = msg
    }

    private fun buildNotificationItemsFromRequest(
        requestId: String,
        doc: com.google.firebase.firestore.DocumentSnapshot,
        lastSeen: Timestamp?,
        dismissed: Set<String>
    ): List<NotificationUi> {
        val issue = doc.getString("issue") ?: "Repair update"
        val mechanicName = doc.getString("mechanicName")
        val status = doc.getString("status") ?: return emptyList()

        fun isUnread(t: Timestamp?): Boolean {
            return lastSeen == null || (t != null && t > lastSeen)
        }

        val items = mutableListOf<NotificationUi>()

        // Accepted event
        if (status == "accepted" || status == "completed_pending" || status == "closed") {
            val t = doc.getTimestamp("acceptedAt")
            if (t != null) {
                val id = "$requestId:accepted"
                if (id !in dismissed) {
                    items.add(
                        NotificationUi(
                            id = id,
                            title = "Mechanic accepted \u2705",
                            body = buildString {
                                append(issue)
                                if (!mechanicName.isNullOrBlank()) append("\nBy $mechanicName")
                            },
                            timeLabel = formatTime(t),
                            isUnread = isUnread(t),
                            sortTimeMillis = t.toDate().time,
                            requestId = requestId
                        )
                    )
                }
            }
        }

        // Mechanic on the way (location sharing active)
        val locationSharingActive = doc.getBoolean("locationSharingActive") ?: false
        if (locationSharingActive && status == "accepted") {
            val mechanicLocationUpdatedAt = doc.getTimestamp("mechanicLocationUpdatedAt")
            val acceptedAt = doc.getTimestamp("acceptedAt")
            val t = mechanicLocationUpdatedAt ?: acceptedAt
            if (t != null) {
                val id = "$requestId:on_the_way"
                if (id !in dismissed) {
                    items.add(
                        NotificationUi(
                            id = id,
                            title = "Mechanic is on the way \uD83D\uDEE0\uFE0F",
                            body = buildString {
                                if (!mechanicName.isNullOrBlank()) append("$mechanicName is heading to you.\n")
                                append(issue)
                            },
                            timeLabel = formatTime(t),
                            isUnread = isUnread(t),
                            sortTimeMillis = t.toDate().time,
                            requestId = requestId
                        )
                    )
                }
            }
        }

        // Completed event
        if (status == "completed_pending" || status == "closed") {
            val t = doc.getTimestamp("completedAt")
            if (t != null) {
                val id = "$requestId:completed"
                if (id !in dismissed) {
                    items.add(
                        NotificationUi(
                            id = id,
                            title = "Repair completed 🔧",
                            body = "Waiting for your confirmation.\n$issue",
                            timeLabel = formatTime(t),
                            isUnread = isUnread(t),
                            sortTimeMillis = t.toDate().time,
                            requestId = requestId
                        )
                    )
                }
            }
        }

        return items
    }

    private fun formatTime(t: Timestamp): String {
        val sdf = SimpleDateFormat("EEE HH:mm", Locale.getDefault())
        return sdf.format(t.toDate())
    }
}
