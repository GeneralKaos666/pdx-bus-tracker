package com.trimettransit.tracker.model.repository

import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripRequestOptions
import com.trimettransit.tracker.model.TripRequestTime

/**
 * Data-access boundary for the TriMet live transit API. Implementations live in
 * `component:transit`. App code (phone screens and widget workers) depends on
 * this interface rather than on the concrete [TransitApi] object so the data
 * source can be substituted or tested.
 */
interface TransitRepository {
    suspend fun getRoutes(): List<Route>?
    suspend fun getDirections(routeId: Int): List<Direction>?
    suspend fun getStops(routeId: Int, directionId: Int): List<Stop>?

    suspend fun getArrivals(
        locIds: List<Int>,
        showPosition: Boolean = false,
        minutes: Int = 20,
        maxArrivals: Int = 2
    ): ArrivalsResult?
    suspend fun getStopsByLocation(
        ll: String,
        feet: Int? = null,
        meters: Int? = null,
        bbox: String? = null,
        maxStops: Int? = null,
        showRoutes: Boolean = true
    ): List<Stop>?
    suspend fun getStopById(locId: Int): Stop?
    suspend fun searchStops(): List<Stop>?

    /** Plans a from→to trip via the TriMet Trip Planner WS. Offline/missing key surfaces as [TripPlanResult.Error]. */
    suspend fun planTrip(
        from: TripPoint,
        to: TripPoint,
        time: TripRequestTime = TripRequestTime(),
        options: TripRequestOptions = TripRequestOptions()
    ): TripPlanResult
}
