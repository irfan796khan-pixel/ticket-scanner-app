package com.ticketscanner.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.ticketscanner.app.TicketScannerApp
import com.ticketscanner.app.data.entity.AuditLog
import com.ticketscanner.app.data.entity.Ticket
import com.ticketscanner.app.util.PriceMatrix
import com.ticketscanner.app.util.ScanResult
import com.ticketscanner.app.util.TicketParser
import kotlinx.coroutines.launch

class TicketViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as TicketScannerApp).database
    private val ticketDao = database.ticketDao()
    private val auditLogDao = database.auditLogDao()

    val allTickets: LiveData<List<Ticket>> = ticketDao.getAllTickets()
    val grandTotal: LiveData<Int?> = ticketDao.getGrandTotal()
    val allAuditLogs: LiveData<List<AuditLog>> = auditLogDao.getAllLogs()

    private val _scanResult = MutableLiveData<ScanResult>()
    val scanResult: LiveData<ScanResult> = _scanResult

    private val _lastScannedTicket = MutableLiveData<Ticket?>()
    val lastScannedTicket: LiveData<Ticket?> = _lastScannedTicket

    private val _isProcessing = MutableLiveData(false)
    val isProcessing: LiveData<Boolean> = _isProcessing

    private var lastScannedCode: String? = null
    private var lastScanTime: Long = 0

    init {
        PriceMatrix.initialize(application)
    }

    /**
     * Process a raw 29-digit Data Matrix code from camera or manual input.
     */
    fun processScannedCode(rawCode: String) {
        // Duplicate scan prevention (within 2 seconds)
        val now = System.currentTimeMillis()
        if (rawCode == lastScannedCode && now - lastScanTime < 2000) {
            return
        }
        lastScannedCode = rawCode
        lastScanTime = now

        if (_isProcessing.value == true) return
        _isProcessing.value = true

        val parseResult = TicketParser.parse(rawCode)

        parseResult.fold(
            onSuccess = { parsed ->
                viewModelScope.launch {
                    try {
                        val result = processTicket(parsed.ticketId, parsed.ticketNumber)
                        _scanResult.value = result
                    } catch (e: Exception) {
                        _scanResult.value = ScanResult.Error("Processing error: ${e.message}")
                    } finally {
                        _isProcessing.value = false
                    }
                }
            },
            onFailure = { error ->
                _scanResult.value = ScanResult.Error("Parse error: ${error.message}")
                _isProcessing.value = false
            }
        )
    }

    /**
     * Process a manually entered ticket (ticket ID + ticket number).
     */
    fun processManualEntry(ticketId: String, ticketNumber: Int, manualPrice: Int? = null) {
        if (_isProcessing.value == true) return
        _isProcessing.value = true

        viewModelScope.launch {
            try {
                val result = processTicket(ticketId, ticketNumber, manualPrice)
                _scanResult.value = result

                // Log manual entry
                auditLogDao.insert(
                    AuditLog(
                        ticketId = ticketId,
                        actionType = AuditLog.ACTION_MANUAL_ENTRY,
                        details = "Manual entry: ticketNumber=$ticketNumber"
                    )
                )
            } catch (e: Exception) {
                _scanResult.value = ScanResult.Error("Processing error: ${e.message}")
            } finally {
                _isProcessing.value = false
            }
        }
    }

    /**
     * Core ticket processing logic implementing Case A and Case B.
     */
    private suspend fun processTicket(
        ticketId: String,
        newTicketNumber: Int,
        manualPrice: Int? = null
    ): ScanResult {
        val existingTicket = ticketDao.getTicketById(ticketId)

        return if (existingTicket != null) {
            processExistingTicket(existingTicket, newTicketNumber)
        } else {
            processNewTicket(ticketId, newTicketNumber, manualPrice)
        }
    }

    /**
     * Case A: Existing Ticket (Previously Scanned)
     */
    private suspend fun processExistingTicket(
        existingTicket: Ticket,
        newTicketNumber: Int
    ): ScanResult {
        val oldTicketNumber = existingTicket.ticketNumber
        val now = System.currentTimeMillis()

        return when {
            // Normal sale: new ticket number is less than old (tickets were sold)
            newTicketNumber < oldTicketNumber -> {
                val ticketsSold = oldTicketNumber - newTicketNumber
                val saleAmount = ticketsSold * existingTicket.ticketPrice

                val updatedTicket = existingTicket.copy(
                    ticketNumber = newTicketNumber,
                    totalSale = existingTicket.totalSale + saleAmount,
                    lastScanTimestamp = now,
                    isSoldOut = newTicketNumber == 0
                )

                ticketDao.updateTicket(updatedTicket)
                _lastScannedTicket.value = updatedTicket

                // Log the sale
                auditLogDao.insert(
                    AuditLog(
                        ticketId = existingTicket.ticketId,
                        actionType = AuditLog.ACTION_SALE,
                        amountChange = saleAmount,
                        details = "Sold $ticketsSold tickets ($oldTicketNumber -> $newTicketNumber)"
                    )
                )

                ScanResult.SaleRecorded(
                    ticket = updatedTicket,
                    saleAmount = saleAmount,
                    ticketsSold = ticketsSold
                )
            }

            // Quantity increased (unusual case)
            newTicketNumber > oldTicketNumber -> {
                ScanResult.QuantityIncreased(
                    ticket = existingTicket,
                    oldTicketNumber = oldTicketNumber,
                    newTicketNumber = newTicketNumber
                )
            }

            // Same ticket number — no change
            else -> {
                val updatedTicket = existingTicket.copy(lastScanTimestamp = now)
                ticketDao.updateTicket(updatedTicket)
                _lastScannedTicket.value = updatedTicket
                ScanResult.NoChange(updatedTicket)
            }
        }
    }

    /**
     * Case B: New Ticket (First Scan)
     */
    private suspend fun processNewTicket(
        ticketId: String,
        ticketNumber: Int,
        manualPrice: Int? = null
    ): ScanResult {
        val price = manualPrice ?: PriceMatrix.getPrice(ticketNumber)

        val needsManualPrice = price == null

        val ticketPrice = price ?: 0 // Will be updated later if manual price needed

        val newTicket = Ticket(
            ticketId = ticketId,
            ticketNumber = ticketNumber,
            ticketPrice = ticketPrice,
            totalSale = 0,
            lastScanTimestamp = System.currentTimeMillis(),
            isSoldOut = false
        )

        ticketDao.insertTicket(newTicket)
        _lastScannedTicket.value = newTicket

        // Log new ticket creation
        auditLogDao.insert(
            AuditLog(
                ticketId = ticketId,
                actionType = AuditLog.ACTION_NEW,
                details = "New ticket: qty=$ticketNumber, price=${PriceMatrix.formatPrice(ticketPrice)}"
            )
        )

        return ScanResult.NewTicket(
            ticket = newTicket,
            needsManualPrice = needsManualPrice
        )
    }

    /**
     * Handle the "quantity increased" scenario when user confirms remaining stock was sold.
     */
    fun confirmRemainingStockSold(ticket: Ticket) {
        viewModelScope.launch {
            val saleAmount = ticket.ticketNumber * ticket.ticketPrice
            val updatedTicket = ticket.copy(
                totalSale = ticket.totalSale + saleAmount,
                ticketNumber = 0,
                isSoldOut = true,
                lastScanTimestamp = System.currentTimeMillis()
            )

            ticketDao.updateTicket(updatedTicket)
            _lastScannedTicket.value = updatedTicket

            auditLogDao.insert(
                AuditLog(
                    ticketId = ticket.ticketId,
                    actionType = AuditLog.ACTION_SOLD_OUT,
                    amountChange = saleAmount,
                    details = "Remaining ${ticket.ticketNumber} tickets marked as sold"
                )
            )

            _scanResult.value = ScanResult.SaleRecorded(
                ticket = updatedTicket,
                saleAmount = saleAmount,
                ticketsSold = ticket.ticketNumber
            )
        }
    }

    /**
     * Update the price of a ticket manually.
     */
    fun updateTicketPrice(ticketId: String, newPriceInCents: Int) {
        viewModelScope.launch {
            val ticket = ticketDao.getTicketById(ticketId) ?: return@launch
            val updatedTicket = ticket.copy(ticketPrice = newPriceInCents)
            ticketDao.updateTicket(updatedTicket)
            _lastScannedTicket.value = updatedTicket

            auditLogDao.insert(
                AuditLog(
                    ticketId = ticketId,
                    actionType = AuditLog.ACTION_PRICE_OVERRIDE,
                    details = "Price updated to ${PriceMatrix.formatPrice(newPriceInCents)}"
                )
            )
        }
    }

    /**
     * Clear all data from the database.
     */
    fun clearAllData() {
        viewModelScope.launch {
            ticketDao.deleteAllTickets()
            auditLogDao.deleteAllLogs()
            _lastScannedTicket.value = null
            _scanResult.value = null
        }
    }

    /**
     * Get audit logs for a specific ticket.
     */
    fun getLogsForTicket(ticketId: String): LiveData<List<AuditLog>> {
        return auditLogDao.getLogsForTicket(ticketId)
    }
}
