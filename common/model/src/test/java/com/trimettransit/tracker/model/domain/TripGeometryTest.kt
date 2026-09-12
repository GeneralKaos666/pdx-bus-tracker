package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Stop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripGeometryTest {

    private fun direction(dir: Int, desc: String) = Direction(dir = dir, desc = desc)

    private fun stop(locId: Int, lat: Double, lng: Double) = Stop(
        desc = "Stop $locId",
        latitude = lat,
        longitude = lng,
        locId = locId
    )

    @Test
    fun `matchDirection matches a compass prefix in the route config direction`() {
        val directions = listOf(
            direction(0, "Northbound to Tualatin Park & Ride"),
            direction(1, "Southbound to City Center")
        )
        assertEquals(1, matchDirection(directions, "Southbound")?.dir)
        assertEquals(0, matchDirection(directions, "Northbound")?.dir)
    }

    @Test
    fun `matchDirection falls back to the first direction for unknown labels`() {
        val directions = listOf(direction(0, "Clockwise"), direction(1, "Counterclockwise"))
        assertEquals(0, matchDirection(directions, "Loop")?.dir)
        assertEquals(directions.first(), matchDirection(directions, ""))
    }

    @Test
    fun `matchDirection returns null for an empty list`() {
        assertNull(matchDirection(emptyList(), "Southbound"))
    }

    @Test
    fun `sliceStopsForLeg returns the span between the nearest stops`() {
        // Stops run north along a line of longitude -122.68.
        val stops = (1..5).map { i -> stop(i, 45.500 + (i - 1) * 0.010, -122.680) }
        val slice = sliceStopsForLeg(
            stops,
            fromLat = 45.506, fromLng = -122.681, // near stop 2
            toLat = 45.528, toLng = -122.679    // near stop 4
        )
        assertEquals(listOf(2, 3, 4), slice.map { it.locId })
    }

    @Test
    fun `sliceStopsForLeg returns a reversed span in sequence order`() {
        val stops = (1..4).map { i -> stop(i, 45.500 + (i - 1) * 0.010, -122.680) }
        val slice = sliceStopsForLeg(
            stops,
            fromLat = 45.528, fromLng = -122.679, // near stop 4
            toLat = 45.506, toLng = -122.681    // near stop 2
        )
        // Sequence order preserved (2,3,4) regardless of leg direction.
        assertEquals(listOf(2, 3, 4), slice.map { it.locId })
    }

    @Test
    fun `sliceStopsForLeg returns the whole sequence for tiny or single-stop spans`() {
        val single = listOf(stop(1, 45.5, -122.68))
        assertEquals(single, sliceStopsForLeg(single, 45.5, -122.68, 45.5, -122.68))

        val meets = listOf(stop(1, 45.5, -122.68), stop(2, 45.51, -122.68))
        val bothEndpoints = sliceStopsForLeg(meets, 45.501, -122.68, 45.509, -122.68)
        assertTrue(bothEndpoints.size >= 2)
    }
}