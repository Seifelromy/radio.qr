package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticDao {
    @Query("SELECT * FROM diagnostic_logs ORDER BY timestamp DESC LIMIT 50")
    fun getLatestLogsFlow(): Flow<List<DiagnosticLogEntity>>

    @Insert
    suspend fun insertLog(log: DiagnosticLogEntity)

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearLogs()
}
