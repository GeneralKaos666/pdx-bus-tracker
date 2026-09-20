package com.trimettransit.tracker.model.repository

import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.StopsWithArrivals
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripRequestOptions
import com.trimettransit.tracker.model.TripRequestTime
import com.trimettransit.tracker.model.TransitAlert
import com.trimettransit.tracker.model.VehicleResult
import com.trimettransit.tracker.model.BlockStatusResult
import com.trimettransit.tracker.model.TripStatusResult

/**
 * Data-access boundary for the TriMet live transit API. Implementations live in
 * `component:transit`. App code (phone screens and widget workers) depends on
 * this interface rather than on the concrete [TransitApi] object so the data
 * source can be substituted or tested.
 */
interface TransitRepository {
    /** True when a TriMet API key is configured; false means live calls will no-op. */
    fun isConfigured(): Boolean
    suspend fun getRoutes(): List<Route>?
    suspend fun getDirections(routeId: Int): List<Direction>?
    suspend fun getStops(routeId: Int, directionId: Int): List<Stop>?

    /**
     * Fetches TriMet Alerts V2 entries scoped to specific [routes] and/or stop
     * [locIds]. At least one non-empty scope must be supplied: this app has no
     * unscoped/system-wide alerts UI, so passing both null/empty refuses the call
     * (returns null) rather than fetching the unfiltered feed.
     */
    suspend fun getAlerts(routes: List<Int>? = null, locIds: List<Int>? = null): List<TransitAlert>?

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

    /**
     * Nearby-stops "fast path": stops near [ll] with their arrivals already embedded
     * in the same response, keyed by stop locId in [StopsWithArrivals.arrivalsByStop].
     * Returns null on the same offline/missing-key/error conditions as
     * [getStopsByLocation] — callers should fall back to that call (and, if arrival
     * previews are still wanted, a separate [getArrivals] call) when this returns null.
     */
    suspend fun getStopsWithArrivals(
        ll: String,
        feet: Int? = null,
        meters: Int? = null,
        bbox: String? = null,
        maxStops: Int? = null,
        showRoutes: Boolean = true,
        maxStopArrivals: Int? = null,
        showRouteDirs: Boolean = false,
        minutes: Int? = null,
        maxArrivals: Int? = null
    ): StopsWithArrivals?
    suspend fun getStopById(locId: Int): Stop?
    suspend fun searchStops(): List<Stop>?

    /** Plans a from→to trip via the TriMet Trip Planner WS. Offline/missing key surfaces as [TripPlanResult.Error]. */
    suspend fun planTrip(
        from: TripPoint,
        to: TripPoint,
        time: TripRequestTime = TripRequestTime(),
        options: TripRequestOptions = TripRequestOptions()
    ): TripPlanResult

    /**
     * Fetches live vehicle positions from the TriMet Vehicle location WS, optionally
     * filtered by route/block/vehicle id, bounding box, or update recency. Offline or
     * a blank API key returns null; an empty fleet (no matching vehicles) returns a
     * non-null [VehicleResult] with an empty list.
     */
    suspend fun getVehicles(
        routes: List<Int>? = null,
        blocks: List<Int>? = null,
        ids: List<Int>? = null,
        bbox: String? = null,
        since: Long? = null,
        showNonRevenue: Boolean = false,
        onRouteOnly: Boolean = true,
        showStale: Boolean = false
    ): VehicleResult?

    suspend fun getTripStatus(
        tripIds: List<String>? = null,
        blockIds: List<Int>? = null,
        showRoutes: Boolean = true,
        showStops: Boolean = true
    ): TripStatusResult?

    suspend fun getBlockStatus(
        blockId: Int? = null,
        blockIds: List<Int>? = null,
        showRoutes: Boolean = true,
        showStops: Boolean = true
    ): BlockStatusResult?
}
