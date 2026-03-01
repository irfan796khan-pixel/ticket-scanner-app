package com.ticketscanner.app

import android.app.Application
import com.ticketscanner.app.data.database.AppDatabase

class TicketScannerApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: TicketScannerApp
            private set
    }
}
