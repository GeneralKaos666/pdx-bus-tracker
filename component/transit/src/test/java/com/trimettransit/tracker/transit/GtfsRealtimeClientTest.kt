package com.trimettransit.tracker.transit

import java.io.IOException
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class GtfsRealtimeClientTest {
    @Test
    fun `rejects realtime payload over bound`() {
        try {
            checkGtfsRealtimeSize(MAX_GTFS_REALTIME_BYTES + 1)
            fail("expected IOException")
        } catch (e: IOException) {
            assertTrue(e.message!!.contains("over"))
        }
    }

    @Test
    fun `accepts realtime payload within bound`() {
        checkGtfsRealtimeSize(0)
        checkGtfsRealtimeSize(MAX_GTFS_REALTIME_BYTES)
    }

    @Test
    fun `rejects non-https feed url`() {
        try {
            requireHttpsFeedUrl("http://example.com/feed")
            fail("expected IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("HTTPS"))
        }
        requireHttpsFeedUrl("https://developer.trimet.org/ws/V1/VehiclePositions")
    }
}
