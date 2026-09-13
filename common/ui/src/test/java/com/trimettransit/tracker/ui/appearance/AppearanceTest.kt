package com.trimettransit.tracker.ui.appearance

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.dynamicColorScheme
import com.trimettransit.tracker.ui.theme.TrimetBlue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceTest {

    @Test
    fun `vibrancy factor maps presets`() {
        assertEquals(1.0f, Vibrancy.MUTED.factor)
        assertEquals(1.25f, Vibrancy.DEFAULT.factor)
        assertEquals(1.5f, Vibrancy.VIBRANT.factor)
    }

    @Test
    fun `saturated with factor 1 keeps color unchanged`() {
        val color = Color(0xFF0079C1)
        assertEquals(color, color.saturated(1.0f))
    }

    @Test
    fun `saturated boosts a grey - tinted color`() {
        val color = Color(0xFF9A9AC4)
        val boosted = color.saturated(1.5f)
        assertTrue("saturation should increase", boosted.saturation() > color.saturation())
    }

    @Test
    fun `saturated preserves alpha and hue family`() {
        val color = Color(0x802B3E6E)
        val boosted = color.saturated(2.0f)
        assertEquals(color.alpha, boosted.alpha, 0.0001f)
        assertTrue("blue stays the dominant channel", boosted.blue > boosted.green)
        assertTrue("green stays above red", boosted.green > boosted.red)
        assertTrue("red stays the recessive channel", boosted.red < boosted.blue)
    }

    @Test
    fun `achromatic color stays achromatic`() {
        val gray = Color(0xFF808080)
        assertEquals(gray, gray.saturated(5.0f))
    }

    @Test
    fun `fromPrefs maps and defaults all appearance tokens`() {
        val style = appearanceStyleFromPrefs(
            theme = "dark",
            colorMode = "seed",
            accentColor = "#FF6600",
            vibrancy = "vibrant",
            amoledDark = true,
            pillAccent = "#00FF00",
            density = "compact",
            fontScale = "larger",
            motion = "low"
        )
        assertEquals(ThemePreference.DARK, style.theme)
        assertEquals(ColorMode.SEED, style.colorMode)
        assertEquals(Color(0xFFFF6600), style.seedColor)
        assertEquals(Vibrancy.VIBRANT, style.vibrancy)
        assertTrue(style.amoledDark)
        assertEquals(Color(0xFF00FF00), style.pillAccent)
        assertEquals(Density.COMPACT, style.density)
        assertEquals(FontScale.LARGER, style.fontScale)
        assertEquals(MotionIntensity.LOW, style.motionIntensity)

        val defaults = appearanceStyleFromPrefs(
            theme = "system",
            colorMode = "dynamic",
            accentColor = "",
            vibrancy = "default",
            amoledDark = false,
            pillAccent = "",
            density = "comfortable",
            fontScale = "default",
            motion = "expressive"
        )
        assertEquals(ThemePreference.SYSTEM, defaults.theme)
        assertEquals(ColorMode.DYNAMIC, defaults.colorMode)
        assertEquals(TrimetBlue, defaults.seedColor)
        assertEquals(Vibrancy.DEFAULT, defaults.vibrancy)
        assertFalse(defaults.amoledDark)
        assertEquals(null, defaults.pillAccent)
        assertEquals(Density.COMFORTABLE, defaults.density)
        assertEquals(FontScale.DEFAULT, defaults.fontScale)
        assertEquals(MotionIntensity.EXPRESSIVE, defaults.motionIntensity)
    }

    @Test
    fun `seed scheme produces distinct light and dark palettes`() {
        val light = dynamicColorScheme(seedColor = TrimetBlue, isDark = false)
        val dark = dynamicColorScheme(seedColor = TrimetBlue, isDark = true)
        assertNotEquals(light.surface, dark.surface)
        assertNotEquals(light.primary, dark.primary)
    }

    @Test
    fun `amoled dark scheme renders near - black backgrounds`() {
        val dark = dynamicColorScheme(seedColor = TrimetBlue, isDark = true, isAmoled = true)
        assertEquals(Color.Black, dark.background)
        assertEquals(Color.Black, dark.surface)
    }

    @Test
    fun `vibrancy wrapper boosts scheme chroma while amoled zeroes surfaces`() {
        val base = dynamicColorScheme(seedColor = Color(0xFF4A90D9), isDark = true)
        val vibrant = base.withVibrancy(Vibrancy.VIBRANT)
        assertNotEquals(base.primary, vibrant.primary)
        val amoled = base.asAmoled()
        assertEquals(Color.Black, amoled.surface)
        assertNotEquals(Color.Black, base.surface)
    }

    @Test
    fun `muted vibrancy leaves the scheme untouched`() {
        val base = dynamicColorScheme(seedColor = Color(0xFF4A90D9), isDark = false)
        assertEquals(base, base.withVibrancy(Vibrancy.MUTED))
    }

    @Test
    fun `accent presets are unique and parse to colours`() {
        val presets = accentPresets
        assertTrue("at least six presets", presets.size >= 6)
        val seen = mutableSetOf<Int>()
        presets.forEach { preset ->
            val argb = preset.color.toArgb()
            assertTrue("no duplicate colors: $argb", seen.add(argb))
        }
    }

    @Test
    fun `onColorFor picks ink on light and white on dark`() {
        assertTrue("white background gets dark ink", onColorFor(Color.White).luminance() < 0.2f)
        assertTrue("near-black background gets white", onColorFor(Color.Black).luminance() > 0.9f)
    }
}