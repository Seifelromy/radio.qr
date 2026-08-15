package com.example.data.repository

import com.example.data.db.DiagnosticDao
import com.example.data.db.DiagnosticLogEntity
import com.example.data.db.StreamDao
import com.example.data.db.StreamEntity
import com.example.data.pref.SettingsRepository
import kotlinx.coroutines.flow.Flow

class StreamRepository(
    private val streamDao: StreamDao,
    private val diagnosticDao: DiagnosticDao,
    private val settingsRepository: SettingsRepository
) {
    val manager: RadioStreamManager = RadioStreamManager.getInstance(streamDao, diagnosticDao, settingsRepository)

    val allStreams: Flow<List<StreamEntity>> = manager.allStreams
    val diagnosticLogs: Flow<List<DiagnosticLogEntity>> = manager.diagnosticLogs

    suspend fun getActiveStream(): StreamEntity? = manager.getActiveStream()

    suspend fun reportStreamFailure(url: String, errorMessage: String) = 
        manager.reportStreamFailure(url, errorMessage)

    suspend fun performFailover(failedUrl: String): StreamEntity? = 
        manager.performFailover(failedUrl)

    suspend fun logEvent(eventType: String, message: String, streamUrl: String = "") = 
        manager.logEvent(eventType, message, streamUrl)

    suspend fun validateAudioStream(url: String): Triple<Boolean, Long, String> = 
        manager.validateAudioStream(url)

    suspend fun testAndRankStreams() = manager.testAndRankStreams()

    suspend fun resetAllStreams() = manager.resetAllStreams()
}
