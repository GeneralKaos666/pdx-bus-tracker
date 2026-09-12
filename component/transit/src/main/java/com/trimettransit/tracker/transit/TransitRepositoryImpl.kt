package com.trimettransit.tracker.transit

import android.content.Context
import com.trimettransit.tracker.model.Alert
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripRequestOptions
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

    private val searchCache = SearchStopCache()

    override suspend fun getRoutes(): List<Route>? = TransitApi.fetchRoutes(context)

    override suspend fun getDirections(routeId: Int): List<Direction>? =
        TransitApi.fetchDirections(context, routeId)

    override suspend fun getStops(routeId: Int, directionId: Int): List<Stop>? =
        TransitApi.fetchStops(context, routeId, directionId)

    override suspend fun getAlerts(): List<Alert>? = TransitApi.fetchAlerts(context)

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

    override suspend fun searchStops(): List<Stop>? {
        searchCache.get()?.let { return it }
        val fresh = TransitApi.fetchSearchStops(context)
        if (fresh != null && fresh.isNotEmpty()) {
            StopSearchStore.write(context, fresh)
            searchCache.put(fresh)
            return fresh
        }
        val fallback = StopSearchStore.read(context)
        if (fallback != null) {
            searchCache.putFallback(fallback)
            return fallback
        }
        return null
    }

    override suspend fun planTrip(
        from: TripPoint,
        to: TripPoint,
        time: TripRequestTime,
        options: TripRequestOptions
    ): TripPlanResult? = TransitApi.fetchTripPlan(context, from, to, time, options)
}

/**
 * Bounds fetchSearchStops to once per 15 minutes per process, and keeps a
 * 30-second memo of the on-disk fallback. The full-network stop dump is several
 * MB; re-parsing it per keystroke was the peak-memory hot path. Guarded with
 * @Synchronized because searchStops may be called from concurrent scopes (home +
 * trip planner share one repository instance).
 */
private class SearchStopCache {
    private data class Snapshot(val stops: List<Stop>, val fetchedAtMillis: Long)
    private var snapshot: Snapshot? = null

    @Synchronized
    fun get(): List<Stop>? {
        val sn = snapshot ?: return null
        val ageMillis = System.currentTimeMillis() - sn.fetchedAtMillis
        return if (ageMillis < TTL_MILLIS) sn.stops else null
    }

    @Synchronized
    fun put(stops: List<Stop>) {
        snapshot = Snapshot(stops, System.currentTimeMillis())
    }

    /**
     * Memoizes the disk cache so repeated keys don't re-read it each time, but
     * expires after [FALLBACK_HOLDOVER_MILLIS] so the next search still attempts
     * a fresh network fetch once connectivity returns.
     */
    @Synchronized
    fun putFallback(stops: List<Stop>) {
        snapshot = Snapshot(stops, System.currentTimeMillis() - (TTL_MILLIS - FALLBACK_HOLDOVER_MILLIS))
    }

    private companion object {
        const val TTL_MILLIS = 15L * 60 * 1000
        const val FALLBACK_HOLDOVER_MILLIS = 30L * 1000
    }
}
