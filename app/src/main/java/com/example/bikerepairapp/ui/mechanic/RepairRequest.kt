package com.example.bikerepairapp.ui.mechanic

import com.google.firebase.firestore.GeoPoint

data class RepairRequest(
    val id: String,
    val issue: String,
    val date: String,
    val location: String,
    val status: String,
    val customerEmail: String,
    val customerId: String,
    val mechanicId: String?,
    val mechanicName: String?,
    val customerName: String = "",
    val chatId: String? = null,
    // Vehicle fields (set for Car/Motorbike requests, null for legacy Bike requests)
    val vehicleType: String? = null,
    val locationGeo: GeoPoint? = null
)
