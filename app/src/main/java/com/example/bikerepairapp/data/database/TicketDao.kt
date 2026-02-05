package com.example.bikerepairapp.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bikerepairapp.data.model.BookingTicket

@Dao
interface TicketDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: BookingTicket)

    @Query("SELECT * FROM booking_tickets ORDER BY id DESC LIMIT 1")
    suspend fun getLatestTicket(): BookingTicket?
}
