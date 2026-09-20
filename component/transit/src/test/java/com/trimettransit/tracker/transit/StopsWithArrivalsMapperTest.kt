package com.trimettransit.tracker.transit

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StopsWithArrivalsMapperTest {

    @Test
    fun `parseStopsWithArrivals reads stops and groups embedded per-location arrivals`() {
        val json = JSONObject(
            """
            {
              "location": [
                {
                  "locid": 1, "desc": "A", "dir": "N", "lat": 45.52, "lng": -122.67,
                  "route": [ { "route": 4, "desc": "D", "type": "B" } ],
                  "arrival": [
                    { "tripID": "t1", "route": 4, "scheduled": 1700000060000 },
                    { "tripID": "t2", "route": 4, "scheduled": 1700000120000 }
                  ]
                },
                {
                  "locid": 2, "desc": "B", "dir": "S", "lat": 45.53, "lng": -122.68,
                  "route": [ { "route": 9, "desc": "E", "type": "B" } ]
                }
              ]
            }
            """.trimIndent()
        )

        val result = TransitJsonMapper.parseStopsWithArrivals(json)

        assertEquals(2, result.stops.size)
        assertEquals(listOf(1, 2), result.stops.map { it.locId })

        assertEquals(2, result.arrivalsByStop[1]?.size)
        assertEquals("t1", result.arrivalsByStop[1]?.get(0)?.tripID)
        // locid was implicit on the embedded arrival element; must be backfilled from the parent stop.
        assertEquals(1, result.arrivalsByStop[1]?.get(0)?.locId)

        // A stop with no embedded "arrival" array has no entry at all (not an empty list).
        assertTrue(result.arrivalsByStop[2] == null)
    }

    @Test
    fun `parseStopsWithArrivals reads a flat root-level locid-tagged arrival array`() {
        val json = JSONObject(
            """
            {
              "location": [
                { "locid": 1, "desc": "A", "dir": "N", "lat": 45.52, "lng": -122.67 },
                { "locid": 2, "desc": "B", "dir": "S", "lat": 45.53, "lng": -122.68 }
              ],
              "arrival": [
                { "tripID": "t1", "locid": 1, "route": 4, "scheduled": 1700000060000 },
                { "tripID": "t2", "locid": 2, "route": 9, "scheduled": 1700000120000 }
              ]
            }
            """.trimIndent()
        )

        val result = TransitJsonMapper.parseStopsWithArrivals(json)

        assertEquals(1, result.arrivalsByStop[1]?.size)
        assertEquals(1, result.arrivalsByStop[2]?.size)
        assertEquals("t1", result.arrivalsByStop[1]?.single()?.tripID)
        assertEquals("t2", result.arrivalsByStop[2]?.single()?.tripID)
    }

    @Test
    fun `parseStopsWithArrivals dedupes identical arrivals per stop`() {
        val json = JSONObject(
            """
            {
              "location": [
                {
                  "locid": 1, "desc": "A", "dir": "N", "lat": 45.52, "lng": -122.67,
                  "arrival": [
                    { "tripID": "t1", "route": 4, "scheduled": 1700000060000, "blockID": 1, "vehicleID": 2 },
                    { "tripID": "t1", "route": 4, "scheduled": 1700000060000, "blockID": 1, "vehicleID": 2 }
                  ]
                }
              ]
            }
            """.trimIndent()
        )

        val result = TransitJsonMapper.parseStopsWithArrivals(json)
        assertEquals(1, result.arrivalsByStop[1]?.size)
    }

    @Test
    fun `parseStopsWithArrivals ignores an arrival with a zero-valued root locid`() {
        val json = JSONObject().apply {
            put("location", JSONArray())
            put(
                "arrival",
                JSONArray().put(JSONObject("""{ "tripID": "orphan", "route": 4 }"""))
            )
        }

        val result = TransitJsonMapper.parseStopsWithArrivals(json)
        assertTrue(result.arrivalsByStop.isEmpty())
    }

    @Test
    fun `parseStopsWithArrivals treats a payload with no locations or arrivals as empty`() {
        val result = TransitJsonMapper.parseStopsWithArrivals(JSONObject("{}"))
        assertTrue(result.stops.isEmpty())
        assertTrue(result.arrivalsByStop.isEmpty())
    }
}
