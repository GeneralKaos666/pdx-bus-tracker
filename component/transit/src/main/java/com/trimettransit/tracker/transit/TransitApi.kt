package com.trimettransit.tracker.transit

import android.content.Context
import android.net.Uri
import java.io.IOException
import timber.log.Timber
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.StopsWithArrivals
import com.trimettransit.tracker.model.TransitAlert
import com.trimettransit.tracker.model.TripPlannerError
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripRequestOptions
import com.trimettransit.tracker.model.TripRequestTime
import com.trimettransit.tracker.model.VehicleResult
import com.trimettransit.tracker.model.BlockStatusResult
import com.trimettransit.tracker.model.TripStatusResult
import com.trimettransit.tracker.model.computeTransitType
import com.trimettransit.tracker.util.ConnectionUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal object TransitApi {
    private val parser = JSONParser

    internal fun scrubApiKey(msg: String, apiKey: String): String =
        if (apiKey.isBlank()) msg else msg.replace("/appID/$apiKey", "/appID/<redacted>").replace(apiKey, "<redacted>")

    // Rebuilds the throwable chain with every message scrubbed: Timber.e prints the
    // full "Caused by" chain, so chaining the raw exception would leak the key via a
    // cause message (e.g. an OkHttp IOException embedding the request URL).
    internal fun scrubbedForLog(e: Exception, apiKey: String): IOException {
        val top = IOException(scrubApiKey(e.message ?: e.toString(), apiKey))
        top.stackTrace = e.stackTrace
        val seen = mutableSetOf<Throwable>(e)
        var orig: Throwable? = e.cause
        var copy: Throwable = top
        while (orig != null && seen.add(orig)) {
            val next = IOException(scrubApiKey(orig.message ?: orig.toString(), apiKey))
            next.stackTrace = orig.stackTrace
            copy.initCause(next)
            copy = next
            orig = orig.cause
        }
        return top
    }

    private suspend fun <T> guarded(
        context: Context,
        label: String,
        block: suspend (String) -> T?
    ): T? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Timber.w("TriMet API key not configured")
            return@withContext null
        }
        try {
            block(apiKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(scrubbedForLog(e, apiKey), "Failed to $label")
            null
        }
    }

    /** Route description excluded from listings (non-revenue aerial tram). */
    const val EXCLUDED_ROUTE_DESC = "Portland Aerial Tram"

    suspend fun fetchRoutes(context: Context): List<Route>? = guarded(context, "fetch routes") { apiKey ->
        val baseUrl = context.getString(R.string.base_route_url)
        val url = "$baseUrl/appID/$apiKey"
        val json = parser.fetch(url)
        parseRoutes(json.optJSONObject("resultSet"))
    }

    /**
     * A successful route response with no route array is an empty listing, not a
     * transport failure. Keep null reserved for the guarded network/configuration
     * failure path so the route screen can distinguish "nothing returned" from
     * "could not load".
     */
    internal fun parseRoutes(resultSet: JSONObject?): List<Route> {
        val arr = resultSet?.optJSONArray("route") ?: return emptyList()
        val routes = mutableListOf<Route>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val route = TransitJsonMapper.parseRoute(obj)
            if (route.desc != EXCLUDED_ROUTE_DESC) {
                routes.add(route)
            }
        }
        return routes
    }

    suspend fun fetchDirections(context: Context, routeId: Int): List<Direction>? =
        guarded(context, "fetch directions") { apiKey ->
            val baseUrl = context.getString(R.string.base_route_url)
            val url = "$baseUrl/appID/$apiKey/route/$routeId/dir/true"
            val json = parser.fetch(url)
            val dirs = mutableListOf<Direction>()
            val routeArr = json.getJSONObject("resultSet").optJSONArray("route")
            if (routeArr == null || routeArr.length() == 0) return@guarded emptyList()
            val routeObj = routeArr.optJSONObject(0) ?: return@guarded emptyList()
            val route = TransitJsonMapper.parseRoute(routeObj)
            val arr = routeObj.optJSONArray("dir") ?: return@guarded emptyList()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val dir = Direction(
                    dir = obj.optInt("dir", 0),
                    desc = obj.optString("desc", ""),
                    route = route
                )
                dirs.add(dir)
            }
            dirs
        }

    suspend fun fetchStops(context: Context, routeId: Int, directionId: Int): List<Stop>? =
        guarded(context, "fetch stops") { apiKey ->
            val baseUrl = context.getString(R.string.base_route_url)
            val url = "$baseUrl/appID/$apiKey/route/$routeId/dir/$directionId/stops/true"
            val json = parser.fetch(url)
            val resultSet = json.optJSONObject("resultSet") ?: return@guarded null
            val routeArr = resultSet.optJSONArray("route")
            if (routeArr == null || routeArr.length() == 0) return@guarded null

            val route0 = routeArr.optJSONObject(0) ?: return@guarded null
            val dirArr = route0.optJSONArray("dir")
            if (dirArr == null || dirArr.length() == 0) return@guarded null

            val dir0 = dirArr.optJSONObject(0) ?: return@guarded null
            val stopArr = dir0.optJSONArray("stop")
            if (stopArr == null || stopArr.length() == 0) return@guarded emptyList()

            val route = TransitJsonMapper.parseRoute(route0)
            val stops = mutableListOf<Stop>()
            for (i in 0 until stopArr.length()) {
                val obj = stopArr.optJSONObject(i) ?: continue
                val dirField = obj.optString("dir", "")
                val dirDesc = if (dirField == "") context.getString(R.string.stop_bidirectional_text) else dirField
                val latitude = obj.optDouble("lat", 0.0)
                val longitude = obj.optDouble("lng", obj.optDouble("lon", 0.0))
                if (!TransitJsonMapper.isValidCoordinate(latitude, longitude)) continue
                stops.add(
                    Stop(
                        desc = obj.optString("desc", ""),
                        dirDesc = dirDesc,
                        latitude = latitude,
                        longitude = longitude,
                        transitType = computeTransitType(listOf(route)),
                        locId = obj.optInt("locid", 0),
                        routes = listOf(route)
                    )
                )
            }
            stops
        }

    suspend fun fetchArrivals(
        context: Context,
        locIds: List<Int>,
        showPosition: Boolean = false,
        minutes: Int = 20,
        maxArrivals: Int = 2
    ): ArrivalsResult? = guarded(context, "fetch arrivals") { apiKey ->
        val baseUrl = context.getString(R.string.base_arrival_url)
        val url = buildString {
            append(baseUrl)
            append("/appID/").append(apiKey)
            append("/locIDs/").append(locIds.joinToString(","))
            if (showPosition) append("/showPosition/true")
            append("/minutes/").append(minutes)
            append("/arrivals/").append(maxArrivals)
        }
        val json = parser.fetch(url)
        TransitJsonMapper.parseArrivals(json.getJSONObject("resultSet"))
    }

    /**
     * Fetches TriMet Alerts V2 entries scoped to [routes] and/or [locIds]. This app
     * has no unscoped/system-wide alerts UI, so a request with both empty is refused
     * before any network call — the same fail-closed intent as [guarded]'s offline
     * and missing-API-key checks, just evaluated first since it needs no network.
     */
    suspend fun fetchAlerts(
        context: Context,
        routes: List<Int>? = null,
        locIds: List<Int>? = null
    ): List<TransitAlert>? {
        if (!isAlertsScopeValid(routes, locIds)) {
            Timber.w("Refusing unscoped alerts fetch: routes and locIDs both empty")
            return null
        }
        return guarded(context, "fetch alerts") { apiKey ->
            val baseUrl = context.getString(R.string.base_alerts_url)
            val url = buildAlertsUrl(baseUrl, apiKey, routes, locIds)
            val json = parser.fetch(url)
            TransitJsonMapper.parseAlerts(json.getJSONObject("resultSet"))
        }
    }

    /**
     * Fetches nearby stops. The trailing `includeArrivals`/`embedArrivals`/`maxStopArrivals`/
     * `showRouteDirs`/`minutes`/`maxArrivals` parameters are opt-in (default off) and only
     * widen the request URL — omitting them reproduces the exact prior request/response
     * shape. Prefer [fetchStopsWithArrivals] to actually consume the embedded arrivals;
     * these params exist here so both entry points share one URL builder.
     */
    suspend fun fetchStopsByLocation(
        context: Context,
        ll: String,
        feet: Int? = null,
        meters: Int? = null,
        bbox: String? = null,
        maxStops: Int? = null,
        showRoutes: Boolean = true,
        includeArrivals: Boolean = false,
        embedArrivals: Boolean = true,
        maxStopArrivals: Int? = null,
        showRouteDirs: Boolean = false,
        minutes: Int? = null,
        maxArrivals: Int? = null
    ): List<Stop>? = guarded(context, "fetch stops by location") { apiKey ->
        val url = buildStopsByLocationUrl(
            baseUrl = context.getString(R.string.base_stop_location_v2_url),
            apiKey = apiKey,
            ll = Uri.encode(ll),
            feet = feet,
            meters = meters,
            bbox = bbox?.let { Uri.encode(it) },
            maxStops = maxStops,
            showRoutes = showRoutes,
            includeArrivals = includeArrivals,
            embedArrivals = embedArrivals,
            maxStopArrivals = maxStopArrivals,
            showRouteDirs = showRouteDirs,
            minutes = minutes,
            maxArrivals = maxArrivals
        )
        val json = parser.fetch(url)
        TransitJsonMapper.parseStopsByLocation(json.getJSONObject("resultSet"))
    }

    /**
     * Nearby-stops "fast path": one request that returns stops and their embedded
     * arrivals together, so callers (e.g. the nearby-stops list) can show a next-arrival
     * preview without a second per-stop `/arrivals` fetch. When `showRouteDirs` is
     * requested, route direction info comes back embedded on each stop's routes too, so
     * callers can skip a separate per-route directions fetch. Callers should fall back to
     * [fetchStopsByLocation] (and, if needed, a separate [fetchArrivals] call) when this
     * returns null.
     */
    suspend fun fetchStopsWithArrivals(
        context: Context,
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
    ): StopsWithArrivals? = guarded(context, "fetch stops with arrivals") { apiKey ->
        val url = buildStopsByLocationUrl(
            baseUrl = context.getString(R.string.base_stop_location_v2_url),
            apiKey = apiKey,
            ll = Uri.encode(ll),
            feet = feet,
            meters = meters,
            bbox = bbox?.let { Uri.encode(it) },
            maxStops = maxStops,
            showRoutes = showRoutes,
            includeArrivals = true,
            embedArrivals = true,
            maxStopArrivals = maxStopArrivals,
            showRouteDirs = showRouteDirs,
            minutes = minutes,
            maxArrivals = maxArrivals
        )
        val json = parser.fetch(url)
        TransitJsonMapper.parseStopsWithArrivals(json.getJSONObject("resultSet"))
    }

    suspend fun fetchStopById(context: Context, locId: Int): Stop? =
        guarded(context, "fetch stop by ID") { apiKey ->
            val baseUrl = context.getString(R.string.base_stop_location_v2_url)
            val url = "$baseUrl/appID/$apiKey/locIDs/$locId"
            val json = parser.fetch(url)
            TransitJsonMapper.parseStopById(json.getJSONObject("resultSet"))
        }

    suspend fun fetchSearchStops(context: Context): List<Stop>? =
        guarded(context, "fetch search stops") { apiKey ->
            val baseUrl = context.getString(R.string.base_route_url)
            val url = "$baseUrl/appID/$apiKey/dir/true/stops/true"
            val json = parser.fetch(url)
            TransitJsonMapper.parseSearchStops(
                json.optJSONObject("resultSet") ?: return@guarded null
            ) ?: return@guarded null
        }

    suspend fun fetchVehicles(
        context: Context,
        routes: List<Int>? = null,
        blocks: List<Int>? = null,
        ids: List<Int>? = null,
        bbox: String? = null,
        since: Long? = null,
        showNonRevenue: Boolean = false,
        onRouteOnly: Boolean = true,
        showStale: Boolean = false
    ): VehicleResult? = guarded(context, "fetch vehicles") { apiKey ->
        val baseUrl = context.getString(R.string.base_vehicle_url)
        val url = buildVehiclesUrl(
            baseUrl = baseUrl,
            apiKey = apiKey,
            routes = routes,
            blocks = blocks,
            ids = ids,
            bbox = bbox?.let { Uri.encode(it) },
            since = since,
            showNonRevenue = showNonRevenue,
            onRouteOnly = onRouteOnly,
            showStale = showStale
        )
        val json = parser.fetch(url)
        TransitJsonMapper.parseVehicles(json.getJSONObject("resultSet"))
    }

    suspend fun fetchTripStatus(
        context: Context,
        tripIds: List<String>? = null,
        blockIds: List<Int>? = null,
        showRoutes: Boolean = true,
        showStops: Boolean = true
    ): TripStatusResult? = guarded(context, "fetch trip status") { apiKey ->
        val url = buildTripStatusUrl(
            baseUrl = context.getString(R.string.base_trip_status_url),
            apiKey = apiKey,
            tripIds = tripIds,
            blockIds = blockIds,
            showRoutes = showRoutes,
            showStops = showStops
        )
        TransitJsonMapper.parseTripStatus(parser.fetch(url).getJSONObject("resultSet"))
    }

    suspend fun fetchBlockStatus(
        context: Context,
        blockId: Int? = null,
        blockIds: List<Int>? = null,
        showRoutes: Boolean = true,
        showStops: Boolean = true
    ): BlockStatusResult? = guarded(context, "fetch block status") { apiKey ->
        val url = buildBlockStatusUrl(
            baseUrl = context.getString(R.string.base_block_status_url),
            apiKey = apiKey,
            blockId = blockId,
            blockIds = blockIds,
            showRoutes = showRoutes,
            showStops = showStops
        )
        TransitJsonMapper.parseBlockStatus(parser.fetch(url).getJSONObject("resultSet"))
    }

    suspend fun fetchTripPlan(
        context: Context,
        from: TripPoint,
        to: TripPoint,
        time: TripRequestTime,
        options: TripRequestOptions = TripRequestOptions()
    ): TripPlanResult = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext TripPlanResult.Error(TripPlannerError.NETWORK)
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Timber.w("TriMet API key not configured")
            return@withContext TripPlanResult.Error(TripPlannerError.NETWORK)
        }
        if (!from.isValid || !to.isValid) {
            Timber.w("Trip planner request rejected: invalid coordinates")
            return@withContext TripPlanResult.Error(TripPlannerError.UNKNOWN)
        }
        // The Trip Planner WS interprets date/time in the Transit service's local zone.
        val requestedMillis = time.timeMillis ?: System.currentTimeMillis()
        val date = formatTripPlannerDate(requestedMillis)
        val clock = formatTripPlannerClock(requestedMillis)
        val baseUrl = context.getString(R.string.base_trip_planner_url)
        val url = buildTripPlannerRequestUrl(
            baseUrl = baseUrl,
            apiKey = apiKey,
            fromPlace = Uri.encode(from.description),
            fromCoord = "${from.longitude},${from.latitude}",
            toPlace = Uri.encode(to.description),
            toCoord = "${to.longitude},${to.latitude}",
            date = date,
            clock = Uri.encode(clock),
            arriveBy = time.arriveBy,
            options = options
        )
        try {
            val xml = parser.fetchXml(url)
            TripPlannerXmlParser.parseTripPlanResponse(xml)
                ?: TripPlanResult.Error(TripPlannerError.UNKNOWN)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val classified = TripPlanFailureClassifier.classify(e)
            // Timeouts, DNS/TLS trouble, and WS 5xx hiccups are transient — give the
            // request one retry before surfacing an error, mirroring the widget's fetch.
            if (classified == TripPlannerError.NETWORK || classified == TripPlannerError.SYSTEM_OUTAGE) {
                try {
                    val xml = parser.fetchXml(url)
                    return@withContext TripPlannerXmlParser.parseTripPlanResponse(xml)
                        ?: TripPlanResult.Error(TripPlannerError.UNKNOWN)
                } catch (e2: CancellationException) {
                    throw e2
                } catch (e2: Exception) {
                    Timber.e(scrubbedForLog(e2, apiKey), "Failed to fetch trip plan (retry)")
                    return@withContext TripPlanResult.Error(TripPlanFailureClassifier.classify(e2))
                }
            }
            Timber.e(scrubbedForLog(e, apiKey), "Failed to fetch trip plan")
            TripPlanResult.Error(classified)
        }
    }

}

