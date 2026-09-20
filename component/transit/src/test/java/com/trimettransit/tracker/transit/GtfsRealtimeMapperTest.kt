package com.trimettransit.tracker.transit

import com.google.transit.realtime.GtfsRealtime
import org.junit.Assert.assertEquals
import org.junit.Test

class GtfsRealtimeMapperTest {
    @Test
    fun `maps vehicle positions onto the existing vehicle model`() {
        val feed = GtfsRealtime.FeedMessage.newBuilder()
            .setHeader(
                GtfsRealtime.FeedHeader.newBuilder()
                    .setGtfsRealtimeVersion("2.0")
                    .setIncrementality(GtfsRealtime.FeedHeader.Incrementality.FULL_DATASET)
                    .setTimestamp(1_700_000_000L)
            )
            .addEntity(
                GtfsRealtime.FeedEntity.newBuilder()
                    .setId("vehicle-1")
                    .setVehicle(
                        GtfsRealtime.VehiclePosition.newBuilder()
                            .setTrip(GtfsRealtime.TripDescriptor.newBuilder().setRouteId("4").setTripId("trip-4"))
                            .setVehicle(GtfsRealtime.VehicleDescriptor.newBuilder().setId("3518"))
                            .setPosition(
                                GtfsRealtime.Position.newBuilder()
                                    .setLatitude(45.52f)
                                    .setLongitude(-122.67f)
                                    .setBearing(90f)
                            )
                            .setTimestamp(1_700_000_000L)
                    )
            )
            .build()

        val vehicle = GtfsRealtimeMapper.vehicles(feed).single()
        assertEquals(3518, vehicle.vehicleID)
        assertEquals(4, vehicle.routeNumber)
        assertEquals("trip-4", vehicle.tripID)
        assertEquals(1_700_000_000_000L, vehicle.time)
    }
}
