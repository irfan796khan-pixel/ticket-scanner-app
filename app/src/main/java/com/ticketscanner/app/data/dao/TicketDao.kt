package com.ticketscanner.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.ticketscanner.app.data.entity.Ticket

@Dao
interface TicketDao {

    @Query("SELECT * FROM tickets WHERE ticket_id = :ticketId")
    suspend fun getTicketById(ticketId: String): Ticket?

    @Query("SELECT * FROM tickets ORDER BY last_scan_timestamp DESC")
    fun getAllTickets(): LiveData<List<Ticket>>

    @Query("SELECT * FROM tickets ORDER BY last_scan_timestamp DESC")
    suspend fun getAllTicketsList(): List<Ticket>

    @Query("SELECT SUM(total_sale) FROM tickets")
    fun getGrandTotal(): LiveData<Int?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: Ticket)

    @Update
    suspend fun updateTicket(ticket: Ticket)

    @Query("DELETE FROM tickets")
    suspend fun deleteAllTickets()

    @Query("SELECT COUNT(*) FROM tickets")
    suspend fun getTicketCount(): Int
}
