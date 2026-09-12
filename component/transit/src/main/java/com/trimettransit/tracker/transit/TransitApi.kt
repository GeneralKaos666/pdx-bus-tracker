package com.trimettransit.tracker.transit

import android.content.Context
import android.net.Uri
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
import com.trimettransit.tracker.model.VehiclePosition
import com.trimettransit.tracker.model.computeTransitType
import com.trimettransit.tracker.util.ConnectionUtils
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

object TransitApi {
    private val parser = JSONParser

    suspend fun fetchRoutes(context: Context): List<Route>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Timber.w("TriMet API key not configured")
            return@withContext null
        }
        try {
            val baseUrl = context.getString(R.string.base_route_url)
            val url = "$baseUrl/appID/$apiKey"
            val json = parser.fetch(url)
            val routes = mutableListOf<Route>()
            val arr = json.getJSONObject("resultSet").getJSONArray("route")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val route = TransitJsonMapper.parseRoute(obj)
                if (route.desc != "Portland Aerial Tram") {
                    routes.add(route)
                }
            }
            routes
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch routes")
            null
        }
    }

    suspend fun fetchDirections(context: Context, routeId: Int): List<Direction>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Timber.w("TriMet API key not configured")
            return@withContext null
        }
        try {
            val baseUrl = context.getString(R.string.base_route_url)
            val url = "$baseUrl/appID/$apiKey/route/$routeId/dir/true"
            val json = parser.fetch(url)
            val dirs = mutableListOf<Direction>()
            val routeArr = json.getJSONObject("resultSet").optJSONArray("route")
            if (routeArr == null || routeArr.length() == 0) return@withContext emptyList()
            val routeObj = routeArr.getJSONObject(0)
            val route = TransitJsonMapper.parseRoute(routeObj)
            val arr = routeObj.getJSONArray("dir")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val dir = Direction(
                    dir = obj.optInt("dir", 0),
                    desc = obj.optString("desc", ""),
                    route = route
                )
                dirs.add(dir)
            }
            dirs
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch directions")
            null
        }
    }

    suspend fun fetchStops(context: Context, routeId: Int, directionId: Int): List<Stop>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Timber.w("TriMet API key not configured")
            return@withContext null
        }
        try {
            val baseUrl = context.getString(R.string.base_route_url)
            val url = "$baseUrl/appID/$apiKey/route/$routeId/dir/$directionId/stops/true"
            val json = parser.fetch(url)
            val resultSet = json.optJSONObject("resultSet") ?: return@withContext null
            val routeArr = resultSet.optJSONArray("route")
            if (routeArr == null || routeArr.length() == 0) return@withContext null

            val route0 = routeArr.getJSONObject(0)
            val dirArr = route0.optJSONArray("dir")
            if (dirArr == null || dirArr.length() == 0) return@withContext null

            val dir0 = dirArr.getJSONObject(0)
            val stopArr = dir0.optJSONArray("stop")
            if (stopArr == null || stopArr.length() == 0) return@withContext emptyList()

            val route = TransitJsonMapper.parseRoute(route0)
            val stops = mutableListOf<Stop>()
            for (i in 0 until stopArr.length()) {
                val obj = stopArr.getJSONObject(i)
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch stops")
            null
        }
    }

    suspend fun fetchArrivals(
        context: Context,
        locIds: List<Int>,
        showPosition: Boolean = false,
        minutes: Int = 20,
        maxArrivals: Int = 2
    ): ArrivalsResult? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Timber.w("TriMet API key not configured")
            return@withContext null
        }
        try {
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch arrivals")
            null
        }
    }

    suspend fun fetchVehicles(
        context: Context,
        routes: List<Int>? = null,
        blocks: List<Int>? = null,
        ids: List<Int>? = null,
        bbox: String? = null,
        showNonRevenue: Boolean = false,
        onRouteOnly: Boolean = true,
        showStale: Boolean = false
    ): List<VehiclePosition>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Timber.w("TriMet API key not configured")
            return@withContext null
        }
        try {
            val baseUrl = context.getString(R.string.base_vehicles_url)
            val url = buildString {
                append(baseUrl)
                append("/appID/").append(apiKey)
                if (routes != null && routes.isNotEmpty()) {
                    append("/routes/").append(routes.joinToString(","))
                }
                if (blocks != null && blocks.isNotEmpty()) {
                    append("/blocks/").append(blocks.joinToString(","))
                }
                if (ids != null && ids.isNotEmpty()) {
                    append("/ids/").append(ids.joinToString(","))
                }
                if (bbox != null) {
                    append("/bbox/").append(bbox)
                }
                if (showNonRevenue) append("/showNonRevenue/true")
                if (!onRouteOnly) append("/onRouteOnly/false")
                if (showStale) append("/showStale/true")
            }
            val json = parser.fetch(url)
            TransitJsonMapper.parseVehicles(json.getJSONObject("resultSet"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch vehicles")
            null
        }
    }

    suspend fun fetchStopsByLocation(
        context: Context,
        ll: String,
        feet: Int? = null,
        meters: Int? = null,
        bbox: String? = null,
        maxStops: Int? = null,
        showRoutes: Boolean = true
    ): List<Stop>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Timber.w("TriMet API key not configured")
            return@withContext null
        }
        try {
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch stops by location")
            null
        }
    }

    suspend fun fetchStopById(context: Context, locId: Int): Stop? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) return@withContext null
        try {
            val baseUrl = context.getString(R.string.base_stop_location_v2_url)
            val url = "$baseUrl/appID/$apiKey/locIDs/$locId"
            val json = parser.fetch(url)
            TransitJsonMapper.parseStopById(json.getJSONObject("resultSet"))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch stop by ID")
            null
        }
    }

    suspend fun fetchSearchStops(context: Context): List<Stop>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Timber.w("TriMet API key not configured")
            return@withContext null
        }
        try {
            val baseUrl = context.getString(R.string.base_route_url)
            val url = "$baseUrl/appID/$apiKey/dir/true/stops/true"
            val json = parser.fetch(url)
            val result = TransitJsonMapper.parseSearchStops(
                json.optJSONObject("resultSet") ?: return@withContext null
            ) ?: return@withContext null
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch search stops")
            null
        }
    }

    suspend fun fetchTripPlan(
        context: Context,
        from: TripPoint,
        to: TripPoint,
        time: TripRequestTime,
        options: TripRequestOptions = TripRequestOptions()
    ): TripPlanResult? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Timber.w("TriMet API key not configured")
            return@withContext null
        }
        try {
            val now = DateTime.now()
            val requested = time.timeMillis?.let { DateTime(it) } ?: now
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
            val xml = parser.fetchXml(url)
            TripPlannerXmlParser.parseTripPlanResponse(xml)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch trip plan")
            TripPlanResult.Error(TripPlannerError.SYSTEM_OUTAGE)
        }
    }

}
