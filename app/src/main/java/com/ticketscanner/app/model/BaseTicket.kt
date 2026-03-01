package com.ticketscanner.app.model

/**
 * Abstract parent class defining common attributes for all ticket entities.
 */
abstract class BaseTicket {
    abstract val ticketId: String
    abstract val ticketNumber: Int
    abstract val ticketPrice: Int
}
