package com.trimettransit.tracker.transit

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VehiclesJsonMapperTest {

    @Test
    fun `parseVehicles maps a full payload`() {
        val json = JSONObject(
            """
            {
              "queryTime": 1700000005000,
              "vehicle": [
                {
                  "vehicleID": 3518,
                  "type": "bus",
                  "blockID": 3401,
                  "tripID": "11269782",
                  "routeNumber": 4,
                  "direction": 1,
                  "newTrip": true,
                  "latitude": 45.52,
                  "longitude": -122.67,
                  "bearing": 90.5,
                  "time": 1700000000000,
                  "expires": 1700000060000,
                  "serviceDate": 1699999200000,
                  "locationInScheduleDay": 36000,
                  "delay": -30,
                  "signMessage": "4 Division",
                  "nextLocID": 100,
                  "lastLocID": 99,
                  "garage": "MERLO",
                  "inCongestion": true,
                  "loadPercentage": 70
                }
              ]
            }
            """.trimIndent()
        )

        val result = TransitJsonMapper.parseVehicles(json)

        assertEquals(1_700_000_005_000L, result.queryTime)
        assertEquals(1, result.vehicles.size)
        val v = result.vehicles[0]
        assertEquals(3518, v.vehicleID)
        assertEquals("bus", v.type)
        assertEquals(3401, v.blockID)
        assertEquals("11269782", v.tripID)
        assertEquals(4, v.routeNumber)
        assertEquals(1, v.direction)
        assertTrue(v.newTrip)
        assertEquals(45.52, v.latitude, 0.0)
        assertEquals(-122.67, v.longitude, 0.0)
        assertEquals(90.5f, v.bearing)
        assertEquals(1_700_000_000_000L, v.time)
        assertEquals(1_700_000_060_000L, v.expires)
        assertEquals(1_699_999_200_000L, v.serviceDate)
        assertEquals(36000, v.locationInScheduleDay)
        assertEquals(-30, v.delay)
        assertEquals("4 Division", v.signMessage)
        assertEquals(100, v.nextLocID)
        assertEquals(99, v.lastLocID)
        assertEquals("MERLO", v.garage)
        assertTrue(v.inCongestion)
        assertEquals(70, v.loadPercentage)
    }

    @Test
    fun `parseVehicles returns non-null empty result for an empty fleet`() {
        val result = TransitJsonMapper.parseVehicles(JSONObject("""{"queryTime": 1700000000000, "vehicle": []}"""))
        assertTrue(result.vehicles.isEmpty())
        assertEquals(1_700_000_000_000L, result.queryTime)
    }

    @Test
    fun `parseVehicles treats a missing vehicle array as empty, not an error`() {
        val result = TransitJsonMapper.parseVehicles(JSONObject("""{"queryTime": 1700000000000}"""))
        assertTrue(result.vehicles.isEmpty())
        assertEquals(1_700_000_000_000L, result.queryTime)
    }

    @Test
    fun `parseVehicles skips a malformed element without voiding the fleet`() {
        val json = JSONObject()
        val arr = JSONArray()
        arr.put(JSONObject("""{"vehicleID": 1, "routeNumber": 4}"""))
        arr.put("not-an-object")
        arr.put(JSONObject("""{"vehicleID": 2, "routeNumber": 9}"""))
        json.put("vehicle", arr)

        val result = TransitJsonMapper.parseVehicles(json)
        assertEquals(2, result.vehicles.size)
        assertEquals(1, result.vehicles[0].vehicleID)
        assertEquals(2, result.vehicles[1].vehicleID)
    }

    @Test
    fun `parseVehicles defaults loadPercentage to unavailable sentinel`() {
        val result = TransitJsonMapper.parseVehicles(
            JSONObject("""{"vehicle": [{"vehicleID": 1}]}""")
        )
        assertEquals(-1, result.vehicles[0].loadPercentage)
        assertFalse(result.vehicles[0].inCongestion)
    }
}
