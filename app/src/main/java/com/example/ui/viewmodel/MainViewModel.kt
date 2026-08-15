package com.example.ui.viewmodel

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.data.db.AppDatabase
import com.example.data.db.DiagnosticLogEntity
import com.example.data.db.StreamEntity
import com.example.data.pref.SettingsRepository
import com.example.data.repository.StreamRepository
import com.example.playback.PlaybackService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class City(
    val nameAr: String,
    val nameEn: String,
    val latitude: Double,
    val longitude: Double
)

class MainViewModel(
    private val context: Context,
    private val streamRepository: StreamRepository,
    val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _mediaController = MutableStateFlow<MediaController?>(null)
    val mediaController = _mediaController.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackState = MutableStateFlow(Player.STATE_IDLE)
    val playbackState: StateFlow<Int> = _playbackState.asStateFlow()

    private val _currentPlayingUrl = MutableStateFlow<String?>(null)
    val currentPlayingUrl: StateFlow<String?> = _currentPlayingUrl.asStateFlow()

    private val _isInternetAvailable = MutableStateFlow(true)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable.asStateFlow()

    private val _isRetrievingLocation = MutableStateFlow(false)
    val isRetrievingLocation: StateFlow<Boolean> = _isRetrievingLocation.asStateFlow()

    val allStreams: StateFlow<List<StreamEntity>> = streamRepository.allStreams
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val diagnosticLogs: StateFlow<List<DiagnosticLogEntity>> = streamRepository.diagnosticLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val citiesList = listOf(
        City("القاهرة", "Cairo", 30.0444, 31.2357),
        City("الإسكندرية", "Alexandria", 31.2001, 29.9187),
        City("الجيزة", "Giza", 30.0131, 31.2089),
        City("بورسعيد", "Port Said", 31.2653, 32.3019),
        City("السويس", "Suez", 29.9668, 32.5498),
        City("طنطا", "Tanta", 30.7865, 31.0004),
        City("المنصورة", "Mansoura", 31.0409, 31.3785),
        City("المحلة الكبرى", "Mahalla", 30.9763, 31.1686),
        City("الزقازيق", "Zagazig", 30.5877, 31.5020),
        City("الفيوم", "Fayoum", 29.3084, 30.8428),
        City("الإسماعيلية", "Ismailia", 30.6043, 32.2723),
        City("أسيوط", "Asyut", 27.1783, 31.1837),
        City("الأقصر", "Luxor", 25.6872, 32.6396),
        City("أسوان", "Aswan", 24.0889, 32.8998)
    )

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private var isFirstConnectionInit = true

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            viewModelScope.launch {
                _isInternetAvailable.value = true
                streamRepository.logEvent("SUCCESS", "تم الاتصال بالإنترنت بنجاح.", "")
                
                if (settingsRepository.autoReconnect.value) {
                    if (!isFirstConnectionInit) {
                        streamRepository.logEvent("RECONNECT", "عودة الاتصال بالشبكة ورصد الخدمة النشطة. جاري قياس نبض الخوادم وإعادة ربط البث…", "")
                        streamRepository.testAndRankStreams()
                        
                        val active = streamRepository.getActiveStream()
                        if (active != null && _isPlaying.value) {
                            playStream(active)
                        }
                    }
                }
                isFirstConnectionInit = false
            }
        }

        override fun onLost(network: Network) {
            _isInternetAvailable.value = false
            viewModelScope.launch {
                streamRepository.logEvent("ERROR", "فُقد الاتصال بالإنترنت. يرجى التحقق من الشبكة الخاصة بك.", "")
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }

        override fun onPlaybackStateChanged(state: Int) {
            _playbackState.value = state
            if (state == Player.STATE_READY) {
                _currentPlayingUrl.value = _mediaController.value?.currentMediaItem?.localConfiguration?.uri?.toString()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _currentPlayingUrl.value = mediaItem?.localConfiguration?.uri?.toString()
        }
    }

    init {
        initMediaController()
        registerNetworkObserver()
        checkActiveInternet()
        checkAdhkarReset()

        // Dynamic initial testing & active health monitoring every 3 minutes
        viewModelScope.launch {
            while (true) {
                try {
                    streamRepository.testAndRankStreams()
                } catch (e: Exception) {
                    // Safe catch
                }
                kotlinx.coroutines.delay(180_000)
            }
        }
    }

    private fun checkAdhkarReset() {
        val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val lastReset = settingsRepository.getAdhkarLastResetDay()
        if (lastReset != currentDay) {
            resetAllAdhkarProgress()
            settingsRepository.setAdhkarLastResetDay(currentDay)
        }
    }

    private fun resetAllAdhkarProgress() {
        val thikrIds = listOf(
            "morning_1", "morning_2", "morning_3", "morning_4", "morning_5",
            "evening_1", "evening_2", "evening_3", "evening_4", "evening_5",
            "sleep_1", "sleep_2", "sleep_3",
            "wakeup_1", "wakeup_2",
            "after_prayer_1", "after_prayer_2", "after_prayer_3"
        )
        for (id in thikrIds) {
            settingsRepository.resetThikrProgress(id)
        }
    }

    private fun initMediaController() {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        
        controllerFuture.addListener({
            try {
                val controller = controllerFuture.get()
                _mediaController.value = controller
                _isPlaying.value = controller.isPlaying
                _playbackState.value = controller.playbackState
                _currentPlayingUrl.value = controller.currentMediaItem?.localConfiguration?.uri?.toString()
                controller.addListener(playerListener)
                
                // Autoplay logic if there's no active background session
                if (controller.currentMediaItem == null || controller.playbackState == Player.STATE_IDLE) {
                    viewModelScope.launch {
                        streamRepository.logEvent("STARTUP", "بدء التشغيل التلقائي للتطبيق المعتمد...")
                        val primaryStream = streamRepository.getActiveStream()
                        if (primaryStream != null) {
                            playStream(primaryStream)
                        }
                    }
                }
            } catch (e: Exception) {
                viewModelScope.launch {
                    streamRepository.logEvent("ERROR", "فشل ربط جهاز التحكم بجلسة البث الخلفي: ${e.localizedMessage}")
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun registerNetworkObserver() {
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    private fun checkActiveInternet() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        _isInternetAvailable.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }

    fun playStream(stream: StreamEntity) {
        val controller = _mediaController.value ?: return
        
        viewModelScope.launch {
            streamRepository.logEvent("SUCCESS", "بدء تشغيل قناة البث المحدد يدوياً: ${stream.displayNameAr}", stream.url)
        }

        val metadata = MediaMetadata.Builder()
            .setTitle("إذاعة القرآن الكريم من القاهرة")
            .setArtist("الجهير المباشر من القاهرة")
            .setDisplayTitle(stream.displayNameAr)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(stream.url)
            .setMediaMetadata(metadata)
            .build()

        controller.setMediaItem(mediaItem)
        controller.prepare()
        controller.play()
        _currentPlayingUrl.value = stream.url
    }

    fun togglePlayback() {
        val controller = _mediaController.value ?: return
        if (controller.isPlaying) {
            controller.pause()
            _isPlaying.value = false
            viewModelScope.launch {
                streamRepository.logEvent("SUCCESS", "إيقاف مؤقت للبث الإذاعي بناءً على رغبة المستمع.")
            }
        } else {
            if (!_isInternetAvailable.value) {
                viewModelScope.launch {
                    streamRepository.logEvent("ERROR", "تعذر الربط بسبب انقطاع الاتصال بالشبكة.")
                }
                return
            }
            viewModelScope.launch {
                val active = streamRepository.getActiveStream()
                if (active != null) {
                    playStream(active)
                }
            }
        }
    }

    fun triggerSpeedTest() {
        viewModelScope.launch {
            streamRepository.testAndRankStreams()
        }
    }

    fun resetDiagnostics() {
        viewModelScope.launch {
            streamRepository.resetAllStreams()
        }
    }

    fun changePreferredStream(stream: StreamEntity) {
        viewModelScope.launch {
            settingsRepository.setPreferredStreamUrl(stream.url)
            streamRepository.logEvent("SUCCESS", "تعديل المسرى الرئيسي المفضل بنجاح إلى: [${stream.displayNameAr}]", stream.url)
            
            if (_isPlaying.value) {
                playStream(stream)
            }
        }
    }

    fun changeThemeMode(mode: String) {
        settingsRepository.setThemeMode(mode)
    }

    fun toggleAutoReconnect(enabled: Boolean) {
        settingsRepository.setAutoReconnect(enabled)
    }

    fun toggleBackgroundPlayback(enabled: Boolean) {
        settingsRepository.setBackgroundPlayback(enabled)
    }

    // Location Retrieval and travel-aware updates
    fun retrieveGPSLocation() {
        _isRetrievingLocation.value = true
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            
            var bestLocation: Location? = null
            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                val loc = locationManager.getLastKnownLocation(provider) ?: continue
                if (bestLocation == null || loc.accuracy < bestLocation.accuracy) {
                    bestLocation = loc
                }
            }

            if (bestLocation != null) {
                updateGPSCoordinates(bestLocation.latitude, bestLocation.longitude)
            } else {
                try {
                    val listener = object : LocationListener {
                        override fun onLocationChanged(location: Location) {
                            updateGPSCoordinates(location.latitude, location.longitude)
                            locationManager.removeUpdates(this)
                        }
                        @Deprecated("Deprecated")
                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {}
                    }

                    val provider = if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        LocationManager.NETWORK_PROVIDER
                    } else if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        LocationManager.GPS_PROVIDER
                    } else {
                        null
                    }

                    if (provider != null) {
                        locationManager.requestSingleUpdate(provider, listener, context.mainLooper)
                    } else {
                        _isRetrievingLocation.value = false
                        viewModelScope.launch {
                            streamRepository.logEvent("ERROR", "تعذر تحديد الموقع تلقائياً. يرجى التأكد من تفعيل خدمة GPS بالهاتف.")
                        }
                    }
                } catch (e: Exception) {
                    _isRetrievingLocation.value = false
                    viewModelScope.launch {
                        streamRepository.logEvent("ERROR", "خطأ أثناء محاولة رصد الموقع الجغرافي: ${e.localizedMessage}")
                    }
                }
            }
        } else {
            _isRetrievingLocation.value = false
        }
    }

    private fun updateGPSCoordinates(latitude: Double, longitude: Double) {
        val nearest = getNearestCity(latitude, longitude)
        settingsRepository.setAutoLocation(latitude.toFloat(), longitude.toFloat(), "${nearest.nameAr} (تلقائي)")
        _isRetrievingLocation.value = false
        viewModelScope.launch {
            streamRepository.logEvent("SUCCESS", "تم تحديد موقعك التلقائي بنجاح: ${nearest.nameAr} (${String.format(java.util.Locale.US, "%.4f", latitude)}, ${String.format(java.util.Locale.US, "%.4f", longitude)})")
        }
    }

    fun getNearestCity(lat: Double, lon: Double): City {
        var nearest = citiesList.first()
        var minDistance = Double.MAX_VALUE
        for (city in citiesList) {
            val d = Math.pow(city.latitude - lat, 2.0) + Math.pow(city.longitude - lon, 2.0)
            if (d < minDistance) {
                minDistance = d
                nearest = city
            }
        }
        return nearest
    }

    override fun onCleared() {
        _mediaController.value?.removeListener(playerListener)
        connectivityManager.unregisterNetworkCallback(networkCallback)
        super.onCleared()
    }
}

class MainViewModelFactory(
    private val context: Context,
    private val streamRepository: StreamRepository,
    private val settingsRepository: SettingsRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(context, streamRepository, settingsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
