package com.example.bikerepairapp.ui.mechanic

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.navigation.fragment.findNavController



class MechanicHomeFragment : Fragment(R.layout.fragment_mechanic_home),
    OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var firestore: FirebaseFirestore
    private var lastClickedMarkerId: String? = null

    // 1. Track the current filter
    private var currentFilter = "all"
    private var registration: com.google.firebase.firestore.ListenerRegistration? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firestore = FirebaseFirestore.getInstance()

        // 2. Setup Button Listeners
        view.findViewById<View>(R.id.btnFilterAll).setOnClickListener { applyFilter("all", view) }
        view.findViewById<View>(R.id.btnFilterPending).setOnClickListener { applyFilter("pending", view) }
        view.findViewById<View>(R.id.btnFilterAccepted).setOnClickListener { applyFilter("accepted", view) }

        val mapFragment = childFragmentManager.findFragmentById(R.id.mechanicMap) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private fun applyFilter(filter: String, view: View) {
        currentFilter = filter
        updateButtonStyles(view)
        listenForBookings() // Restart listener with new query
    }

    private fun updateButtonStyles(view: View) {
        val activeColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FFFF00"))
        val inactiveColor = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#444444"))

        val btnAll = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterAll)
        val btnPending = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterPending)
        val btnAccepted = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFilterAccepted)

        btnAll.backgroundTintList = if (currentFilter == "all") activeColor else inactiveColor
        btnAll.setTextColor(if (currentFilter == "all") android.graphics.Color.BLACK else android.graphics.Color.WHITE)

        btnPending.backgroundTintList = if (currentFilter == "pending") activeColor else inactiveColor
        btnPending.setTextColor(if (currentFilter == "pending") android.graphics.Color.BLACK else android.graphics.Color.WHITE)

        btnAccepted.backgroundTintList = if (currentFilter == "accepted") activeColor else inactiveColor
        btnAccepted.setTextColor(if (currentFilter == "accepted") android.graphics.Color.BLACK else android.graphics.Color.WHITE)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val turku = LatLng(60.4518, 22.2666)
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(turku, 12f))

        listenForBookings()

        googleMap.setOnInfoWindowClickListener { marker ->
            val requestId = marker.tag as? String ?: return@setOnInfoWindowClickListener

            // This is the "Magic" NavOptions that mimics the Bottom Nav bar behavior
            val navOptions = androidx.navigation.NavOptions.Builder()
                .setLaunchSingleTop(true)
                .setRestoreState(true)
                .setPopUpTo(
                    R.id.mechanicHomeFragment, // This is your graph ID from nav_mechanic.xml
                    inclusive = false,
                    saveState = true
                )
                .build()

            findNavController().navigate(
                R.id.mechanicRequestsFragment,
                Bundle().apply {
                    putString("highlightRequestId", requestId)
                },
                navOptions // Pass these options!
            )
        }
    }

    private fun listenForBookings() {
        // Remove old listener before starting a new one to prevent memory leaks/duplicate markers
        registration?.remove()

        // 3. Dynamic Firestore Query
        var query = firestore.collection("requests").whereNotEqualTo("status", "completed")

        if (currentFilter != "all") {
            query = query.whereEqualTo("status", currentFilter)
        }

        registration = query.addSnapshotListener { snapshots, _ ->
            if (snapshots == null) return@addSnapshotListener
            googleMap.clear()

            for (doc in snapshots) {
                val lat = doc.getDouble("locationLat")
                val lng = doc.getDouble("locationLng")
                val issue = doc.getString("issue")
                val status = doc.getString("status")

                if (lat != null && lng != null) {
                    val position = LatLng(lat, lng)
                    val color = when (status) {
                        "pending" -> BitmapDescriptorFactory.HUE_YELLOW
                        "accepted" -> BitmapDescriptorFactory.HUE_BLUE
                        else -> BitmapDescriptorFactory.HUE_RED
                    }

                    val marker = googleMap.addMarker(
                        MarkerOptions()
                            .position(position)
                            .title(issue)
                            .snippet("Status: $status")
                            .icon(BitmapDescriptorFactory.defaultMarker(color))
                    )
                    marker?.tag = doc.id
                }
            }
        }
    }
}