/**
 * Assembles the `/ws/v2/vehicles` request URL from already-rendered pieces. Pure so
 * the parameter layout (filters + `showNonRevenue`/`onRouteOnly`/`showStale` defaults)
 * is unit-testable without Android framework calls; `bbox` arrives Uri-encoded by the
 * caller, matching [buildTripPlannerRequestUrl]'s convention.
 */
/**
 * True when at least one alert scope ([routes] or [locIds]) is non-empty. Pure so
 * the "refuse unscoped calls" rule is unit-testable without a Context/network stack.
 */
internal fun isAlertsScopeValid(routes: List<Int>?, locIds: List<Int>?): Boolean =
    !routes.isNullOrEmpty() || !locIds.isNullOrEmpty()

/**
 * Assembles the `/ws/v2/alerts` request URL. Pure and unit-testable, mirroring
 * [buildVehiclesUrl]'s convention of path-segment filters.
 */
internal fun buildAlertsUrl(
    baseUrl: String,
    apiKey: String,
    routes: List<Int>? = null,
    locIds: List<Int>? = null
): String = buildString {
    append(baseUrl)
    append("/appID/").append(apiKey)
    if (!routes.isNullOrEmpty()) append("/routes/").append(routes.joinToString(","))
    if (!locIds.isNullOrEmpty()) append("/locIDs/").append(locIds.joinToString(","))
}

