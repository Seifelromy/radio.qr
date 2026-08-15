package com.example.data.repository

import java.util.Calendar
import java.util.Locale
import kotlin.math.*

data class PrayerTime(
    val nameAr: String,
    val nameEn: String,
    val timeString: String,
    val dateString: String, // HH:mm format for timing comparison
    val iconName: String,
    val isActive: Boolean = false
)

class PrayerTimesCalculator {
    companion object {
        fun getPrayerTimesForDate(
            calendar: Calendar,
            latitude: Double = 30.0444,
            longitude: Double = 31.2357,
            timezoneOffset: Double = 3.0
        ): List<PrayerTime> {
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            
            // Standard seasonal model for Sunset / Solar Noon / Prayer shifts:
            val declination = 23.45 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 80)))
            val latRad = Math.toRadians(latitude)
            val decRad = Math.toRadians(declination)
            
            // Equation of Time correction in hours
            val eqOfTime = 0.165 * sin(Math.toRadians(2 * 360.0 / 365.0 * (dayOfYear - 81))) - 
                         0.126 * cos(Math.toRadians(360.0 / 365.0 * (dayOfYear - 2))) - 
                         0.025 * sin(Math.toRadians(360.0 / 365.0 * (dayOfYear - 121)))
            
            val solarNoonGMT = 12.0 - (longitude / 15.0) - eqOfTime
            val solarNoonLocal = solarNoonGMT + timezoneOffset
            
            // Sunrise/Sunset hour angles
            val cosSunrise = (-sin(Math.toRadians(-0.833)) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
            val hourAngleSunrise = Math.toDegrees(acos(cosSunrise.coerceIn(-1.0, 1.0))) / 15.0
            
            val sunriseTime = solarNoonLocal - hourAngleSunrise
            val sunsetTime = solarNoonLocal + hourAngleSunrise
            
            // Fajr angle 19.5 degrees for Egypt Survey Authority
            val cosFajr = (-sin(Math.toRadians(-19.5)) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
            val hourAngleFajr = Math.toDegrees(acos(cosFajr.coerceIn(-1.0, 1.0))) / 15.0
            val fajrTime = solarNoonLocal - hourAngleFajr
            
            // Isha angle 17.5 degrees for Egypt
            val cosIsha = (-sin(Math.toRadians(-17.5)) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
            val hourAngleIsha = Math.toDegrees(acos(cosIsha.coerceIn(-1.0, 1.0))) / 15.0
            val ishaTime = solarNoonLocal + hourAngleIsha
            
            // Asr (Shafi'i/Standard: shadow length increases by 1x object length)
            val asrAngle = atan(1.0 + tan(abs(latRad - decRad)))
            val cosAsr = (sin(asrAngle) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
            val hourAngleAsr = Math.toDegrees(acos(cosAsr.coerceIn(-1.0, 1.0))) / 15.0
            val asrTime = solarNoonLocal + hourAngleAsr
            
            return listOf(
                createPrayerTimeObject("الفجر", "Fajr", fajrTime),
                createPrayerTimeObject("الشروق", "Sunrise", sunriseTime),
                createPrayerTimeObject("الظهر", "Dhuhr", solarNoonLocal + (1.0 / 60.0)), // +1 min safety
                createPrayerTimeObject("العصر", "Asr", asrTime),
                createPrayerTimeObject("المغرب", "Maghrib", sunsetTime),
                createPrayerTimeObject("العشاء", "Isha", ishaTime)
            )
        }
        
        private fun createPrayerTimeObject(nameAr: String, nameEn: String, decimalHour: Double): PrayerTime {
            var hour = decimalHour.toInt()
            var min = ((decimalHour - hour) * 60).roundToInt()
            
            if (min >= 60) {
                hour += 1
                min -= 60
            }
            if (hour >= 24) {
                hour -= 24
            }
            if (hour < 0) {
                hour += 24
            }
            
            // Convert to 12 hour AM/PM standard format in Arabic
            val period = if (hour >= 12) "م" else "ص"
            val displayHour = if (hour % 12 == 0) 12 else hour % 12
            val formattedTime = String.format(Locale("ar"), "%d:%02d %s", displayHour, min, period)
            
            return PrayerTime(
                nameAr = nameAr,
                nameEn = nameEn,
                timeString = formattedTime,
                dateString = String.format("%02d:%02d", hour, min),
                iconName = nameEn.lowercase()
            )
        }
        
        // Find current active and next active prayer
        fun getCurrentAndNextPrayer(times: List<PrayerTime>): Pair<PrayerTime?, PrayerTime?> {
            val nowCalendar = Calendar.getInstance()
            val currentHour = nowCalendar.get(Calendar.HOUR_OF_DAY)
            val currentMin = nowCalendar.get(Calendar.MINUTE)
            val currentMinutesSinceMidnight = currentHour * 60 + currentMin
            
            // Parse times to minutes
            val timesInMinutes = times.map { prayer ->
                val parts = prayer.dateString.split(":")
                val h = parts[0].toIntOrNull() ?: 12
                val m = parts[1].toIntOrNull() ?: 0
                Pair(prayer, h * 60 + m)
            }
            
            // Find the active (last passed) prayer
            var activePrayer: Pair<PrayerTime, Int>? = null
            var nextPrayer: Pair<PrayerTime, Int>? = null
            
            for (i in timesInMinutes.indices) {
                val current = timesInMinutes[i]
                val next = timesInMinutes[(i + 1) % timesInMinutes.size]
                
                if (currentMinutesSinceMidnight >= current.second && (i == timesInMinutes.size - 1 || currentMinutesSinceMidnight < next.second)) {
                    activePrayer = current
                    nextPrayer = next
                    break
                }
            }
            
            // Fallback for before Fajr (active is Isha of yesterday, next is Fajr)
            if (activePrayer == null && timesInMinutes.isNotEmpty()) {
                activePrayer = timesInMinutes.last() // Isha
                nextPrayer = timesInMinutes.first() // Fajr
            }
            
            // Map the Pair to return active with modifications, e.g. marking as active
            val modifiedActive = activePrayer?.first?.copy(isActive = true)
            
            return Pair(modifiedActive, nextPrayer?.first)
        }
    }
}
