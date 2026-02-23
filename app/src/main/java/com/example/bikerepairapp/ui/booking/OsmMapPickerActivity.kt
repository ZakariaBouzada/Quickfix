package com.example.bikerepairapp.ui.booking

import android.location.Geocoder
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bikerepairapp.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import java.util.Locale
import kotlin.concurrent.thread

class OsmMapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null
    private var marker: Marker? = null

    private var pickedLatLng: LatLng? = null
    private var pickedAddress: String? = null

    private lateinit var hint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_osm_map_picker)

        hint = findViewById(R.id.tvMapHint)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.googleMapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        findViewById<ImageButton>(R.id.btnCloseMap).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnUseThisLocation).setOnClickListener {
            val p = pickedLatLng
            if (p == null) {
                hint.text = "Tap the map first to place a pin."
                return@setOnClickListener
            }

            val data = intent.apply {
                putExtra(EXTRA_LAT, p.latitude)
                putExtra(EXTRA_LNG, p.longitude)
                putExtra(EXTRA_ADDRESS, pickedAddress ?: "")
            }

            setResult(RESULT_OK, data)
            finish()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Default center: Turku, Finland
        val turku = LatLng(60.4518, 22.2666)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(turku, 13f))

        map.setOnMapClickListener { latLng ->
            pickedLatLng = latLng
            placeMarker(latLng)
            hint.text = "Looking up address…"
            reverseGeocode(latLng) { address ->
                pickedAddress = address
                runOnUiThread {
                    hint.text = address
                        ?: "Pinned: ${"%.5f".format(latLng.latitude)}, ${"%.5f".format(latLng.longitude)}"
                }
            }
        }
    }

    private fun placeMarker(latLng: LatLng) {
        marker?.remove()
        marker = googleMap?.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Repair location")
        )
    }

    private fun reverseGeocode(latLng: LatLng, cb: (String?) -> Unit) {
        thread {
            try {
                val geocoder = Geocoder(this, Locale.getDefault())
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (results.isNullOrEmpty()) {
                    cb(null)
                    return@thread
                }
                val addr = results[0]
                val nice = when {
                    !addr.thoroughfare.isNullOrBlank() && !addr.subThoroughfare.isNullOrBlank() ->
                        "${addr.thoroughfare} ${addr.subThoroughfare}"
                    !addr.thoroughfare.isNullOrBlank() ->
                        addr.thoroughfare
                    else ->
                        addr.getAddressLine(0)
                }
                cb(nice)
            } catch (_: Exception) {
                cb(null)
            }
        }
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_ADDRESS = "extra_address"
    }
}