internal fun buildVehiclesUrl(
    baseUrl: String,
    apiKey: String,
    routes: List<Int>? = null,
    blocks: List<Int>? = null,
    ids: List<Int>? = null,
    bbox: String? = null,
    since: Long? = null,
    showNonRevenue: Boolean = false,
    onRouteOnly: Boolean = true,
    showStale: Boolean = false
): String = buildString {
    append(baseUrl)
    append("/appID/").append(apiKey)
    if (!routes.isNullOrEmpty()) append("/routes/").append(routes.joinToString(","))
    if (!blocks.isNullOrEmpty()) append("/blocks/").append(blocks.joinToString(","))
    if (!ids.isNullOrEmpty()) append("/ids/").append(ids.joinToString(","))
    if (!bbox.isNullOrEmpty()) append("/bbox/").append(bbox)
    if (since != null) append("/since/").append(since)
    if (showNonRevenue) append("/showNonRevenue/true")
    if (!onRouteOnly) append("/onRouteOnly/false")
    if (showStale) append("/showStale/true")
}

/**
 * Assembles the `/ws/v2/stops` request URL. Pure so the opt-in arrivals-embedding
 * layout is unit-testable without Android framework calls; `ll`/`bbox` arrive already
 * Uri-encoded by the caller, matching [buildVehiclesUrl]'s convention. `embedArrivals`,
 * `maxStopArrivals`, `minutes`, and `maxArrivals` are only appended when `includeArrivals`
 * is true, so a non-combined caller's URL is byte-identical to before this parameter
 * existed.
 */
