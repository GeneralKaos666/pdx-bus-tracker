package com.trimettransit.tracker.transit

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VehiclesJsonMapperTest {

    @Test
    fun `parseVehicles maps a full vehicle`() {
        val json = """
            {
              "vehicle": [
                {
                  "vehicleID": 3518,
                  "type": "bus",
                  "blockID": 3401,
                  "latitude": 45.52,
                  "longitude": -122.67,
                  "bearing": 90.5,
                  "routeNumber": 4,
                  "direction": 1,
                  "tripID": "11269782",
                  "newTrip": true,
                  "delay": -2,
                  "signMessage": "E-Side Express",
                  "signMessageLong": "E-Side Express to Downtown",
                  "nextLocID": 1234,
                  "nextStopSeq": 3,
                  "lastLocID": 1220,
                  "lastStopSeq": 2,
                  "serviceDate": 1700000000000,
                  "locationInScheduleDay": 12345,
                  "time": 1700000010000,
                  "expires": 1700000040000,
                  "inCongestion": true,
                  "loadPercentage": 75,
                  "garage": "Center",
                  "extrablockID": "X34",
                  "offRoute": false
                },
                {
                  "vehicleID": 12
                }
              ]
            }
        """.trimIndent()

        val vehicles = TransitJsonMapper.parseVehicles(JSONObject(json))
        assertEquals(2, vehicles.size)

        val first = vehicles[0]
        assertEquals(3518, first.vehicleID)
        assertEquals("bus", first.type)
        assertEquals(3401, first.blockID)
        assertEquals(45.52, first.latitude, 0.0)
        assertEquals(-122.67, first.longitude, 0.0)
        assertEquals(90.5f, first.bearing)
        assertEquals(4, first.routeNumber)
        assertEquals(1, first.direction)
        assertEquals("11269782", first.tripID)
        assertTrue(first.isNewTrip)
        assertEquals(-2, first.delay)
        assertEquals("E-Side Express", first.signMessage)
        assertEquals("E-Side Express to Downtown", first.signMessageLong)
        assertEquals(1234, first.nextLocID)
        assertEquals(3, first.nextStopSeq)
        assertEquals(1220, first.lastLocID)
        assertEquals(2, first.lastStopSeq)
        assertEquals(1_700_000_000_000L, first.serviceDate)
        assertEquals(12345, first.locationInScheduleDay)
        assertEquals(1_700_000_010_000L, first.time)
        assertEquals(1_700_000_040_000L, first.expires)
        assertTrue(first.isInCongestion)
        assertEquals(75, first.loadPercentage)
        assertEquals("Center", first.garage)
        assertEquals("X34", first.extraBlockID)

        val minimal = vehicles[1]
        assertEquals(12, minimal.vehicleID)
        assertEquals(0, minimal.routeNumber)
        assertEquals(0f, minimal.bearing)
        assertEquals("", minimal.tripID)
    }

    @Test
    fun `parseVehicles returns empty when no vehicles present`() {
        assertTrue(TransitJsonMapper.parseVehicles(JSONObject("{}")).isEmpty())
    }
}