package com.example.bikerepairapp.ui.mechanic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Foreground-only location sharing service for mechanics.
 * Writes the mechanic's GPS position to Firestore every ~5 seconds.
 */
class LocationSharingService(private val context: Context) {

    companion object {
        private const val TAG = "LocationSharing"
        private const val WRITE_INTERVAL_MS = 5_000L
    }

    private val db = FirebaseFirestore.getInstance()
    private val fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    var isSharing = false
        private set

    var currentRequestId: String? = null
        private set

    private var lastWriteTime = 0L

    /** Callback that receives location updates */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            val reqId = currentRequestId ?: return
            val now = System.currentTimeMillis()

            // Throttle writes to every 5 seconds
            if (now - lastWriteTime < WRITE_INTERVAL_MS) return
            lastWriteTime = now

            updateMechanicLocation(reqId, location.latitude, location.longitude)
        }
    }

    /**
     * Start sharing location for a specific request.
     * Returns false if location permission is not granted.
     */
    fun startSharing(requestId: String): Boolean {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return false
        }

        // Stop any existing sharing first
        if (isSharing) {
            stopSharingInternal()
        }

        currentRequestId = requestId
        isSharing = true
        lastWriteTime = 0L

        // Write initial state to Firestore
        db.collection("requests").document(requestId)
            .update(mapOf(
                "locationSharingActive" to true,
                "mechanicLocationUpdatedAt" to FieldValue.serverTimestamp()
            ))

        // Request location updates
        val request = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            WRITE_INTERVAL_MS
        ).setMinUpdateIntervalMillis(3_000L)
            .build()

        try {
            fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            Log.d(TAG, "Started location sharing for request $requestId")
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException requesting location", e)
            isSharing = false
            currentRequestId = null
            return false
        }

        return true
    }

    /** Stop sharing and clear Firestore fields */
    fun stopSharing() {
        val reqId = currentRequestId
        stopSharingInternal()

        if (reqId != null) {
            db.collection("requests").document(reqId)
                .update(mapOf(
                    "locationSharingActive" to false,
                    "mechanicLat" to FieldValue.delete(),
                    "mechanicLng" to FieldValue.delete(),
                    "mechanicLocationUpdatedAt" to FieldValue.delete()
                ))
            Log.d(TAG, "Stopped location sharing for request $reqId")
        }
    }

    private fun stopSharingInternal() {
        fusedClient.removeLocationUpdates(locationCallback)
        isSharing = false
        currentRequestId = null
    }

    private fun updateMechanicLocation(requestId: String, lat: Double, lng: Double) {
        db.collection("requests").document(requestId)
            .update(mapOf(
                "mechanicLat" to lat,
                "mechanicLng" to lng,
                "mechanicLocationUpdatedAt" to FieldValue.serverTimestamp()
            ))
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update location", e)
            }
    }
}
