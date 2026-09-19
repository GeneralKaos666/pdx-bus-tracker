package com.trimettransit.tracker.transit

import android.content.Context
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripRequestOptions
import com.trimettransit.tracker.model.TripRequestTime
import com.trimettransit.tracker.model.repository.TransitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Adapter exposing the singleton [TransitApi] behind the [TransitRepository]
 * boundary. Retains only the application [Context] so this process-wide shared
 * instance can never leak an Activity/Service context.
 */
class TransitRepositoryImpl(
    context: Context
) : TransitRepository {
    private val context: Context = context.applicationContext

    private val searchCache = SearchStopCache()

    /**
     * Singleflight for the multi-MB stop dump: concurrent callers (Home search +
     * trip planner share one repository instance) await the same in-flight fetch
     * instead of each downloading/parsing it. Guarded by [searchMutex]; the fetch
     * itself runs on [searchScope] so one caller cancelling can't abort the others.
     */
    private val searchScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val searchMutex = Mutex()
    private var inflight: Deferred<List<Stop>?>? = null

    override fun isConfigured(): Boolean = ApiKeys.getTrimetApiKey().isNotBlank()

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
        val deferred = searchMutex.withLock {
            // Double-check under the lock: the winner's put() may have landed
            // while this caller was waiting for the mutex.
            searchCache.get()?.let { return it }
            inflight?.let { return@withLock it }
            searchScope.async { TransitApi.fetchSearchStops(context) }.also { inflight = it }
        }
        try {
            val fresh = deferred.await()
            if (fresh != null && fresh.isNotEmpty()) {
                withContext(Dispatchers.IO) { StopSearchStore.write(context, fresh) }
                searchCache.put(fresh)
                return fresh
            }
            val fallback = withContext(Dispatchers.IO) { StopSearchStore.read(context) }
            if (fallback != null) {
                searchCache.putFallback(fallback)
                return fallback
            }
            return null
        } finally {
            searchMutex.withLock { if (inflight === deferred) inflight = null }
        }
    }

    override suspend fun planTrip(
        from: TripPoint,
        to: TripPoint,
        time: TripRequestTime,
        options: TripRequestOptions
    ): TripPlanResult = TransitApi.fetchTripPlan(context, from, to, time, options)
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
