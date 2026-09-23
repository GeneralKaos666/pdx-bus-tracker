package com.trimettransit.tracker.model.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BearingTest {

    @Test
    fun `a stop due north reads as north`() {
        val bearing = bearingDegrees(45.52, -122.67, 45.53, -122.67)

        assertEquals(0.0, bearing, 0.5)
        assertEquals(CompassDirection.N, compassDirection(bearing))
    }

    @Test
    fun `a stop due east reads as east`() {
        val bearing = bearingDegrees(45.52, -122.67, 45.52, -122.66)

        assertEquals(90.0, bearing, 0.5)
        assertEquals(CompassDirection.E, compassDirection(bearing))
    }

    @Test
    fun `a stop due south reads as south`() {
        val bearing = bearingDegrees(45.53, -122.67, 45.52, -122.67)

        assertEquals(CompassDirection.S, compassDirection(bearing))
    }

    @Test
    fun `a stop due west reads as west`() {
        val bearing = bearingDegrees(45.52, -122.66, 45.52, -122.67)

        assertEquals(CompassDirection.W, compassDirection(bearing))
    }

    @Test
    fun `north east and north west are told apart`() {
        assertEquals(
            CompassDirection.NE,
            compassDirection(bearingDegrees(45.52, -122.67, 45.53, -122.66))
        )
        assertEquals(
            CompassDirection.NW,
            compassDirection(bearingDegrees(45.52, -122.67, 45.53, -122.68))
        )
    }

    @Test
    fun `a bearing is always inside one turn of the circle`() {
        val west = bearingDegrees(45.52, -122.66, 45.52, -122.67)

        assertEquals(270.0, west, 0.5)
        assertEquals(CompassDirection.W, compassDirection(west))
    }

    @Test
    fun `sector boundaries belong to the point they open`() {
        // Each point owns the half-sector either side of it, so there is no gap between sectors.
        assertEquals(CompassDirection.N, compassDirection(0.0))
        assertEquals(CompassDirection.N, compassDirection(22.4))
        assertEquals(CompassDirection.NE, compassDirection(22.5))
        assertEquals(CompassDirection.NE, compassDirection(67.4))
        assertEquals(CompassDirection.E, compassDirection(67.5))
        assertEquals(CompassDirection.N, compassDirection(337.5))
        assertEquals(CompassDirection.N, compassDirection(359.9))
    }

    @Test
    fun `a bearing outside one turn is wrapped rather than crashing`() {
        assertEquals(CompassDirection.N, compassDirection(360.0))
        assertEquals(CompassDirection.E, compassDirection(450.0))
        assertEquals(CompassDirection.NW, compassDirection(-45.0))
    }
}
