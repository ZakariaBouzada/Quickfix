package com.example.bikerepairapp.ui.mechanic

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.example.bikerepairapp.ui.messages.ChatBottomSheet

class MechanicHomeFragment : Fragment(R.layout.fragment_mechanic_home), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listener: ListenerRegistration? = null

    // Currently displayed request in the detail card
    private var activeRequest: RepairRequest? = null

    // Maps marker.id → RepairRequest for tap handling
    private val markerRequestMap = mutableMapOf<String, RepairRequest>()

    private val defaultCenter = LatLng(60.4518, 22.2666)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mechanicMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Close detail card
        view.findViewById<ImageButton>(R.id.btnCloseDetail).setOnClickListener {
            view.findViewById<MaterialCardView>(R.id.cardRequestDetail).visibility = View.GONE
            activeRequest = null
        }

        // Zoom in
        view.findViewById<ImageButton>(R.id.btnZoomIn).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomIn())
        }

        // Zoom out
        view.findViewById<ImageButton>(R.id.btnZoomOut).setOnClickListener {
            googleMap?.animateCamera(CameraUpdateFactory.zoomOut())
        }

        // Accept from detail card
        view.findViewById<MaterialButton>(R.id.btnDetailAccept).setOnClickListener {
            val req = activeRequest ?: return@setOnClickListener
            acceptRequest(req)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Enable built-in zoom gestures (pinch-to-zoom) — buttons we handle ourselves
        map.uiSettings.isZoomControlsEnabled = false   // we use our own buttons
        map.uiSettings.isZoomGesturesEnabled = true    // pinch zoom still works
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isTiltGesturesEnabled = false

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCenter, 12f))

        map.setOnMarkerClickListener { marker ->
            val req = markerRequestMap[marker.id]
            if (req != null) showDetailCard(req)
            true
        }

        map.setOnMapClickListener {
            view?.findViewById<MaterialCardView>(R.id.cardRequestDetail)?.visibility = View.GONE
            activeRequest = null
        }

        listenForPendingRequests()
    }

    private fun listenForPendingRequests() {
        listener?.remove()
        listener = db.collection("requests")
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, _ ->
                if (snap == null || !isAdded) return@addSnapshotListener

                val map = googleMap ?: return@addSnapshotListener
                map.clear()
                markerRequestMap.clear()

                // If the card was open, its request may no longer exist — hide it
                val previousId = activeRequest?.id
                if (previousId != null &&
                    snap.documents.none { it.id == previousId }) {
                    view?.findViewById<MaterialCardView>(R.id.cardRequestDetail)
                        ?.visibility = View.GONE
                    activeRequest = null
                }

                val requests = snap.documents.mapNotNull { doc ->
                    val mechanicId = doc.getString("mechanicId")
                    if (!mechanicId.isNullOrBlank()) return@mapNotNull null

                    RepairRequest(
                        id = doc.id,
                        issue = doc.getString("issue") ?: return@mapNotNull null,
                        date = doc.getString("date") ?: "",
                        location = doc.getString("location") ?: "",
                        status = "pending",
                        customerEmail = doc.getString("customerEmail") ?: "",
                        customerId = doc.getString("customerId") ?: "",
                        mechanicId = null,
                        mechanicName = null,
                        customerName = doc.getString("customerName") ?: "",
                        chatId = null,
                        vehicleType = doc.getString("vehicleType"),
                        locationGeo = doc.getGeoPoint("locationGeo")
                    )
                }

                view?.findViewById<TextView>(R.id.tvPinCount)?.text =
                    "${requests.size} pending"

                val boundsBuilder = LatLngBounds.Builder()
                var hasGeo = false

                for (req in requests) {
                    val geo = req.locationGeo ?: continue
                    val latLng = LatLng(geo.latitude, geo.longitude)
                    val color = markerColorForVehicle(req.vehicleType)

                    val marker = map.addMarker(
                        MarkerOptions()
                            .position(latLng)
                            .title(req.issue)
                            .icon(BitmapDescriptorFactory.defaultMarker(color))
                    )
                    if (marker != null) markerRequestMap[marker.id] = req
                    boundsBuilder.include(latLng)
                    hasGeo = true
                }

                if (hasGeo && requests.size > 1) {
                    try {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 120)
                        )
                    } catch (_: Exception) {
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCenter, 12f))
                    }
                } else if (hasGeo && requests.size == 1) {
                    val geo = requests.first().locationGeo!!
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(geo.latitude, geo.longitude), 14f)
                    )
                }
            }
    }

    private fun showDetailCard(req: RepairRequest) {
        val root = view ?: return
        activeRequest = req

        val vehicleEmoji = when (req.vehicleType) {
            "Car"       -> "🚗"
            "Motorbike" -> "🏍️"
            else        -> "🚲"
        }

        root.findViewById<TextView>(R.id.tvDetailVehicleType).text =
            "$vehicleEmoji ${req.vehicleType ?: "Bike"}"
        root.findViewById<TextView>(R.id.tvDetailIssue).text = req.issue
        root.findViewById<TextView>(R.id.tvDetailMeta).text = buildString {
            if (req.date.isNotBlank()) append(req.date)
            if (req.date.isNotBlank() && req.location.isNotBlank()) append(" • ")
            if (req.location.isNotBlank()) append(req.location)
        }
        root.findViewById<TextView>(R.id.tvDetailCustomer).text =
            req.customerName.ifBlank { req.customerEmail }

        // Reset Accept button in case a previous accept is in progress
        val btnAccept = root.findViewById<MaterialButton>(R.id.btnDetailAccept)
        btnAccept.isEnabled = true
        btnAccept.text = "Accept request"

        root.findViewById<MaterialCardView>(R.id.cardRequestDetail).visibility = View.VISIBLE
    }

    private fun acceptRequest(req: RepairRequest) {
        val mechanicId = auth.currentUser?.uid ?: return
        val mechanicName = auth.currentUser?.email ?: "mechanic"

        // Disable button immediately to prevent double-tap
        val btnAccept = view?.findViewById<MaterialButton>(R.id.btnDetailAccept) ?: return
        btnAccept.isEnabled = false
        btnAccept.text = "Accepting…"

        val reqRef = db.collection("requests").document(req.id)

        db.runTransaction { tx ->
            val reqSnap = tx.get(reqRef)

            // Guard: already taken by someone else
            val currentStatus = reqSnap.getString("status")
            val currentMechanic = reqSnap.getString("mechanicId")
            if (currentStatus != "pending" || !currentMechanic.isNullOrBlank()) {
                throw Exception("Request already taken")
            }

            val existingChatId = reqSnap.getString("chatId")

            val acceptedUpdate = mapOf(
                "status"      to "accepted",
                "mechanicId"  to mechanicId,
                "mechanicName" to mechanicName,
                "acceptedAt"  to FieldValue.serverTimestamp()
            )

            if (!existingChatId.isNullOrBlank()) {
                tx.update(reqRef, acceptedUpdate)
                return@runTransaction existingChatId
            }

            val customerId = reqSnap.getString("customerId") ?: ""
            val chatRef = db.collection("chats").document()

            tx.set(chatRef, mapOf(
                "requestId"     to req.id,
                "customerId"    to customerId,
                "mechanicId"    to mechanicId,
                "participants"  to listOf(customerId, mechanicId),
                "createdAt"     to FieldValue.serverTimestamp(),
                "lastMessage"   to "Mechanic accepted ✅",
                "lastMessageAt" to FieldValue.serverTimestamp(),
                "lastSenderId"  to mechanicId
            ))

            tx.update(reqRef, acceptedUpdate + mapOf("chatId" to chatRef.id))
            return@runTransaction chatRef.id
        }.addOnSuccessListener { chatId ->
            // Hide the card (request will disappear from map on next snapshot)
            view?.findViewById<MaterialCardView>(R.id.cardRequestDetail)
                ?.visibility = View.GONE
            activeRequest = null

            Toast.makeText(requireContext(), "Request accepted ✅", Toast.LENGTH_SHORT).show()

            // Open chat immediately
            ChatBottomSheet.newInstance(chatId)
                .show(parentFragmentManager, "chat")
        }.addOnFailureListener { e ->
            Toast.makeText(
                requireContext(),
                if (e.message == "Request already taken") "Someone else just took this request"
                else "Failed to accept: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            // Re-enable button
            btnAccept.isEnabled = true
            btnAccept.text = "Accept request"
        }
    }

    private fun markerColorForVehicle(vehicleType: String?): Float = when (vehicleType) {
        "Car"       -> BitmapDescriptorFactory.HUE_AZURE
        "Motorbike" -> BitmapDescriptorFactory.HUE_ORANGE
        else        -> BitmapDescriptorFactory.HUE_YELLOW
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        googleMap = null
        super.onDestroyView()
    }
}
