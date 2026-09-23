package com.trimettransit.tracker.transit

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class StopsByLocationDistanceTest {

    private fun stopJson(locId: Int, desc: String, distance: Double) = """
        { "locid": $locId, "desc": "$desc", "dir": "N", "lat": 45.52, "lng": -122.67,
          "distance": $distance,
          "route": [ { "route": 4, "desc": "D", "type": "B" } ] }
    """.trimIndent()

    /** The same stop as the API sends it when it is not answering a location query: no "distance". */
    private fun stopJsonWithoutDistance(locId: Int, desc: String) = """
        { "locid": $locId, "desc": "$desc", "dir": "N", "lat": 45.52, "lng": -122.67,
          "route": [ { "route": 4, "desc": "D", "type": "B" } ] }
    """.trimIndent()

    private fun parse(vararg stops: String) =
        TransitJsonMapper.parseStopsByLocation(
            JSONObject("""{ "location": [ ${stops.joinToString(",")} ] }""")
        )

    @Test
    fun `distance is read from the response`() {
        val stops = parse(stopJson(1, "A", 328.5))

        assertEquals(1, stops.size)
        assertEquals(328.5, stops[0].distanceFeet, 0.001)
    }

    @Test
    fun `a stop the API sent no distance for has an unknown distance`() {
        val stops = parse(stopJsonWithoutDistance(1, "A"))

        assertEquals(0.0, stops[0].distanceFeet, 0.0)
    }

    @Test
    fun `stops come back nearest first regardless of the order they arrive in`() {
        val stops = parse(
            stopJson(1, "far", 400.0),
            stopJson(2, "near", 50.0),
            stopJson(3, "mid", 200.0)
        )

        assertEquals(listOf(2, 3, 1), stops.map { it.locId })
    }

    @Test
    fun `stops with no distance sort after every stop that has one`() {
        val stops = parse(stopJsonWithoutDistance(1, "unknown"), stopJson(2, "near", 50.0))

        assertEquals(listOf(2, 1), stops.map { it.locId })
    }

    @Test
    fun `stops sharing a distance keep the order the API sent them in`() {
        val stops = parse(
            stopJson(1, "a", 100.0),
            stopJson(2, "b", 100.0),
            stopJson(3, "c", 100.0)
        )

        assertEquals(listOf(1, 2, 3), stops.map { it.locId })
    }

    @Test
    fun `the combined stops-and-arrivals parse inherits the distance and the ordering`() {
        val result = TransitJsonMapper.parseStopsWithArrivals(
            JSONObject(
                """{ "location": [ ${stopJson(1, "far", 400.0)}, ${stopJson(2, "near", 50.0)} ] }"""
            )
        )

        assertEquals(listOf(2, 1), result.stops.map { it.locId })
        assertEquals(50.0, result.stops[0].distanceFeet, 0.001)
    }
}
