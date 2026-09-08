package com.trimettransit.tracker.widget

import com.trimettransit.tracker.widget.WidgetThemeOption.DARK
import com.trimettransit.tracker.widget.WidgetThemeOption.LIGHT
import com.trimettransit.tracker.widget.WidgetThemeOption.SYSTEM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetConfigTest {

    @Test
    fun `round trips the full config`() {
        val config = WidgetConfig(
            selectedStopIds = listOf("8384", "777", "14550"),
            arrivalsPerStop = 3,
            showClockTime = true,
            theme = DARK,
            compactRows = true,
            titleText = "My stops",
            hideTitle = true
        )
        assertEquals(config, WidgetConfig.fromPersistentMap(config.toPersistentMap()))
    }

    @Test
    fun `round trips selected stop ids`() {
        val config = WidgetConfig(selectedStopIds = listOf("8384", "777", "14550"))
        assertEquals(config, WidgetConfig.fromPersistentMap(config.toPersistentMap()))
    }

    @Test
    fun `round trips arrivals per stop`() {
        val config = WidgetConfig(arrivalsPerStop = 3)
        assertEquals(config, WidgetConfig.fromPersistentMap(config.toPersistentMap()))
    }

    @Test
    fun `round trips show clock time`() {
        assertTrue(WidgetConfig.fromPersistentMap(WidgetConfig(showClockTime = true).toPersistentMap()).showClockTime)
    }

    @Test
    fun `round trips light theme`() {
        assertEquals(LIGHT, WidgetConfig.fromPersistentMap(WidgetConfig(theme = LIGHT).toPersistentMap()).theme)
    }

    @Test
    fun `round trips dark theme`() {
        assertEquals(DARK, WidgetConfig.fromPersistentMap(WidgetConfig(theme = DARK).toPersistentMap()).theme)
    }

    @Test
    fun `round trips compact rows`() {
        assertTrue(WidgetConfig.fromPersistentMap(WidgetConfig(compactRows = true).toPersistentMap()).compactRows)
    }

    @Test
    fun `round trips title text`() {
        val config = WidgetConfig(titleText = "Home")
        assertEquals(config, WidgetConfig.fromPersistentMap(config.toPersistentMap()))
    }

    @Test
    fun `round trips hide title`() {
        assertTrue(WidgetConfig.fromPersistentMap(WidgetConfig(hideTitle = true).toPersistentMap()).hideTitle)
    }

    @Test
    fun `ordered stop ids survive a round trip`() {
        val ids = listOf("14550", "8384", "777")
        val restored = WidgetConfig.fromPersistentMap(WidgetConfig(selectedStopIds = ids).toPersistentMap())
        assertEquals(ids, restored.selectedStopIds)
    }

    @Test
    fun `empty map decodes to the no-arg defaults`() {
        assertEquals(WidgetConfig(), WidgetConfig.fromPersistentMap(emptyMap()))
    }

    @Test
    fun `absent title text decodes to null`() {
        assertEquals(null, WidgetConfig.fromPersistentMap(emptyMap()).titleText)
    }

    @Test
    fun `empty or absent stop ids decode to empty list`() {
        assertEquals(emptyList<String>(), WidgetConfig.fromPersistentMap(emptyMap()).selectedStopIds)
        assertEquals(
            emptyList<String>(),
            WidgetConfig.fromPersistentMap(mapOf(WidgetConfig.KEY_STOP_IDS to "")).selectedStopIds
        )
    }

    @Test
    fun `arrivals per stop clamps out-of-range values to the default`() {
        assertEquals(2, WidgetConfig.fromPersistentMap(mapOf(WidgetConfig.KEY_ARRIVALS_PER_STOP to "0")).arrivalsPerStop)
        assertEquals(2, WidgetConfig.fromPersistentMap(mapOf(WidgetConfig.KEY_ARRIVALS_PER_STOP to "9")).arrivalsPerStop)
        assertEquals(2, WidgetConfig.fromPersistentMap(mapOf(WidgetConfig.KEY_ARRIVALS_PER_STOP to "abc")).arrivalsPerStop)
    }

    @Test
    fun `arrivals per stop accepts the boundary values`() {
        assertEquals(1, WidgetConfig.fromPersistentMap(mapOf(WidgetConfig.KEY_ARRIVALS_PER_STOP to "1")).arrivalsPerStop)
        assertEquals(3, WidgetConfig.fromPersistentMap(mapOf(WidgetConfig.KEY_ARRIVALS_PER_STOP to "3")).arrivalsPerStop)
    }

    @Test
    fun `unknown theme falls back to system`() {
        val config = WidgetConfig.fromPersistentMap(mapOf(WidgetConfig.KEY_THEME to "sepia"))
        assertEquals(SYSTEM, config.theme)
    }

    @Test
    fun `non-boolean flag strings fall back to false`() {
        val map = mapOf(
            WidgetConfig.KEY_SHOW_CLOCK_TIME to "yes",
            WidgetConfig.KEY_COMPACT_ROWS to "1",
            WidgetConfig.KEY_HIDE_TITLE to "on"
        )
        val config = WidgetConfig.fromPersistentMap(map)
        assertFalse(config.showClockTime)
        assertFalse(config.compactRows)
        assertFalse(config.hideTitle)
    }

    @Test
    fun `persistent map uses the exact storage keys and values`() {
        val config = WidgetConfig(
            selectedStopIds = listOf("8384", "777"),
            arrivalsPerStop = 3,
            showClockTime = true,
            theme = LIGHT,
            compactRows = true,
            titleText = "TriMet NW",
            hideTitle = true
        )
        assertEquals(
            mapOf(
                "stop_ids" to "8384,777",
                "arrivals_per_stop" to "3",
                "show_clock_time" to "true",
                "theme" to "light",
                "compact_rows" to "true",
                "title_text" to "TriMet NW",
                "hide_title" to "true"
            ),
            config.toPersistentMap()
        )
    }

    @Test
    fun `null title text omits the key and empty stop ids store an empty string`() {
        assertEquals(
            mapOf(
                "stop_ids" to "",
                "arrivals_per_stop" to "2",
                "show_clock_time" to "false",
                "theme" to "system",
                "compact_rows" to "false",
                "hide_title" to "false"
            ),
            WidgetConfig().toPersistentMap()
        )
    }
}