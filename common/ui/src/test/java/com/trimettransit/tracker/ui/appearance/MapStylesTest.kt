package com.trimettransit.tracker.ui.appearance

import org.junit.Assert.assertEquals
import org.junit.Test

class MapStylesTest {

    @Test
    fun `default streets preset resolves to liberty in light mode`() {
        assertEquals(
            "https://tiles.openfreemap.org/styles/liberty",
            MapStyles.styleUrlFor(MapStyles.DEFAULT, isDark = false)
        )
    }

    @Test
    fun `default streets preset resolves to the dark basemap in dark mode`() {
        assertEquals(
            "https://tiles.openfreemap.org/styles/dark",
            MapStyles.styleUrlFor(MapStyles.DEFAULT, isDark = true)
        )
    }

    @Test
    fun `bright and positron presets keep their own basemap in light mode`() {
        assertEquals(
            "https://tiles.openfreemap.org/styles/bright",
            MapStyles.styleUrlFor(MapStyles.BRIGHT, isDark = false)
        )
        assertEquals(
            "https://tiles.openfreemap.org/styles/positron",
            MapStyles.styleUrlFor(MapStyles.POSITRON, isDark = false)
        )
    }

    @Test
    fun `always dark preset stays dark even in light mode`() {
        assertEquals(
            "https://tiles.openfreemap.org/styles/dark",
            MapStyles.styleUrlFor(MapStyles.DARK, isDark = false)
        )
    }

    @Test
    fun `unknown preset falls back to the liberty basemap in light mode`() {
        assertEquals(
            "https://tiles.openfreemap.org/styles/liberty",
            MapStyles.styleUrlFor("bogus", isDark = false)
        )
    }
}