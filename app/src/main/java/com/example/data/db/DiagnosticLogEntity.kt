package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_logs")
data class DiagnosticLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val eventType: String, // e.g. "ERROR", "BUFF", "SUCCESS", "FAILOVER", "RECONNECT"
    val message: String,
    val streamUrl: String = ""
)
