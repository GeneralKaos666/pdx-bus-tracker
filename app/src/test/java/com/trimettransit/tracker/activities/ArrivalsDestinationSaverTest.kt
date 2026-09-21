package com.trimettransit.tracker.activities

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Test

class ArrivalsDestinationSaverTest {

    private val scope = SaverScope { true }

    private fun roundTrip(dest: ArrivalsDestination): ArrivalsDestination? {
        val saved = with(arrivalsDestinationSaver) { scope.save(dest) }
        @Suppress("UNCHECKED_CAST")
        return arrivalsDestinationSaver.restore(saved as List<Any>)
    }

    @Test
    fun `saver round-trips a fully populated destination`() {
        val dest = ArrivalsDestination(
            stopId = 777,
            stopName = "SW 6th & Pine",
            routeId = 9,
            lat = 45.5,
            lng = -122.6
        )
        assertEquals(dest, roundTrip(dest))
    }

    @Test
    fun `saver round-trips default values`() {
        val dest = ArrivalsDestination(stopId = 1)
        assertEquals(dest, roundTrip(dest))
    }
}
