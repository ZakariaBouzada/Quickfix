package com.example.bikerepairapp.ui.booking

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bikerepairapp.R
import com.google.android.material.button.MaterialButton
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.net.URLEncoder
import java.util.Locale
import kotlin.concurrent.thread

class OsmMapPickerActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var marker: Marker? = null

    private var pickedPoint: GeoPoint? = null
    private var pickedAddress: String? = null

    private val http by lazy { OkHttpClient() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Required by osmdroid
        Configuration.getInstance().userAgentValue = packageName

        setContentView(R.layout.activity_osm_map_picker)

        mapView = findViewById(R.id.osmMapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        val hint = findViewById<TextView>(R.id.tvMapHint)

        // Center Turku by default
        val turku = GeoPoint(60.4518, 22.2666)
        mapView.controller.setZoom(13.0)
        mapView.controller.setCenter(turku)

        // Tap to place pin
        mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(
                e: android.view.MotionEvent?,
                mapView: MapView?
            ): Boolean {
                if (e == null || mapView == null) return false
                val proj = mapView.projection
                val geo = proj.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint

                pickedPoint = geo
                placeMarker(geo)

                hint.text = "Looking up address…"
                reverseGeocode(geo) { address ->
                    pickedAddress = address
                    runOnUiThread {
                        hint.text = address ?: "Pinned: ${"%.5f".format(geo.latitude)}, ${"%.5f".format(geo.longitude)}"
                    }
                }
                return true
            }
        })

        findViewById<ImageButton>(R.id.btnCloseMap).setOnClickListener { finish() }

        findViewById<MaterialButton>(R.id.btnUseThisLocation).setOnClickListener {
            val p = pickedPoint
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

    private fun placeMarker(point: GeoPoint) {
        marker?.let { mapView.overlays.remove(it) }

        val m = Marker(mapView)
        m.position = point
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        m.title = "Repair location"
        marker = m

        mapView.overlays.add(m)
        mapView.invalidate()
    }

    /**
     * Reverse geocode using OpenStreetMap Nominatim.
     * NOTE: Nominatim has usage policies + rate limits; good for small/demo use.
     */
    private fun reverseGeocode(point: GeoPoint, cb: (String?) -> Unit) {
        thread {
            try {
                val lat = point.latitude
                val lon = point.longitude

                val url =
                    "https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=$lat&lon=$lon&zoom=18&addressdetails=1"

                val req = Request.Builder()
                    .url(url)
                    // Nominatim requires a valid User-Agent identifying your app
                    .header("User-Agent", "${packageName} (QuickFix)")
                    .build()

                val resp = http.newCall(req).execute()
                val body = resp.body?.string()
                if (!resp.isSuccessful || body.isNullOrBlank()) {
                    cb(null)
                    return@thread
                }

                val json = JSONObject(body)
                val displayName = json.optString("display_name", null)

                // Try to format a clean street line if possible
                val addrObj = json.optJSONObject("address")
                val road = addrObj?.optString("road", null)
                val houseNo = addrObj?.optString("house_number", null)

                val nice = when {
                    !road.isNullOrBlank() && !houseNo.isNullOrBlank() -> "$road $houseNo"
                    !road.isNullOrBlank() -> road
                    else -> displayName
                }

                cb(nice)
            } catch (_: Exception) {
                cb(null)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_ADDRESS = "extra_address"
    }
}
