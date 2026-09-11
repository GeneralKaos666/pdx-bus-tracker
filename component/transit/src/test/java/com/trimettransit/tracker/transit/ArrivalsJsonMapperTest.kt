package com.trimettransit.tracker.transit

import org.joda.time.DateTime
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalsJsonMapperTest {

    @Test
    fun `parseArrivals maps a full payload`() {
        val json = """
            {
              "arrival": [
                {
                  "fullSign": "E-Side Express",
                  "shortSign": "4",
                  "estimated": 1700000000000,
                  "scheduled": 1700000060000,
                  "route": 4,
                  "status": "estimated",
                  "dropOffOnly": false,
                  "reason": "",
                  "tripID": "11269782",
                  "blockID": 3401,
                  "vehicleID": 3518,
                  "feet": 1200,
                  "dir": 1,
                  "blockPosition": {
                    "id": 3401,
                    "at": 1700000000000,
                    "vehicleID": 3518,
                    "feet": 1200,
                    "heading": 90.5,
                    "lat": 45.52,
                    "lng": -122.67,
                    "routeNumber": 4,
                    "direction": 1,
                    "tripID": "11269782",
                    "newTrip": true
                  }
                },
                {
                  "fullSign": "Second bus",
                  "shortSign": "4",
                  "tripID": "11270000",
                  "blockID": 3402
                }
              ],
              "detour": [
                {
                  "id": 1,
                  "desc": "Detour on Route 4",
                  "route": [ { "route": 4 }, { "route": 9 } ]
                }
              ],
              "location": [ { "lat": 45.52, "lng": -122.67 } ]
            }
        """.trimIndent()

        val result = TransitJsonMapper.parseArrivals(JSONObject(json))

        assertEquals(2, result.arrivals.size)
        val first = result.arrivals[0]
        assertEquals("E-Side Express", first.fullSign)
        assertEquals("4", first.shortSign)
        assertEquals(1_700_000_000_000L, first.estimatedMillis)
        assertEquals(DateTime(1_700_000_000_000L), first.estimated)
        assertEquals(DateTime(1_700_000_060_000L), first.scheduled)
        assertEquals(4, first.routeId)
        assertEquals("estimated", first.status)
        assertFalse(first.dropOffOnly)
        assertEquals("11269782", first.tripID)
        assertEquals(3401, first.blockID)
        assertEquals(3518, first.vehicleID)
        assertEquals(1200, first.feet)
        assertEquals(1, first.dir)

        assertEquals(1, result.blockPositions.size)
        val bp = result.blockPositions[0]
        assertEquals(3401, bp.id)
        assertEquals(3518, bp.vehicleID)
        assertEquals(1200, bp.feet)
        assertEquals(90.5f, bp.bearing)
        assertEquals(45.52, bp.lat, 0.0)
        assertEquals(-122.67, bp.lng, 0.0)
        assertEquals(4, bp.routeNumber)
        assertEquals("11269782", bp.tripID)
        assertTrue(bp.isNewTrip)

        assertEquals(1, result.detours.size)
        val detour = result.detours[0]
        assertEquals(1, detour.id)
        assertEquals("Detour on Route 4", detour.desc)
        assertEquals(listOf(4, 9), detour.routes)

        assertEquals(45.52, result.stopLat, 0.0)
        assertEquals(-122.67, result.stopLng, 0.0)

        val second = result.arrivals[1]
        assertNull(second.estimated)
        assertNull(second.scheduled)
        assertEquals(0L, second.estimatedMillis)
        assertEquals(0, second.routeId)
        assertEquals("Second bus", second.fullSign)
    }

    @Test
    fun `parseArrivals hides an arrival with no live estimate`() {
        val json = """
            {
              "arrival": [
                { "tripID": "11269782", "scheduled": 1700000060000, "status": "scheduled" }
              ]
            }
        """.trimIndent()

        val result = TransitJsonMapper.parseArrivals(JSONObject(json))
        assertEquals(1, result.arrivals.size)
        assertNull(result.arrivals[0].estimated)
        assertEquals(0L, result.arrivals[0].estimatedMillis)
        assertEquals(DateTime(1_700_000_060_000L), result.arrivals[0].scheduled)
        assertEquals(1_700_000_060_000L, result.arrivals[0].scheduledMillis)
    }

    @Test
    fun `parseArrivals prefers route before routes fallback for detours`() {
        val json = """
            {
              "detour": [
                { "id": 2, "desc": "Fallback", "routes": [4, 9] },
                { "id": 3, "desc": "No routes" }
              ]
            }
        """.trimIndent()

        val result = TransitJsonMapper.parseArrivals(JSONObject(json))
        assertEquals(listOf(4, 9), result.detours[0].routes)
        assertTrue(result.detours[1].routes.isNullOrEmpty())
    }

    @Test
    fun `parseArrivals treats missing sections as empty`() {
        val result = TransitJsonMapper.parseArrivals(JSONObject("{}"))
        assertTrue(result.arrivals.isEmpty())
        assertTrue(result.blockPositions.isEmpty())
        assertTrue(result.detours.isEmpty())
        assertEquals(0.0, result.stopLat, 0.0)
        assertEquals(0.0, result.stopLng, 0.0)
    }
}