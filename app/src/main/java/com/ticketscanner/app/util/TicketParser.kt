package com.ticketscanner.app.util

/**
 * Parses 29-digit Data Matrix codes into ticket identifiers and quantities.
 *
 * Parsing Algorithm:
 * 1. Discard the last 16 digits (unused).
 * 2. From the remaining 13 digits:
 *    - First 10 digits → Ticket ID (unique lottery batch identifier)
 *    - Last 3 digits → Ticket Number (current quantity of tickets remaining)
 */
object TicketParser {

    data class ParseResult(
        val ticketId: String,
        val ticketNumber: Int
    )

    sealed class ParseError {
        data object EmptyInput : ParseError()
        data object InvalidLength : ParseError()
        data object NonNumeric : ParseError()
        data object InvalidTicketNumber : ParseError()
    }

    /**
     * Parses a 29-digit numeric string into a ticket ID and ticket number.
     *
     * @param rawCode The raw 29-digit string from the Data Matrix scan
     * @return Result containing either a ParseResult or a ParseError
     */
    fun parse(rawCode: String): Result<ParseResult> {
        val trimmed = rawCode.trim()

        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Empty input"))
        }

        if (trimmed.length != 29) {
            return Result.failure(
                IllegalArgumentException("Invalid length: expected 29 digits, got ${trimmed.length}")
            )
        }

        if (!trimmed.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("Input contains non-numeric characters"))
        }

        // Step 1: Discard the last 16 digits
        val meaningful = trimmed.substring(0, 13)

        // Step 2: Extract Ticket ID (first 10) and Ticket Number (last 3)
        val ticketId = meaningful.substring(0, 10)
        val ticketNumberStr = meaningful.substring(10, 13)
        val ticketNumber = ticketNumberStr.toIntOrNull()
            ?: return Result.failure(IllegalArgumentException("Invalid ticket number: $ticketNumberStr"))

        if (ticketNumber < 0) {
            return Result.failure(IllegalArgumentException("Ticket number cannot be negative"))
        }

        return Result.success(ParseResult(ticketId, ticketNumber))
    }

    /**
     * Validates a manually entered ticket ID.
     */
    fun isValidTicketId(ticketId: String): Boolean {
        return ticketId.length == 10 && ticketId.all { it.isDigit() }
    }

    /**
     * Validates a manually entered ticket number.
     */
    fun isValidTicketNumber(ticketNumber: String): Boolean {
        val num = ticketNumber.toIntOrNull() ?: return false
        return num in 0..999
    }
}
