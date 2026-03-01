package com.ticketscanner.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ticketscanner.app.model.BaseTicket

/**
 * Room entity representing a specific ticket batch with sales tracking.
 */
@Entity(tableName = "tickets")
data class Ticket(
    @PrimaryKey
    @ColumnInfo(name = "ticket_id")
    override val ticketId: String,

    @ColumnInfo(name = "ticket_no")
    override val ticketNumber: Int,

    @ColumnInfo(name = "price_per_ticket")
    override val ticketPrice: Int,

    @ColumnInfo(name = "total_sale")
    val totalSale: Int = 0,

    @ColumnInfo(name = "last_scan_timestamp")
    val lastScanTimestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "sold_flag")
    val isSoldOut: Boolean = false
) : BaseTicket()