internal fun buildStopsByLocationUrl(
    baseUrl: String,
    apiKey: String,
    ll: String,
    feet: Int? = null,
    meters: Int? = null,
    bbox: String? = null,
    maxStops: Int? = null,
    showRoutes: Boolean = true,
    includeArrivals: Boolean = false,
    embedArrivals: Boolean = true,
    maxStopArrivals: Int? = null,
    showRouteDirs: Boolean = false,
    minutes: Int? = null,
    maxArrivals: Int? = null
): String = buildString {
    append(baseUrl)
    append("/appID/").append(apiKey)
    append("/ll/").append(ll)
    if (feet != null) append("/feet/").append(feet)
    if (meters != null) append("/meters/").append(meters)
    if (!bbox.isNullOrEmpty()) append("/bbox/").append(bbox)
    if (maxStops != null) append("/maxStops/").append(maxStops)
    if (showRoutes) append("/showRoutes/true")
    if (includeArrivals) {
        if (embedArrivals) append("/embedArrivals/true")
        if (maxStopArrivals != null) append("/maxStopArrivals/").append(maxStopArrivals)
        if (minutes != null) append("/minutes/").append(minutes)
        if (maxArrivals != null) append("/arrivals/").append(maxArrivals)
    }
    if (showRouteDirs) append("/showRouteDirs/true")
}

