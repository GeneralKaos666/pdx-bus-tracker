package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentTripsTest {
    private fun point(lat: Double, lon: Double, desc: String = "Stop") =
        TripPoint(latitude = lat, longitude = lon, description = desc)

    @Test
    fun `round-trips points in order`() {
        val trips = listOf(point(45.51, -122.67, "A"), point(45.52, -122.68, "B"))
        assertEquals(trips, decodeRecentTrips(encodeRecentTrips(trips)))
    }

    @Test
    fun `descriptions cannot break the field layout`() {
        val trips = listOf(point(45.51, -122.67, "tab\there\nand newline"))
        val decoded = decodeRecentTrips(encodeRecentTrips(trips))
        assertEquals(1, decoded.size)
        assertEquals(45.51, decoded.first().latitude, 0.0)
        assertEquals(-122.67, decoded.first().longitude, 0.0)
        assertTrue(decoded.first().description.contains("tab here"))
    }

    @Test
    fun `garbage and invalid coordinates are dropped`() {
        val raw = "not a row\nA\t45.5\nB\t45.5\t-122.6\nC\t0\t0\nD\tabc\t-122.6"
        val decoded = decodeRecentTrips(raw)
        assertEquals(1, decoded.size)
        assertEquals("B", decoded.first().description)
    }

    @Test
    fun `null and blank input decode to empty`() {
        assertEquals(emptyList<TripPoint>(), decodeRecentTrips(null))
        assertEquals(emptyList<TripPoint>(), decodeRecentTrips("   "))
    }

    @Test
    fun `add puts the newest first and caps the list`() {
        var trips = emptyList<TripPoint>()
        for (i in 1..7) trips = addRecentTrip(trips, point(45.5 + i / 100.0, -122.6, "S$i"))
        assertEquals(MAX_RECENT_TRIPS, trips.size)
        assertEquals("S7", trips.first().description)
        assertEquals("S3", trips.last().description)
    }

    @Test
    fun `add moves an existing destination to the front without duplicating`() {
        val trips = listOf(point(45.51, -122.67, "A"), point(45.52, -122.68, "B"))
        val updated = addRecentTrip(trips, point(45.52, -122.68, "B again"))
        assertEquals(2, updated.size)
        assertEquals("B again", updated.first().description)
        assertEquals("A", updated.last().description)
    }

    @Test
    fun `invalid point is ignored`() {
        val trips = listOf(point(45.51, -122.67, "A"))
        assertEquals(trips, addRecentTrip(trips, TripPoint(0.0, 0.0, "nowhere")))
    }
}
