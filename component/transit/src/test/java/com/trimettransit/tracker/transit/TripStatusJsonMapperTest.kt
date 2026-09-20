package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.TripStopStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripStatusJsonMapperTest {
    @Test
    fun `parseTripStatus maps trip stops and falls back to aimed times`() {
        val result = TransitJsonMapper.parseTripStatus(
            JSONObject(
                """
                {
                  "queryTime": 1700000005000,
                  "location": [{"locid": 100, "desc": "Main & 1st", "dir": "North"}],
                  "trip": [{
                    "tripID": "T1", "blockID": 42, "routeNumber": 4,
                    "distance": 10000, "progressFeet": 2500, "extraTrip": true,
                    "modified": true, "destination": "Downtown",
                    "tripBeginTime": 1700000000000, "tripEndTime": 1700003600000,
                    "stop": [
                      {
                        "locid": 100, "stopSequence": 1, "isPassed": false,
                        "passengerAccessible": true, "distance": 1000,
                        "aimedArrival": 1700000060000, "aimedDeparture": 1700000120000,
                        "arrivalDelay": 90, "departureDelay": 120,
                        "estimated": true, "adjustedArrival": 1700000150000,
                        "status": "dropOffOnly"
                      },
                      {
                        "locid": 101, "isPassed": true,
                        "aimedArrival": 1700000180000, "aimedDeparture": 1700000240000,
                        "status": "canceled"
                      }
                    ]
                  }]
                }
                """.trimIndent()
            )
        )

        assertEquals(1_700_000_005_000L, result.queryTimeMillis)
        val trip = result.trips.single()
        assertEquals("T1", trip.tripId)
        assertTrue(trip.extra)
        assertTrue(trip.modified)
        assertEquals(0.25f, trip.progress)
        assertEquals(2, trip.stops.size)

        val first = trip.stops[0]
        assertEquals("Main & 1st", first.description)
        assertEquals(TripStopStatus.DROP_OFF_ONLY, first.status)
        assertEquals(1_700_000_150_000L, first.effectiveArrivalMillis)
        assertEquals(1_700_000_120_000L, first.effectiveDepartureMillis)
        assertEquals(90, first.arrivalDelaySeconds)

        val second = trip.stops[1]
        assertTrue(second.isPassed)
        assertTrue(second.isCanceled)
        assertEquals(second.aimedArrivalMillis, second.effectiveArrivalMillis)
        assertNull(second.adjustedArrivalMillis)
    }

    @Test
    fun `parseTripStatus tolerates missing sections`() {
        val result = TransitJsonMapper.parseTripStatus(JSONObject("{}"))
        assertTrue(result.trips.isEmpty())
        assertEquals(0L, result.queryTimeMillis)
    }
}

class BlockStatusJsonMapperTest {
    @Test
    fun `parseBlockStatus maps nullable vehicle state and nested trips`() {
        val result = TransitJsonMapper.parseBlockStatus(
            JSONObject(
                """
                {
                  "queryTime": 1700000005000,
                  "blockStatus": [{
                    "blockID": 42, "currentTripID": "T1", "lat": 45.5,
                    "lng": -122.6, "bearing": 180, "positionTimestamp": 1700000000000,
                    "deviation": 75, "vehicleID": 123, "schedulePosition": 1700000100000,
                    "trip": {"tripID": "T1", "blockID": 42, "routeNumber": 4}
                  }]
                }
                """.trimIndent()
            )
        )
        val block = result.blocks.single()
        assertEquals(42, block.blockId)
        assertEquals("T1", block.currentTripId)
        assertEquals(75, block.deviationSeconds)
        assertEquals(123, block.vehicleId)
        assertEquals(1, block.trips.size)
        assertEquals("T1", block.trips.single().tripId)
    }

    @Test
    fun `parseBlockStatus preserves absent position as null`() {
        val block = TransitJsonMapper.parseBlockStatus(JSONObject("""{"blockStatus":[{"blockID":1}]}"""))
            .blocks.single()
        assertNull(block.latitude)
        assertNull(block.positionTimestampMillis)
        assertFalse(block.trips.isNotEmpty())
    }
}
