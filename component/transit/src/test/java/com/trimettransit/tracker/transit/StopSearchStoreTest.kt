package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StopSearchStoreTest {

    private val sampleStops = listOf(
        Stop(
            desc = "Max SB & 1st Ave",
            dirDesc = "Southbound",
            latitude = 45.523062,
            longitude = -122.679252,
            transitType = "M",
            routeNum = 1,
            locId = 7783,
            routes = listOf(
                Route(desc = "MAX Blue Line", routeId = 1, isMax = true),
                Route(desc = "Portland Streetcar", routeId = 300, isStreetcar = true)
            )
        ),
        Stop(
            desc = "Everett & 5th",
            dirDesc = "Eastbound",
            latitude = 45.5251,
            longitude = -122.6766,
            transitType = "B",
            routeNum = 20,
            locId = 8233,
            routes = listOf(Route(desc = "20-Burnside/Stark", routeId = 20, isBus = true))
        )
    )

    @Test
    fun `serialize and deserialize round-trip stops with their routes`() {
        val restored = deserializeStops(serializeStops(sampleStops))
        assertEquals(sampleStops, restored)
    }

    @Test
    fun `an empty list round-trips as an empty list`() {
        val restored = deserializeStops(serializeStops(emptyList()))
        assertEquals(emptyList<Stop>(), restored)
    }

    @Test
    fun `malformed json deserializes to null`() {
        assertNull(deserializeStops("this is not json"))
        assertNull(deserializeStops("{\"wrongShape\": true}"))
        assertNull(deserializeStops("[null]"))
        assertNull(deserializeStops("[{\"routes\": [true]}]"))
    }

    @Test
    fun `order and duplicate stops are preserved`() {
        val stops = listOf(sampleStops[0], sampleStops[1], sampleStops[0])
        assertEquals(stops, deserializeStops(serializeStops(stops)))
    }
}