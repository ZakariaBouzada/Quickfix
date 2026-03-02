package com.example.bikerepairapp.ui.mechanic

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.example.bikerepairapp.ui.messages.ChatBottomSheet


class MechanicRequestsFragment : Fragment(R.layout.fragment_mechanic_requests) {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private lateinit var incomingAdapter: IncomingRequestsAdapter
    private lateinit var activeAdapter: ActiveRequestsAdapter

    private var locationSharingService: LocationSharingService? = null

    /** Pending request to share location for (waiting on permission result) */
    private var pendingShareRequest: RepairRequest? = null

    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pendingShareRequest?.let { startLocationSharing(it) }
            } else {
                Toast.makeText(requireContext(), "Location permission required to share location", Toast.LENGTH_SHORT).show()
            }
            pendingShareRequest = null
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        locationSharingService = LocationSharingService(requireContext())

        val btnMessages = view.findViewById<android.widget.ImageButton>(R.id.btnMessages)
        btnMessages.setOnClickListener {
            com.example.bikerepairapp.ui.messages.MessagesBottomSheet()
                .show(parentFragmentManager, "messages")
        }

        val rvIncoming = view.findViewById<RecyclerView>(R.id.rvIncoming)
        val rvActive = view.findViewById<RecyclerView>(R.id.rvActive)

        val btnIncoming = view.findViewById<MaterialButton>(R.id.btnTabIncoming)
        val btnActive = view.findViewById<MaterialButton>(R.id.btnTabActive)

        incomingAdapter = IncomingRequestsAdapter(
            onAccept = { acceptRequest(it) },
            onReject = { rejectRequest(it) }
        )

        activeAdapter = ActiveRequestsAdapter(
            onComplete = { completeRequest(it) },
            onRelease = { releaseRequest(it) },
            onShareLocation = { requestLocationSharing(it) },
            onStopSharing = { stopLocationSharing(it) }
        )

        rvIncoming.layoutManager = LinearLayoutManager(requireContext())
        rvIncoming.adapter = incomingAdapter

        rvActive.layoutManager = LinearLayoutManager(requireContext())
        rvActive.adapter = activeAdapter

        // ---------- UI-only tab styling (same look as customer) ----------
        fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

        fun applyTabSelection(isIncoming: Boolean) {
            val blue = Color.parseColor("#4472C4")
            val dark = Color.parseColor("#202020")

            fun selected(btn: MaterialButton) {
                btn.backgroundTintList = ColorStateList.valueOf(blue)
                btn.setTextColor(Color.WHITE)
                btn.strokeWidth = 0
                btn.isAllCaps = false
            }

            fun unselected(btn: MaterialButton) {
                btn.backgroundTintList = ColorStateList.valueOf(dark)
                btn.setTextColor(Color.WHITE)
                btn.strokeColor = ColorStateList.valueOf(blue)
                btn.strokeWidth = dp(1)
                btn.isAllCaps = false
            }

            if (isIncoming) {
                selected(btnIncoming); unselected(btnActive)
            } else {
                selected(btnActive); unselected(btnIncoming)
            }
        }

        // Default state
        rvIncoming.visibility = View.VISIBLE
        rvActive.visibility = View.GONE
        applyTabSelection(isIncoming = true)

        btnIncoming.setOnClickListener {
            rvIncoming.visibility = View.VISIBLE
            rvActive.visibility = View.GONE
            applyTabSelection(isIncoming = true)
        }
        btnActive.setOnClickListener {
            rvIncoming.visibility = View.GONE
            rvActive.visibility = View.VISIBLE
            applyTabSelection(isIncoming = false)
        }

        listenForIncoming()
        listenForActive()
    }

    override fun onDestroyView() {
        // Stop location sharing when leaving the fragment
        locationSharingService?.stopSharing()
        locationSharingService = null
        super.onDestroyView()
    }

    // ---- Location sharing ----

    private fun requestLocationSharing(req: RepairRequest) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationSharing(req)
        } else {
            pendingShareRequest = req
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun startLocationSharing(req: RepairRequest) {
        val service = locationSharingService ?: return
        val success = service.startSharing(req.id)
        if (success) {
            activeAdapter.sharingRequestId = req.id
            activeAdapter.notifyDataSetChanged()
            Toast.makeText(requireContext(), "Sharing your location", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Could not start location sharing", Toast.LENGTH_SHORT).show()
        }
    }

    private fun stopLocationSharing(req: RepairRequest) {
        locationSharingService?.stopSharing()
        activeAdapter.sharingRequestId = null
        activeAdapter.notifyDataSetChanged()
        Toast.makeText(requireContext(), "Location sharing stopped", Toast.LENGTH_SHORT).show()
    }

    // ---- Existing methods ----

    private fun openChatForRequest(req: RepairRequest) {
        if (req.chatId.isNullOrBlank()) {
            Toast.makeText(requireContext(), "Accept the request to start chat", Toast.LENGTH_SHORT).show()
            return
        }
        openChat(req.chatId!!)
    }

    private fun currentUid(): String? = auth.currentUser?.uid

    private fun listenForIncoming() {
        val uid = currentUid() ?: return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { userSnap ->
                @Suppress("UNCHECKED_CAST")
                val prefs = userSnap.get("preferredVehicles") as? List<String>
                    ?: listOf("Bike", "Car", "Motorbike")

                startIncomingListener(uid, prefs)
            }
            .addOnFailureListener {
                startIncomingListener(uid, listOf("Bike", "Car", "Motorbike"))
            }
    }

    private fun startIncomingListener(uid: String, preferredVehicles: List<String>) {
        db.collection("requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { doc ->
                    val rejectedBy = doc.get("rejectedBy") as? List<*> ?: emptyList<Any>()
                    if (rejectedBy.contains(uid)) return@mapNotNull null

                    val mechanicId = doc.getString("mechanicId")
                    if (!mechanicId.isNullOrBlank()) return@mapNotNull null

                    val vehicleType = doc.getString("vehicleType") ?: "Bike"
                    if (!preferredVehicles.contains(vehicleType)) return@mapNotNull null

                    RepairRequest(
                        id = doc.id,
                        issue = doc.getString("issue") ?: return@mapNotNull null,
                        date = doc.getString("date") ?: "",
                        location = doc.getString("location") ?: "",
                        status = doc.getString("status") ?: "pending",
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        mechanicId = doc.getString("mechanicId"),
                        mechanicName = doc.getString("mechanicName"),
                        customerName = doc.getString("customerName") ?: "",
                        chatId = doc.getString("chatId"),
                        vehicleType = vehicleType,
                        locationGeo = doc.getGeoPoint("locationGeo")
                    )
                }

                incomingAdapter.submitList(list)
            }
    }

    private fun listenForActive() {
        val uid = currentUid() ?: return

        db.collection("requests")
            .whereEqualTo("mechanicId", uid)
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null) return@addSnapshotListener

                val list = snap.documents.mapNotNull { doc ->
                    val status = doc.getString("status") ?: "accepted"

                    if (status != "accepted" && status != "completed_pending") return@mapNotNull null

                    RepairRequest(
                        id = doc.id,
                        issue = doc.getString("issue") ?: return@mapNotNull null,
                        date = doc.getString("date") ?: "",
                        location = doc.getString("location") ?: "",
                        status = status,
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        mechanicId = doc.getString("mechanicId"),
                        mechanicName = doc.getString("mechanicName"),
                        customerName = doc.getString("customerName") ?: "",
                        chatId = doc.getString("chatId"),
                        vehicleType = doc.getString("vehicleType"),
                        locationGeo = doc.getGeoPoint("locationGeo"),
                        locationSharingActive = (doc.getBoolean("locationSharingActive") ?: false)
                    )
                }

                activeAdapter.submitList(list)
            }
    }

    /**
     * Accept request + ensure chat exists.
     */
    private fun acceptRequest(req: RepairRequest) {
        val mechanicId = currentUid() ?: return
        val mechanicName = auth.currentUser?.email ?: "mechanic"

        val reqRef = db.collection("requests").document(req.id)

        db.runTransaction { tx ->
            val reqSnap = tx.get(reqRef)
            val existingChatId = reqSnap.getString("chatId")

            val acceptedUpdate = mapOf(
                "status" to "accepted",
                "mechanicId" to mechanicId,
                "mechanicName" to mechanicName,
                "acceptedAt" to FieldValue.serverTimestamp()
            )

            if (!existingChatId.isNullOrBlank()) {
                tx.update(reqRef, acceptedUpdate)
                return@runTransaction existingChatId
            }

            val customerId = reqSnap.getString("customerId") ?: ""
            val chatRef = db.collection("chats").document()

            tx.set(chatRef, mapOf(
                "requestId" to req.id,
                "customerId" to customerId,
                "mechanicId" to mechanicId,
                "participants" to listOf(customerId, mechanicId),
                "createdAt" to FieldValue.serverTimestamp(),
                "lastMessage" to "Mechanic accepted \u2705",
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastSenderId" to mechanicId
            ))

            tx.update(reqRef, acceptedUpdate + mapOf("chatId" to chatRef.id))
            return@runTransaction chatRef.id
        }.addOnSuccessListener { chatId ->
            openChat(chatId)
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Failed to accept", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openChat(chatId: String) {
        ChatBottomSheet.newInstance(chatId)
            .show(parentFragmentManager, "chat")
    }

    private fun rejectRequest(req: RepairRequest) {
        val uid = currentUid() ?: return

        db.collection("requests").document(req.id)
            .update(mapOf("rejectedBy" to FieldValue.arrayUnion(uid)))
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to reject", Toast.LENGTH_SHORT).show()
            }
    }

    private fun completeRequest(req: RepairRequest) {
        val uid = currentUid() ?: return

        // Stop location sharing if active for this request
        if (locationSharingService?.currentRequestId == req.id) {
            locationSharingService?.stopSharing()
            activeAdapter.sharingRequestId = null
        }

        db.collection("requests").document(req.id)
            .update(
                mapOf(
                    "status" to "completed_pending",
                    "completedAt" to FieldValue.serverTimestamp(),
                    "completedBy" to uid
                )
            )
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to mark completed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun releaseRequest(req: RepairRequest) {
        val uid = currentUid() ?: return

        // Stop location sharing if active for this request
        if (locationSharingService?.currentRequestId == req.id) {
            locationSharingService?.stopSharing()
            activeAdapter.sharingRequestId = null
        }

        db.collection("requests").document(req.id)
            .update(
                mapOf(
                    "status" to "pending",
                    "mechanicId" to null,
                    "mechanicName" to null,
                    "handledAt" to FieldValue.serverTimestamp(),
                    "rejectedBy" to FieldValue.arrayUnion(uid),
                    // Clear location sharing fields
                    "locationSharingActive" to false,
                    "mechanicLat" to FieldValue.delete(),
                    "mechanicLng" to FieldValue.delete(),
                    "mechanicLocationUpdatedAt" to FieldValue.delete()
                )
            )
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to release", Toast.LENGTH_SHORT).show()
            }
    }
}
