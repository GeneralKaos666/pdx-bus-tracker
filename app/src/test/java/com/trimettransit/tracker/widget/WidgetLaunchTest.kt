package com.trimettransit.tracker.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WidgetLaunchTest {

    @Test
    fun `valid extras build the stop`() {
        val stop = stopFromLaunchExtras(
            stopId = 8384L,
            name = "SW 5th & Stark",
            routeId = 12,
            lat = 45.52,
            lng = -122.68
        )!!
        assertEquals(8384, stop.locId)
        assertEquals("SW 5th & Stark", stop.desc)
        assertEquals(12, stop.routeNum)
        assertEquals(45.52, stop.latitude, 0.0)
        assertEquals(-122.68, stop.longitude, 0.0)
    }

    @Test
    fun `zero stop id yields null`() {
        assertNull(stopFromLaunchExtras(0L, "X", 1, 0.0, 0.0))
    }

    @Test
    fun `negative stop id yields null`() {
        assertNull(stopFromLaunchExtras(-1L, "X", 1, 0.0, 0.0))
    }

    @Test
    fun `stop id above Int range yields null`() {
        assertNull(stopFromLaunchExtras(Int.MAX_VALUE.toLong() + 1, "X", 1, 0.0, 0.0))
    }

    @Test
    fun `missing name falls back to empty`() {
        val stop = stopFromLaunchExtras(8384L, null, 0, 0.0, 0.0)!!
        assertEquals("", stop.desc)
        assertEquals(0, stop.routeNum)
    }
}
