package com.example.bikerepairapp.ui.repairs

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
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
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Locale

/**
 * Full-screen map showing the mechanic's live location for a specific repair request.
 * Yellow pin = repair location, Blue pin = mechanic's live position.
 */
class MechanicTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null
    private var googleMap: GoogleMap? = null

    private var requestId: String = ""

    private var repairMarker: Marker? = null
    private var mechanicMarker: Marker? = null
    private var hasCenteredOnce = false

    private lateinit var tvStatus: TextView
    private lateinit var tvUpdated: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mechanic_tracking)

        requestId = intent.getStringExtra("requestId") ?: run {
            finish()
            return
        }

        tvStatus = findViewById(R.id.tvTrackingStatus)
        tvUpdated = findViewById(R.id.tvTrackingUpdated)

        findViewById<ImageButton>(R.id.btnBackTracking).setOnClickListener {
            finish()
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.trackingMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isZoomGesturesEnabled = true
        map.uiSettings.isScrollGesturesEnabled = true
        map.uiSettings.isTiltGesturesEnabled = false

        // Default camera (Turku, Finland)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(60.4518, 22.2666), 12f))

        startListening()
    }

    private fun startListening() {
        listener?.remove()
        listener = db.collection("requests").document(requestId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener

                val map = googleMap ?: return@addSnapshotListener

                // Repair location (yellow pin)
                val geo = snap.getGeoPoint("locationGeo")
                if (geo != null && repairMarker == null) {
                    val pos = LatLng(geo.latitude, geo.longitude)
                    repairMarker = map.addMarker(
                        MarkerOptions()
                            .position(pos)
                            .title("Repair location")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                    )
                }

                // Mechanic location (blue pin)
                val locationSharingActive = snap.getBoolean("locationSharingActive") ?: false
                val mechanicLat = snap.getDouble("mechanicLat")
                val mechanicLng = snap.getDouble("mechanicLng")
                val updatedAt = snap.getTimestamp("mechanicLocationUpdatedAt")

                if (locationSharingActive && mechanicLat != null && mechanicLng != null) {
                    val mechPos = LatLng(mechanicLat, mechanicLng)

                    if (mechanicMarker == null) {
                        mechanicMarker = map.addMarker(
                            MarkerOptions()
                                .position(mechPos)
                                .title("Mechanic")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                        )
                    } else {
                        mechanicMarker?.position = mechPos
                    }

                    tvStatus.text = "Mechanic is on the way"
                    tvUpdated.text = formatUpdatedAgo(updatedAt)

                    // Center camera to show both pins
                    if (!hasCenteredOnce) {
                        hasCenteredOnce = true
                        fitBothMarkers(map)
                    }
                } else if (!locationSharingActive && mechanicMarker != null) {
                    // Location sharing has ended
                    mechanicMarker?.remove()
                    mechanicMarker = null
                    tvStatus.text = "Location sharing ended"
                    tvUpdated.text = ""
                } else if (!locationSharingActive) {
                    tvStatus.text = "Waiting for mechanic to share location\u2026"
                    tvUpdated.text = ""

                    // If we haven't centered and have a repair location, center on it
                    if (!hasCenteredOnce && geo != null) {
                        hasCenteredOnce = true
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(geo.latitude, geo.longitude), 14f
                            )
                        )
                    }
                }
            }
    }

    private fun fitBothMarkers(map: GoogleMap) {
        val repair = repairMarker?.position
        val mechanic = mechanicMarker?.position
        if (repair != null && mechanic != null) {
            try {
                val bounds = LatLngBounds.Builder()
                    .include(repair)
                    .include(mechanic)
                    .build()
                map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120))
            } catch (_: Exception) {
                // fallback
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(mechanic, 14f))
            }
        } else if (mechanic != null) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(mechanic, 14f))
        }
    }

    private fun formatUpdatedAgo(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val seconds = (System.currentTimeMillis() - timestamp.toDate().time) / 1000
        return when {
            seconds < 5 -> "Updated just now"
            seconds < 60 -> "Updated ${seconds}s ago"
            seconds < 3600 -> "Updated ${seconds / 60}m ago"
            else -> String.format(Locale.getDefault(), "Updated %dh ago", seconds / 3600)
        }
    }

    override fun onDestroy() {
        listener?.remove()
        listener = null
        super.onDestroy()
    }
}
