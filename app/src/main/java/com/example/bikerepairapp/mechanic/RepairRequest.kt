package com.example.bikerepairapp.ui.mechanic

data class RepairRequest(
    val id: String = "",
    val issue: String = "",
    val date: String = "",
    val location: String = "",
    val status: String = "",
    val customerEmail: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val mechanicId: String? = null,
    val mechanicName: String? = null,
    val imageUri: String? = null
)
