package com.ticketscanner.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for maintaining a chronological log of all scan events.
 */
@Entity(tableName = "audit_log")
data class AuditLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "ticket_id")
    val ticketId: String,

    @ColumnInfo(name = "action_type")
    val actionType: String,

    @ColumnInfo(name = "amount_change")
    val amountChange: Int = 0,

    @ColumnInfo(name = "details")
    val details: String = ""
) {
    companion object {
        const val ACTION_NEW = "NEW"
        const val ACTION_UPDATE = "UPDATE"
        const val ACTION_SALE = "SALE"
        const val ACTION_SOLD_OUT = "SOLD_OUT"
        const val ACTION_MANUAL_ENTRY = "MANUAL_ENTRY"
        const val ACTION_PRICE_OVERRIDE = "PRICE_OVERRIDE"
    }
}
