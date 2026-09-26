package com.trimettransit.tracker.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSizesTest {

    @Test
    fun `short height uses compact layout`() {
        assertEquals(WidgetLayout.COMPACT, layoutForSize(250, 100))
    }

    @Test
    fun `narrow width uses compact layout`() {
        assertEquals(WidgetLayout.COMPACT, layoutForSize(100, 300))
    }

    @Test
    fun `medium box uses list layout`() {
        assertEquals(WidgetLayout.LIST, layoutForSize(250, 200))
    }

    @Test
    fun `tall box uses tall layout`() {
        assertEquals(WidgetLayout.TALL, layoutForSize(250, 400))
    }

    @Test
    fun `preview fallback has sample rows with future arrivals`() {
        val now = System.currentTimeMillis()
        val snapshot = samplePreviewSnapshot(now)
        assertTrue(snapshot.rows.isNotEmpty())
        assertTrue(snapshot.rows.all { row -> row.arrivals.all { it.atMillis > now } })
    }
}
