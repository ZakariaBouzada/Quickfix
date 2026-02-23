package com.example.bikerepairapp.ui.mechanic

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.bikerepairapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.card.MaterialCardView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration

class MechanicHomeFragment : Fragment(R.layout.fragment_mechanic_home), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null

    // Maps marker → RepairRequest so we can show details on tap
    private val markerRequestMap = mutableMapOf<String, RepairRequest>()

    // Default center: Turku
    private val defaultCenter = LatLng(60.4518, 22.2666)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mechanicMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        view.findViewById<ImageButton>(R.id.btnCloseDetail).setOnClickListener {
            view.findViewById<MaterialCardView>(R.id.cardRequestDetail).visibility = View.GONE
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Dark map style feel — just set background black (full style JSON needs res file)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultCenter, 12f))

        map.setOnMarkerClickListener { marker ->
            val req = markerRequestMap[marker.id]
            if (req != null) showDetailCard(req)
            true
        }

        map.setOnMapClickListener {
            view?.findViewById<MaterialCardView>(R.id.cardRequestDetail)?.visibility = View.GONE
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

                // Update pin count
                view?.findViewById<TextView>(R.id.tvPinCount)?.text =
                    "${requests.size} pending"

                val boundsBuilder = LatLngBounds.Builder()
                var hasGeo = false

                for (req in requests) {
                    val geo = req.locationGeo
                    if (geo != null) {
                        val latLng = LatLng(geo.latitude, geo.longitude)
                        val color = markerColorForVehicle(req.vehicleType)

                        val markerOptions = MarkerOptions()
                            .position(latLng)
                            .title(req.issue)
                            .icon(BitmapDescriptorFactory.defaultMarker(color))

                        val marker = map.addMarker(markerOptions)
                        if (marker != null) {
                            markerRequestMap[marker.id] = req
                        }
                        boundsBuilder.include(latLng)
                        hasGeo = true
                    }
                }

                // Fit camera to all pins if we have any
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

    private fun markerColorForVehicle(vehicleType: String?): Float {
        return when (vehicleType) {
            "Car"       -> BitmapDescriptorFactory.HUE_AZURE   // light blue
            "Motorbike" -> BitmapDescriptorFactory.HUE_ORANGE  // orange
            else        -> BitmapDescriptorFactory.HUE_YELLOW  // yellow = Bike / unknown
        }
    }

    private fun showDetailCard(req: RepairRequest) {
        val root = view ?: return
        val card = root.findViewById<MaterialCardView>(R.id.cardRequestDetail)

        val vehicleEmoji = when (req.vehicleType) {
            "Car"       -> "🚗"
            "Motorbike" -> "🏍️"
            else        -> "🚲"
        }
        val vehicleLabel = req.vehicleType ?: "Bike"

        root.findViewById<TextView>(R.id.tvDetailVehicleType).text = "$vehicleEmoji $vehicleLabel"
        root.findViewById<TextView>(R.id.tvDetailIssue).text = req.issue
        root.findViewById<TextView>(R.id.tvDetailMeta).text = buildString {
            if (req.date.isNotBlank()) append(req.date)
            if (req.date.isNotBlank() && req.location.isNotBlank()) append(" • ")
            if (req.location.isNotBlank()) append(req.location)
        }
        root.findViewById<TextView>(R.id.tvDetailCustomer).text =
            req.customerName.ifBlank { req.customerEmail }

        card.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        listener?.remove()
        listener = null
        googleMap = null
        super.onDestroyView()
    }
}
