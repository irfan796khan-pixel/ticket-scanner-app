package com.ticketscanner.app.util

import com.ticketscanner.app.data.entity.Ticket

/**
 * Represents the result of a ticket scan operation.
 */
sealed class ScanResult {

    /**
     * A new ticket was created from the scan.
     */
    data class NewTicket(
        val ticket: Ticket,
        val needsManualPrice: Boolean = false
    ) : ScanResult()

    /**
     * An existing ticket was updated with a normal sale.
     */
    data class SaleRecorded(
        val ticket: Ticket,
        val saleAmount: Int,
        val ticketsSold: Int
    ) : ScanResult()

    /**
     * Ticket number increased (unusual case) — needs user decision.
     */
    data class QuantityIncreased(
        val ticket: Ticket,
        val oldTicketNumber: Int,
        val newTicketNumber: Int
    ) : ScanResult()

    /**
     * No change detected (same ticket number as before).
     */
    data class NoChange(
        val ticket: Ticket
    ) : ScanResult()

    /**
     * An error occurred during the scan.
     */
    data class Error(
        val message: String
    ) : ScanResult()
}