internal fun encodePathSegment(raw: String): String =
    buildString(raw.length) {
        for (ch in raw) {
            if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' ||
                ch == '-' || ch == '.' || ch == '_' || ch == '~'
            ) {
                append(ch)
            } else {
                val bytes = ch.toString().toByteArray(Charsets.UTF_8)
                for (b in bytes) {
                    append('%')
                    append(Character.forDigit((b.toInt() shr 4) and 0xF, 16).uppercaseChar())
                    append(Character.forDigit(b.toInt() and 0xF, 16).uppercaseChar())
                }
            }
        }
    }

internal fun buildTripStatusUrl(
    baseUrl: String,
    apiKey: String,
    tripIds: List<String>? = null,
    blockIds: List<Int>? = null,
    showRoutes: Boolean = true,
    showStops: Boolean = true
): String = buildString {
    append(baseUrl).append("/appID/").append(apiKey)
    if (!tripIds.isNullOrEmpty()) append("/tripIDs/").append(tripIds.joinToString(",") { encodePathSegment(it) })
    if (!blockIds.isNullOrEmpty()) append("/blockIDs/").append(blockIds.joinToString(","))
    if (showRoutes) append("/showRoutes/true")
    if (showStops) append("/showStops/true")
}

internal fun buildBlockStatusUrl(
    baseUrl: String,
    apiKey: String,
    blockId: Int? = null,
    blockIds: List<Int>? = null,
    showRoutes: Boolean = true,
    showStops: Boolean = true
): String = buildString {
    append(baseUrl).append("/appID/").append(apiKey)
    if (blockId != null) append("/blockID/").append(blockId)
    if (!blockIds.isNullOrEmpty()) append("/blockIDs/").append(blockIds.joinToString(","))
    if (showRoutes) append("/showRoutes/true")
    if (showStops) append("/showStops/true")
}
