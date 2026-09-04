package com.trimettransit.tracker.transit

import android.content.Context
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripRequestTime
import com.trimettransit.tracker.model.VehiclePosition
import com.trimettransit.tracker.model.repository.TransitRepository

/**
 * Adapter exposing the singleton [TransitApi] behind the [TransitRepository]
 * boundary. Holds a [Context] (application-scoped) so callers don't pass it.
 */
class TransitRepositoryImpl(
    private val context: Context
) : TransitRepository {

    override suspend fun getRoutes(): List<Route>? = TransitApi.fetchRoutes(context)

    override suspend fun getDirections(routeId: Int): List<Direction>? =
        TransitApi.fetchDirections(context, routeId)

    override suspend fun getStops(routeId: Int, directionId: Int): List<Stop>? =
        TransitApi.fetchStops(context, routeId, directionId)

    override suspend fun getArrivals(
        locIds: List<Int>,
        showPosition: Boolean,
        minutes: Int,
        maxArrivals: Int
    ): ArrivalsResult? =
        TransitApi.fetchArrivals(context, locIds, showPosition, minutes, maxArrivals)

    override suspend fun getVehicles(
        routes: List<Int>?,
        blocks: List<Int>?,
        ids: List<Int>?,
        bbox: String?,
        showNonRevenue: Boolean,
        onRouteOnly: Boolean,
        showStale: Boolean
    ): List<VehiclePosition>? =
        TransitApi.fetchVehicles(
            context, routes, blocks, ids, bbox, showNonRevenue, onRouteOnly, showStale
        )

    override suspend fun getStopsByLocation(
        ll: String,
        feet: Int?,
        meters: Int?,
        bbox: String?,
        maxStops: Int?,
        showRoutes: Boolean
    ): List<Stop>? =
        TransitApi.fetchStopsByLocation(context, ll, feet, meters, bbox, maxStops, showRoutes)

    override suspend fun getStopById(locId: Int): Stop? = TransitApi.fetchStopById(context, locId)

    override suspend fun searchStops(): List<Stop>? = TransitApi.fetchSearchStops(context)

    override suspend fun planTrip(
        from: TripPoint,
        to: TripPoint,
        time: TripRequestTime
    ): TripPlanResult? = TransitApi.fetchTripPlan(context, from, to, time)
}
