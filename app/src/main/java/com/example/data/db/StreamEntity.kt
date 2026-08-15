package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "streams")
data class StreamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val name: String,
    val displayNameAr: String,
    val failureCount: Int = 0,
    val latencyMs: Long = 0,
    val isPreferred: Boolean = false,
    val isHealthy: Boolean = true,
    val rank: Int = 0
)
