package com.example.bikerepairapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "booking_tickets")
data class BookingTicket(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val issue: String,
    val date: String,
    val location: String,
    val status: String,
    val imageUri: String? = null,
    val userId: String? = null
)
