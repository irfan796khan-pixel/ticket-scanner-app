package com.ticketscanner.app.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ticketscanner.app.data.entity.AuditLog

@Dao
interface AuditLogDao {

    @Insert
    suspend fun insert(auditLog: AuditLog)

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC")
    fun getAllLogs(): LiveData<List<AuditLog>>

    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC")
    suspend fun getAllLogsList(): List<AuditLog>

    @Query("SELECT * FROM audit_log WHERE ticket_id = :ticketId ORDER BY timestamp DESC")
    fun getLogsForTicket(ticketId: String): LiveData<List<AuditLog>>

    @Query("DELETE FROM audit_log")
    suspend fun deleteAllLogs()
}
