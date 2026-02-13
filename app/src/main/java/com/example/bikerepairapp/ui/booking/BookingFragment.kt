package com.example.bikerepairapp.ui.booking

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bikerepairapp.R
import com.example.bikerepairapp.notifications.NotificationBadgeManager
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.material.button.MaterialButton
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import android.location.Geocoder
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import java.util.Locale
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.bikerepairapp.notifications.NotificationOverlayFragment


class BookingFragment : Fragment(R.layout.fragment_booking), OnMapReadyCallback {

    private val notifBadge = NotificationBadgeManager()
    private lateinit var googleMap: GoogleMap
    private var userCreatedMarker: Marker? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnOpenBookingForm = view.findViewById<MaterialButton>(R.id.btnOpenBookingForm)
        val btnOpenMyRepairs = view.findViewById<MaterialButton>(R.id.btnOpenMyRepairs)
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.homeMap) as SupportMapFragment

        mapFragment.getMapAsync(this)

        // Bell include (from fragment_booking.xml)
        val bellInclude = view.findViewById<View>(R.id.includeNotifBell)
        notifBadge.bind(
            includeRoot = bellInclude,
            lifecycleOwner = viewLifecycleOwner,
            onBellClick = {
                // Use the ID from your nav_graph.xml instead of a manual transaction
                try {
                    findNavController().navigate(R.id.notificationOverlayFragment)
                } catch (e: Exception) {
                    android.util.Log.e("NAV_ERROR", "Navigation failed: ${e.message}")
                }
            },
            markSeenOnClick = true
        )

        btnOpenBookingForm.setOnClickListener {
            findNavController().navigate(R.id.action_bookingFragment_to_bookingFormFragment)
        }

        btnOpenMyRepairs.setOnClickListener {
            findNavController().navigate(R.id.action_bookingFragment_to_customerRepairsFragment)
        }
    }

    override fun onDestroyView() {
        notifBadge.unbind()
        super.onDestroyView()
    }
    override fun onResume() {
        super.onResume()
        if (::googleMap.isInitialized) {
            listenForMyBookings()
        }
    }
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val turku = LatLng(60.4518, 22.2666)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(turku, 12f))

        // Start listening for data
        listenForMyBookings()

        googleMap.setOnMapLongClickListener { latLng ->
            // Remove the OLD marker before creating the NEW one
            userCreatedMarker?.remove()
            // Create new and SAVE it to the variable
            userCreatedMarker = placeDraggableMarker(latLng)
        }

        googleMap.setOnInfoWindowClickListener { marker ->
            val requestId = marker.tag as? String

            try {
                if (requestId != null) {
                    // NAVIGATION A: To Repairs List
                    val navOptions = androidx.navigation.NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setPopUpTo(R.id.bookingFragment, false)
                        .build()

                    findNavController().navigate(
                        R.id.action_bookingFragment_to_customerRepairsFragment,
                        Bundle().apply { putString("highlightRequestId", requestId) },
                        navOptions
                    )
                } else {
                    // NAVIGATION B: To Booking Form
                    val bundle = Bundle().apply {
                        putString("prefillAddress", marker.snippet ?: "")
                    }
                    findNavController().navigate(R.id.action_bookingFragment_to_bookingFormFragment, bundle)
                }
            } catch (e: Exception) {
                // This prevents the crash from closing the app if a Nav ID is wrong
                android.util.Log.e("MAP_ERROR", "Navigation failed: ${e.message}")
            }
        }

        googleMap.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
            override fun onMarkerDragStart(marker: Marker) { marker.hideInfoWindow() }
            override fun onMarkerDrag(marker: Marker) {}
            override fun onMarkerDragEnd(marker: Marker) {
                val newAddress = getAddressFromLatLng(marker.position)
                marker.snippet = newAddress
                marker.showInfoWindow()
            }
        })
    }

    private fun placeDraggableMarker(latLng: LatLng): Marker? {
        // Note: Don't call userCreatedMarker?.remove() inside the listener logic
        // if you are just refreshing.

        val address = getAddressFromLatLng(latLng)
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Repair Location")
                .snippet(address)
                .draggable(true)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
        )
        marker?.showInfoWindow()
        return marker
    }

    private fun getAddressFromLatLng(latLng: LatLng): String {
        return try {
            val geocoder = Geocoder(requireContext(), Locale.getDefault())
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                // Returns "Street Name Number"
                addresses[0].getAddressLine(0).split(",")[0]
            } else {
                "Unknown Location"
            }
        } catch (e: Exception) {
            "Address not found"
        }
    }
    private fun listenForMyBookings() {
        // CRASH FIX: Check if user is null before accessing UID
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val uid = user.uid
        val db = FirebaseFirestore.getInstance()

        db.collection("requests")
            .whereEqualTo("customerId", uid)
            .addSnapshotListener { snapshots, e ->
                if (e != null || snapshots == null) return@addSnapshotListener

                // Save the position of the draggable marker before clearing
                val tempPos = userCreatedMarker?.position

                googleMap.clear() // This removes ALL markers, including userCreatedMarker

                // Re-assign the userCreatedMarker using the saved position
                tempPos?.let {
                    userCreatedMarker = placeDraggableMarker(it)
                }

                for (doc in snapshots) {
                    val lat = doc.getDouble("locationLat")
                    val lng = doc.getDouble("locationLng")

                    // CRASH FIX: Use safe calls (?.) instead of (!!)
                    if (lat != null && lng != null) {
                        val status = doc.getString("status") ?: "pending"
                        val issue = doc.getString("issue") ?: "Bicycle Repair"

                        val color = when(status) {
                            "pending" -> BitmapDescriptorFactory.HUE_RED
                            "accepted" -> BitmapDescriptorFactory.HUE_BLUE
                            else -> BitmapDescriptorFactory.HUE_GREEN
                        }

                        val marker = googleMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(lat, lng))
                                .title(issue)
                                .snippet("Status: $status (Tap to View)")
                                .icon(BitmapDescriptorFactory.defaultMarker(color))
                        )
                        marker?.tag = doc.id
                    }
                }
            }
    }
}

