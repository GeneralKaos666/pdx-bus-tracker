package com.trimettransit.tracker.transit

import android.content.Context
import android.net.Uri
import java.io.IOException
import timber.log.Timber
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.TripPlannerError
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripRequestOptions
import com.trimettransit.tracker.model.TripRequestTime
import com.trimettransit.tracker.model.computeTransitType
import com.trimettransit.tracker.util.ConnectionUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object TransitApi {
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

    suspend fun fetchRoutes(context: Context): List<Route>? = guarded(context, "fetch routes") { apiKey ->
        val baseUrl = context.getString(R.string.base_route_url)
        val url = "$baseUrl/appID/$apiKey"
        val json = parser.fetch(url)
        val routes = mutableListOf<Route>()
        val arr = json.getJSONObject("resultSet").getJSONArray("route")
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val route = TransitJsonMapper.parseRoute(obj)
            if (route.desc != "Portland Aerial Tram") {
                routes.add(route)
            }
        }
        routes
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
                stops.add(
                    Stop(
                        desc = obj.optString("desc", ""),
                        dirDesc = dirDesc,
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lng", obj.optDouble("lon", 0.0)),
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

    suspend fun fetchStopsByLocation(
        context: Context,
        ll: String,
        feet: Int? = null,
        meters: Int? = null,
        bbox: String? = null,
        maxStops: Int? = null,
        showRoutes: Boolean = true
    ): List<Stop>? = guarded(context, "fetch stops by location") { apiKey ->
        val baseUrl = context.getString(R.string.base_stop_location_v2_url)
        val url = buildString {
            append(baseUrl)
            append("/appID/").append(apiKey)
            append("/ll/").append(ll)
            if (feet != null) append("/feet/").append(feet)
            if (meters != null) append("/meters/").append(meters)
            if (bbox != null) append("/bbox/").append(bbox)
            if (maxStops != null) append("/maxStops/").append(maxStops)
            if (showRoutes) append("/showRoutes/true")
        }
        val json = parser.fetch(url)
        TransitJsonMapper.parseStopsByLocation(json.getJSONObject("resultSet"))
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
        val requested = time.timeMillis?.let { DateTime(it) } ?: DateTime.now()
        val date = DateTimeFormat.forPattern("M-d-yyyy").print(requested)
        val clock = DateTimeFormat.forPattern("h:mm a").print(requested)
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