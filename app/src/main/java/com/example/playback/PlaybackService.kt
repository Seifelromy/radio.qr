package com.example.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.pref.SettingsRepository
import com.example.data.repository.StreamRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    
    private val serviceJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.Main + serviceJob)
    
    private lateinit var streamRepository: StreamRepository
    private lateinit var settingsRepository: SettingsRepository
    private var bufferingJob: kotlinx.coroutines.Job? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        
        val database = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)
        streamRepository = StreamRepository(database.streamDao(), database.diagnosticDao(), settingsRepository)
        
        // Fast, aggressive HTTP timeouts (2s connect, 2.5s read) to avoid hanging
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setConnectTimeoutMs(2000)
            .setReadTimeoutMs(2500)
            .setAllowCrossProtocolRedirects(true)

        // Aggressive buffering parameters to minimize startup latency (under 1s) while maintaining stability
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                1000, // Min buffer before playing (reduces startup time)
                5000, // Max buffer to maintain
                500,  // Buffer required to startup
                800   // Buffer required to resume after starvation
            )
            .build()

        // Custom error handling policy: fail fast with 1 retry for live streams
        val loadErrorHandlingPolicy = object : DefaultLoadErrorHandlingPolicy() {
            override fun getMinimumLoadableRetryCount(dataType: Int): Int {
                return 1
            }
        }
            
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(dataSourceFactory)
            .setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
            
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setAudioAttributes(
                AudioAttributes.Builder()
                     .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                     .setUsage(C.USAGE_MEDIA)
                     .build(),
                true // Auto handle audio focus
            )
            .build()
            
        // Keep CPU & Network connection alive during background streaming
        player!!.setWakeMode(C.WAKE_MODE_NETWORK)
            
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
            
        mediaSession = MediaSession.Builder(this, player!!)
            .setSessionActivity(pendingIntent)
            .build()
            
        player!!.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                val failedUrl = player!!.currentMediaItem?.localConfiguration?.uri?.toString() ?: ""
                scope.launch {
                    val autoReconnectEnabled = settingsRepository.autoReconnect.value
                    if (autoReconnectEnabled && failedUrl.isNotEmpty()) {
                        val nextStream = streamRepository.performFailover(failedUrl)
                        if (nextStream != null) {
                            playUrl(nextStream.url, nextStream.displayNameAr)
                        }
                    } else {
                        streamRepository.reportStreamFailure(failedUrl, error.message ?: "عطل مبهم")
                        streamRepository.logEvent("ERROR", "فشل تشغيل القناة وتعديل المسار متوقف حسب رغبة المستمع.", failedUrl)
                    }
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                super.onPlaybackStateChanged(playbackState)
                val activeUrl = player!!.currentMediaItem?.localConfiguration?.uri?.toString() ?: ""
                
                bufferingJob?.cancel()
                
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        scope.launch {
                            streamRepository.logEvent("BUFF", "جاري تحميل وتعبئة ذاكرة التدفق المباشر...", activeUrl)
                        }
                        
                        if (player!!.playWhenReady) {
                            // Rapid failover timer: if buffering stalls for more than 2 seconds, switch stream
                            bufferingJob = scope.launch {
                                delay(2000)
                                streamRepository.logEvent("TIMEOUT", "تجاوزت مهلة تحميل البيانات البث المباشر (2 ثانية). جاري التحويل التلقائي...", activeUrl)
                                val nextStream = streamRepository.performFailover(activeUrl)
                                if (nextStream != null) {
                                    playUrl(nextStream.url, nextStream.displayNameAr)
                                }
                            }
                        }
                    }
                    Player.STATE_READY -> {
                        scope.launch {
                            if (player!!.playWhenReady) {
                                streamRepository.logEvent("SUCCESS", "البث متصل ومستقر. الصوت يتدفق بصورة ممتازة الآن.", activeUrl)
                            }
                        }
                    }
                    else -> {}
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                val activeUrl = player!!.currentMediaItem?.localConfiguration?.uri?.toString() ?: ""
                if (isPlaying) {
                    scope.launch {
                        streamRepository.logEvent("SUCCESS", "تم فك ترميز الصوت وبدء الإشعاع الصوتي المباشر.", activeUrl)
                    }
                }
            }
        })

        // Periodic recovery check loop: every 30 seconds checks if the primary official stream has recovered
        scope.launch {
            while (true) {
                delay(30000)
                try {
                    val isPlaying = player?.isPlaying ?: false
                    val activeUrl = player?.currentMediaItem?.localConfiguration?.uri?.toString() ?: ""
                    val autoReconnectEnabled = settingsRepository.autoReconnect.value
                    
                    if (isPlaying && activeUrl.isNotEmpty() && autoReconnectEnabled) {
                        val recoveredStream = streamRepository.manager.checkForRecovery(activeUrl)
                        if (recoveredStream != null) {
                            playUrl(recoveredStream.url, recoveredStream.displayNameAr)
                        }
                    }
                } catch (e: Exception) {
                    // Safe catch
                }
            }
        }
    }

    private fun playUrl(url: String, displayName: String) {
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle("إذاعة القرآن الكريم من القاهرة")
            .setArtist("القاهرة - مصر")
            .setAlbumTitle("إذاعة القرآن الكريم من القاهرة")
            .setDisplayTitle("بث مباشر بجودة عالية - $displayName")
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(mediaMetadata)
            .build()

        player?.setMediaItem(mediaItem)
        player?.prepare()
        player?.play()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        mediaSession?.run {
            player?.release()
            release()
            mediaSession = null
        }
        player = null
        serviceJob.cancel()
        super.onDestroy()
    }
}
