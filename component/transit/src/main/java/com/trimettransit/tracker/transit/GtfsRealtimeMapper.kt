package com.trimettransit.tracker.transit

import com.google.transit.realtime.GtfsRealtime
import com.trimettransit.tracker.model.Vehicle

object GtfsRealtimeMapper {
    fun vehicles(feed: GtfsRealtime.FeedMessage): List<Vehicle> =
        feed.entityList.mapNotNull { entity ->
            if (!entity.hasVehicle()) return@mapNotNull null
            val position = entity.vehicle
            if (!position.hasPosition()) return@mapNotNull null
            val trip = position.trip
            Vehicle(
                vehicleID = position.vehicle.id.toIntOrNull() ?: 0,
                tripID = trip.tripId,
                routeNumber = trip.routeId.toIntOrNull() ?: 0,
                latitude = position.position.latitude.toDouble(),
                longitude = position.position.longitude.toDouble(),
                bearing = if (position.position.hasBearing()) position.position.bearing else 0f,
                time = if (position.hasTimestamp()) position.timestamp * 1000 else 0L
            )
        }
}
