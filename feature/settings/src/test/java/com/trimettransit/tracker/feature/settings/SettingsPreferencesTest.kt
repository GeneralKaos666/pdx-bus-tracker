package com.trimettransit.tracker.feature.settings

import com.trimettransit.tracker.ui.appearance.AppearancePrefs
import com.trimettransit.tracker.ui.appearance.MapStyles
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsPreferencesTest {
    @Test
    fun invalidValuesFallBackToSupportedDefaults() {
        assertEquals(AppearancePrefs.DEFAULT_THEME, normalizeTheme("sepia"))
        assertEquals(AppearancePrefs.DEFAULT_COLOR_MODE, normalizeColorMode("invalid"))
        assertEquals(AppearancePrefs.DEFAULT_VIBRANCY, normalizeVibrancy(null))
        assertEquals(AppearancePrefs.DEFAULT_DENSITY, normalizeDensity("wide"))
        assertEquals(AppearancePrefs.DEFAULT_FONT_SCALE, normalizeFontScale("huge"))
        assertEquals(AppearancePrefs.DEFAULT_MOTION, normalizeMotion("none"))
        assertEquals(MapStyles.DEFAULT, normalizeMapStyle("satellite"))
    }

    @Test
    fun supportedValuesArePreserved() {
        assertEquals(AppearancePrefs.Values.THEME_DARK, normalizeTheme("dark"))
        assertEquals(AppearancePrefs.Values.COLOR_MODE_SEED, normalizeColorMode("seed"))
        assertEquals(AppearancePrefs.Values.VIBRANCY_VIBRANT, normalizeVibrancy("vibrant"))
        assertEquals(AppearancePrefs.Values.DENSITY_COMPACT, normalizeDensity("compact"))
        assertEquals(AppearancePrefs.Values.FONT_LARGER, normalizeFontScale("larger"))
        assertEquals(AppearancePrefs.Values.MOTION_LOW, normalizeMotion("low"))
        assertEquals(MapStyles.POSITRON, normalizeMapStyle(MapStyles.POSITRON))
    }
}
