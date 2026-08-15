package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.pref.SettingsRepository
import com.example.ui.theme.ThemeManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ThemeSettingsTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        repository = SettingsRepository(context)
    }

    @Test
    fun verifyDefaultThemeIsLight() {
        assertEquals("LIGHT", repository.themeMode.value)
    }

    @Test
    fun verifyThemeChangesAndPersists() {
        repository.setThemeMode("DARK")
        assertEquals("DARK", repository.themeMode.value)

        repository.setThemeMode("SYSTEM")
        assertEquals("SYSTEM", repository.themeMode.value)
    }

    @Test
    fun verifyThemeManagerResolution() {
        // Mode: LIGHT -> should never play dark theme
        assertFalse(ThemeManager.shouldPlayDarkTheme("LIGHT", isSystemInDark = false))
        assertFalse(ThemeManager.shouldPlayDarkTheme("LIGHT", isSystemInDark = true))

        // Mode: DARK -> should always play dark theme
        assertTrue(ThemeManager.shouldPlayDarkTheme("DARK", isSystemInDark = false))
        assertTrue(ThemeManager.shouldPlayDarkTheme("DARK", isSystemInDark = true))

        // Mode: SYSTEM -> should follow system dark theme setting
        assertFalse(ThemeManager.shouldPlayDarkTheme("SYSTEM", isSystemInDark = false))
        assertTrue(ThemeManager.shouldPlayDarkTheme("SYSTEM", isSystemInDark = true))
    }
}
