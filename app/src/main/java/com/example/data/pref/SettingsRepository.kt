package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("mizan_radio_settings", Context.MODE_PRIVATE)

    companion object {
        const val KEY_AUTO_RECONNECT = "auto_reconnect"
        const val KEY_PREFERRED_STREAM = "preferred_stream"
        const val KEY_THEME_MODE = "theme_mode"
        const val KEY_BACKGROUND_PLAYBACK = "background_playback"

        // Location settings
        const val KEY_LOCATION_MODE = "location_mode" // "AUTO" or "MANUAL"
        const val KEY_MANUAL_CITY = "manual_city"
        const val KEY_MANUAL_LATITUDE = "manual_latitude"
        const val KEY_MANUAL_LONGITUDE = "manual_longitude"
        const val KEY_AUTO_LATITUDE = "auto_latitude"
        const val KEY_AUTO_LONGITUDE = "auto_longitude"
        const val KEY_AUTO_CITY = "auto_city"

        // Hijri Adjustment
        const val KEY_HIJRI_ADJUSTMENT = "hijri_adjustment" // -1, 0, +1

        // Adhkar Favorites and Progress
        const val KEY_ADHKAR_FAVORITES = "adhkar_favorites"
        const val KEY_ADHKAR_LAST_RESET_DAY = "adhkar_last_reset_day"

        // Future Notifications Settings
        const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val KEY_NOTIFY_FAJR = "notify_fajr"
        const val KEY_NOTIFY_SUNRISE = "notify_sunrise"
        const val KEY_NOTIFY_DHUHR = "notify_dhuhr"
        const val KEY_NOTIFY_ASR = "notify_asr"
        const val KEY_NOTIFY_MAGHRIB = "notify_maghrib"
        const val KEY_NOTIFY_ISHA = "notify_isha"
    }

    private val _autoReconnect = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RECONNECT, true))
    val autoReconnect: StateFlow<Boolean> = _autoReconnect.asStateFlow()

    private val _preferredStreamUrl = MutableStateFlow(prefs.getString(KEY_PREFERRED_STREAM, "https://stream.radiojar.com/8s5u5tpdtwzuv") ?: "https://stream.radiojar.com/8s5u5tpdtwzuv")
    val preferredStreamUrl: StateFlow<String> = _preferredStreamUrl.asStateFlow()

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "LIGHT") ?: "LIGHT")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _backgroundPlayback = MutableStateFlow(prefs.getBoolean(KEY_BACKGROUND_PLAYBACK, true))
    val backgroundPlayback: StateFlow<Boolean> = _backgroundPlayback.asStateFlow()

    // Location Mode Flow
    private val _locationMode = MutableStateFlow(prefs.getString(KEY_LOCATION_MODE, "MANUAL") ?: "MANUAL")
    val locationMode: StateFlow<String> = _locationMode.asStateFlow()

    // Manual selected City and Coordinates
    private val _manualCity = MutableStateFlow(prefs.getString(KEY_MANUAL_CITY, "القاهرة") ?: "القاهرة")
    val manualCity: StateFlow<String> = _manualCity.asStateFlow()

    private val _manualLatitude = MutableStateFlow(prefs.getFloat(KEY_MANUAL_LATITUDE, 30.0444f))
    val manualLatitude: StateFlow<Float> = _manualLatitude.asStateFlow()

    private val _manualLongitude = MutableStateFlow(prefs.getFloat(KEY_MANUAL_LONGITUDE, 31.2357f))
    val manualLongitude: StateFlow<Float> = _manualLongitude.asStateFlow()

    // Auto GPS Coordinates
    private val _autoLatitude = MutableStateFlow(prefs.getFloat(KEY_AUTO_LATITUDE, 30.0444f))
    val autoLatitude: StateFlow<Float> = _autoLatitude.asStateFlow()

    private val _autoLongitude = MutableStateFlow(prefs.getFloat(KEY_AUTO_LONGITUDE, 31.2357f))
    val autoLongitude: StateFlow<Float> = _autoLongitude.asStateFlow()

    private val _autoCity = MutableStateFlow(prefs.getString(KEY_AUTO_CITY, "موقعك الجغرافي") ?: "موقعك الجغرافي")
    val autoCity: StateFlow<String> = _autoCity.asStateFlow()

    // Hijri Calendar Adjustment
    private val _hijriAdjustment = MutableStateFlow(prefs.getInt(KEY_HIJRI_ADJUSTMENT, 0))
    val hijriAdjustment: StateFlow<Int> = _hijriAdjustment.asStateFlow()

    // Favorites & Notification Preferences
    private val _adhkarFavorites = MutableStateFlow(prefs.getStringSet(KEY_ADHKAR_FAVORITES, emptySet()) ?: emptySet())
    val adhkarFavorites: StateFlow<Set<String>> = _adhkarFavorites.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_AUTO_RECONNECT -> _autoReconnect.value = prefs.getBoolean(KEY_AUTO_RECONNECT, true)
            KEY_PREFERRED_STREAM -> _preferredStreamUrl.value = prefs.getString(KEY_PREFERRED_STREAM, "https://stream.radiojar.com/8s5u5tpdtwzuv") ?: "https://stream.radiojar.com/8s5u5tpdtwzuv"
            KEY_THEME_MODE -> _themeMode.value = prefs.getString(KEY_THEME_MODE, "LIGHT") ?: "LIGHT"
            KEY_BACKGROUND_PLAYBACK -> _backgroundPlayback.value = prefs.getBoolean(KEY_BACKGROUND_PLAYBACK, true)
            
            KEY_LOCATION_MODE -> _locationMode.value = prefs.getString(KEY_LOCATION_MODE, "MANUAL") ?: "MANUAL"
            KEY_MANUAL_CITY -> _manualCity.value = prefs.getString(KEY_MANUAL_CITY, "القاهرة") ?: "القاهرة"
            KEY_MANUAL_LATITUDE -> _manualLatitude.value = prefs.getFloat(KEY_MANUAL_LATITUDE, 30.0444f)
            KEY_MANUAL_LONGITUDE -> _manualLongitude.value = prefs.getFloat(KEY_MANUAL_LONGITUDE, 31.2357f)
            KEY_AUTO_LATITUDE -> _autoLatitude.value = prefs.getFloat(KEY_AUTO_LATITUDE, 30.0444f)
            KEY_AUTO_LONGITUDE -> _autoLongitude.value = prefs.getFloat(KEY_AUTO_LONGITUDE, 31.2357f)
            KEY_AUTO_CITY -> _autoCity.value = prefs.getString(KEY_AUTO_CITY, "موقعك الجغرافي") ?: "موقعك الجغرافي"
            
            KEY_HIJRI_ADJUSTMENT -> _hijriAdjustment.value = prefs.getInt(KEY_HIJRI_ADJUSTMENT, 0)
            KEY_ADHKAR_FAVORITES -> _adhkarFavorites.value = prefs.getStringSet(KEY_ADHKAR_FAVORITES, emptySet()) ?: emptySet()
            KEY_NOTIFICATIONS_ENABLED -> _notificationsEnabled.value = prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun setAutoReconnect(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RECONNECT, enabled).apply()
    }

    fun setPreferredStreamUrl(url: String) {
        prefs.edit().putString(KEY_PREFERRED_STREAM, url).apply()
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_PLAYBACK, enabled).apply()
    }

    fun setLocationMode(mode: String) {
        prefs.edit().putString(KEY_LOCATION_MODE, mode).apply()
    }

    fun setManualCity(city: String, lat: Float, lon: Float) {
        prefs.edit()
            .putString(KEY_MANUAL_CITY, city)
            .putFloat(KEY_MANUAL_LATITUDE, lat)
            .putFloat(KEY_MANUAL_LONGITUDE, lon)
            .apply()
    }

    fun setAutoLocation(lat: Float, lon: Float, cityName: String) {
        prefs.edit()
            .putFloat(KEY_AUTO_LATITUDE, lat)
            .putFloat(KEY_AUTO_LONGITUDE, lon)
            .putString(KEY_AUTO_CITY, cityName)
            .apply()
    }

    fun setHijriAdjustment(adjustment: Int) {
        prefs.edit().putInt(KEY_HIJRI_ADJUSTMENT, adjustment).apply()
    }

    // Adhkar actions
    fun isFavorite(id: String): Boolean {
        return _adhkarFavorites.value.contains(id)
    }

    fun toggleFavorite(id: String) {
        val current = _adhkarFavorites.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        prefs.edit().putStringSet(KEY_ADHKAR_FAVORITES, current).apply()
    }

    fun getThikrProgress(id: String): Int {
        return prefs.getInt("thikr_progress_$id", 0)
    }

    fun incrementThikrProgress(id: String, max: Int): Int {
        val current = getThikrProgress(id)
        if (current < max) {
            val newProgress = current + 1
            prefs.edit().putInt("thikr_progress_$id", newProgress).apply()
            return newProgress
        }
        return current
    }

    fun resetThikrProgress(id: String) {
        prefs.edit().remove("thikr_progress_$id").apply()
    }

    fun getAdhkarLastResetDay(): Int {
        return prefs.getInt(KEY_ADHKAR_LAST_RESET_DAY, 0)
    }

    fun setAdhkarLastResetDay(day: Int) {
        prefs.edit().putInt(KEY_ADHKAR_LAST_RESET_DAY, day).apply()
    }

    // Notifications methods
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun isNotificationForPrayerEnabled(prayerKey: String): Boolean {
        return prefs.getBoolean(prayerKey, true)
    }

    fun setNotificationForPrayerEnabled(prayerKey: String, enabled: Boolean) {
        prefs.edit().putBoolean(prayerKey, enabled).apply()
    }
}
