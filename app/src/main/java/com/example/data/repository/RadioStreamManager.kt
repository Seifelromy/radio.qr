package com.example.data.repository

import com.example.data.db.DiagnosticDao
import com.example.data.db.DiagnosticLogEntity
import com.example.data.db.StreamDao
import com.example.data.db.StreamEntity
import com.example.data.pref.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class RadioStreamManager private constructor(
    private val streamDao: StreamDao,
    private val diagnosticDao: DiagnosticDao,
    private val settingsRepository: SettingsRepository
) {
    companion object {
        @Volatile
        private var INSTANCE: RadioStreamManager? = null

        fun getInstance(
            streamDao: StreamDao,
            diagnosticDao: DiagnosticDao,
            settingsRepository: SettingsRepository
        ): RadioStreamManager {
            return INSTANCE ?: synchronized(this) {
                val instance = RadioStreamManager(streamDao, diagnosticDao, settingsRepository)
                INSTANCE = instance
                instance
            }
        }
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(2000, TimeUnit.MILLISECONDS)
        .readTimeout(2500, TimeUnit.MILLISECONDS)
        .build()

    val allStreams: Flow<List<StreamEntity>> = streamDao.getAllStreamsFlow()
    val diagnosticLogs: Flow<List<DiagnosticLogEntity>> = diagnosticDao.getLatestLogsFlow()

    suspend fun logEvent(eventType: String, message: String, streamUrl: String = "") {
        diagnosticDao.insertLog(DiagnosticLogEntity(eventType = eventType, message = message, streamUrl = streamUrl))
    }

    suspend fun getActiveStream(): StreamEntity? = withContext(Dispatchers.IO) {
        val preferredUrl = settingsRepository.preferredStreamUrl.value
        val list = streamDao.getAllStreams()
        
        val preferredStream = list.find { it.url == preferredUrl }
        if (preferredStream != null && preferredStream.isHealthy) {
            return@withContext preferredStream
        }
        
        return@withContext list.firstOrNull { it.isHealthy } ?: list.firstOrNull()
    }

    suspend fun reportStreamFailure(url: String, errorMessage: String) = withContext(Dispatchers.IO) {
        val list = streamDao.getAllStreams()
        val stream = list.find { it.url == url }
        if (stream != null) {
            val newFailures = stream.failureCount + 1
            val isHealthyNow = newFailures < 3
            streamDao.updateStreamStatus(stream.id, newFailures, isHealthyNow)
            logEvent("ERROR", "عطل في خادم [${stream.displayNameAr}]: $errorMessage. إخفاقات متتالية: $newFailures", url)
        }
    }

    suspend fun performFailover(failedUrl: String): StreamEntity? = withContext(Dispatchers.IO) {
        reportStreamFailure(failedUrl, "انقطع تدفق الصوت أو انتهت مهلة الاتصال")
        
        val nextStream = getActiveStream()
        if (nextStream != null && nextStream.url != failedUrl) {
            logEvent("FAILOVER", "انتقال تلقائي ذكي وحماية حلقة الاستماع للمسرى: [${nextStream.displayNameAr}]", nextStream.url)
            return@withContext nextStream
        }
        return@withContext nextStream
    }

    suspend fun checkForRecovery(currentUrl: String): StreamEntity? = withContext(Dispatchers.IO) {
        val list = streamDao.getAllStreams()
        val primaryStream = list.firstOrNull { it.id == 1 } ?: return@withContext null
        
        if (currentUrl != primaryStream.url) {
            val (isValid, latency, _) = validateAudioStream(primaryStream.url)
            if (isValid && latency < 3000) {
                // Primary has recovered! Reset failure count and mark healthy
                streamDao.updateStreamStatus(primaryStream.id, 0, true)
                logEvent("SUCCESS", "استرداد مسرى البث الرئيسي: [${primaryStream.displayNameAr}] يعمل بنبض استجابة ${latency}ms. جاري التحويل التلقائي...", primaryStream.url)
                return@withContext primaryStream
            }
        }
        return@withContext null
    }

    suspend fun validateAudioStream(url: String): Triple<Boolean, Long, String> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        try {
            val request = Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 10) CairoQuranRadio/1.0")
                .header("Connection", "close")
                .build()
                
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Triple(false, 9999L, "HTTP ${response.code}")
                }
                
                val contentType = response.header("Content-Type")?.lowercase() ?: ""
                val isValidFormat = contentType.contains("audio") || 
                                   contentType.contains("mpeg") || 
                                   contentType.contains("ogg") || 
                                   contentType.contains("application/octet-stream") ||
                                   contentType.contains("video") ||
                                   contentType.contains("octet-stream")

                if (!isValidFormat) {
                    return@withContext Triple(false, 9999L, "نوع غير صالح: $contentType")
                }
                
                val body = response.body ?: return@withContext Triple(false, 9999L, "مسرى فارغ")
                val source = body.source()
                if (source.exhausted()) {
                    return@withContext Triple(false, 9999L, "محتوى فارغ")
                }
                
                val buffer = ByteArray(256)
                val bytesRead = source.read(buffer)
                if (bytesRead <= 0) {
                    return@withContext Triple(false, 9999L, "فشل قراءة البايتات")
                }
                
                val latency = System.currentTimeMillis() - startTime
                val bitrate = response.header("icy-br") ?: "128"
                return@withContext Triple(true, latency, bitrate)
            }
        } catch (e: Exception) {
            val errorMsg = when (e) {
                is java.net.SocketTimeoutException -> "انتهت مهلة الاتصال"
                is IOException -> "خطأ شبكة"
                else -> e.localizedMessage ?: "فشل فني"
            }
            return@withContext Triple(false, 9999L, errorMsg)
        }
    }

    suspend fun testAndRankStreams() = withContext(Dispatchers.IO) {
        logEvent("RECONNECT", "بدء فحص دوري وتقييم جودة خوادم البث الثلاثة المتاحة...")
        val streams = streamDao.getAllStreams()
        var healthyCount = 0
        
        val testedList = streams.map { stream ->
            val (isValid, latency, quality) = validateAudioStream(stream.url)
            val finalLatency = if (isValid) latency else 9999L
            
            val score = if (isValid) {
                var baseScore = 100
                baseScore -= (finalLatency / 50).toInt().coerceAtMost(40) // Latency penalty
                baseScore -= (stream.failureCount * 15) // Failure penalty
                baseScore += when (stream.id) {
                    1 -> 50 // Official Primary
                    2 -> 25 // Mirror Backup
                    3 -> 10 // Community Backup
                    else -> 0
                }
                baseScore.coerceIn(0, 150)
            } else {
                0
            }

            val updated = stream.copy(
                latencyMs = finalLatency,
                failureCount = if (isValid) 0 else (stream.failureCount + 1),
                isHealthy = isValid,
                rank = 150 - score // Lower rank means higher priority
            )
            
            if (isValid) {
                healthyCount++
                logEvent("SUCCESS", "خادم [${stream.displayNameAr}] مستقر بنبض ${latency}ms وجودة ${quality}kbps. درجة جودته: $score/150", stream.url)
            } else {
                logEvent("ERROR", "فشل فحص الخادم [${stream.displayNameAr}]. السبب: $quality", stream.url)
            }
            updated
        }
        
        val sortedList = testedList.sortedWith(
            compareBy<StreamEntity> { !it.isHealthy }
                .thenBy { it.rank }
        )
        
        sortedList.forEachIndexed { index, entity ->
            streamDao.updateStream(entity.copy(rank = index + 1))
        }
        
        logEvent("SUCCESS", "اكتمل ترتيب الخوادم بنجاح. القنوات الجاهزة للخدمة: ($healthyCount من ${streams.size})")
    }

    suspend fun resetAllStreams() = withContext(Dispatchers.IO) {
        streamDao.resetAllStreamStatuses()
        diagnosticDao.clearLogs()
        logEvent("SUCCESS", "تم تصفير سجلات التشخيص وإعادة تهيئة القنوات.")
    }
}
