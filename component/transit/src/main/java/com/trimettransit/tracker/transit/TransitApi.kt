package com.trimettransit.tracker.transit

import android.content.Context
import timber.log.Timber
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.util.ConnectionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.BlockPosition
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.VehiclePosition
import com.trimettransit.tracker.model.computeTransitType
import org.json.JSONObject

object TransitApi {
    private val parser = JSONParser

    private fun parseRouteObj(desc: String, routeId: Int, type: String): Route {
        return Route(
            desc = desc,
            routeId = routeId,
            isBus = type == "B",
            isMax = type == "R" && desc.contains("MAX"),
            isStreetcar = desc.contains("Portland Streetcar") && type == "R",
            isWes = type == "R" && desc.contains("WES")
        )
    }

    private fun parseRoute(obj: JSONObject): Route {
        val desc = obj.optString("desc", "")
        val routeId = obj.optInt("route", 0)
        val type = obj.optString("type", "")
        return parseRouteObj(desc, routeId, type)
    }

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
                val route = parseRoute(obj)
                if (route.desc != "Portland Aerial Tram") {
                    routes.add(route)
                }
            }
            routes
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
            val routeObj = json.getJSONObject("resultSet").getJSONArray("route").getJSONObject(0)
            val route = parseRoute(routeObj)
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

            val route = parseRoute(route0)
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
            val resultSet = json.getJSONObject("resultSet")

            val arrivalArr = resultSet.optJSONArray("arrival")
            val arrivalList = mutableListOf<Arrival>()
            val parsedBlockPositions = mutableListOf<BlockPosition>()
            if (arrivalArr != null) {
                for (i in 0 until arrivalArr.length()) {
                    val obj = arrivalArr.getJSONObject(i)
                    val estimatedMs = obj.optLong("estimated", -1)
                    val scheduledMs = obj.optLong("scheduled", -1)
                    val arrival = Arrival(
                        fullSign = obj.optString("fullSign", ""),
                        shortSign = obj.optString("shortSign", ""),
                        estimated = if (estimatedMs != -1L) org.joda.time.DateTime(estimatedMs) else null,
                        scheduled = if (scheduledMs != -1L) org.joda.time.DateTime(scheduledMs) else null,
                        routeId = obj.optInt("route", 0),
                        status = obj.optString("status", ""),
                        dropOffOnly = obj.optBoolean("dropOffOnly", false),
                        reason = obj.optString("reason", ""),
                        tripID = obj.optString("tripID", ""),
                        blockID = obj.optInt("blockID", 0),
                        vehicleID = obj.optInt("vehicleID", 0),
                        feet = obj.optInt("feet", 0),
                        dir = obj.optInt("dir", 0),
                        estimatedMillis = if (estimatedMs != -1L) estimatedMs else 0L,
                        scheduledMillis = if (scheduledMs != -1L) scheduledMs else 0L
                    )
                    arrivalList.add(arrival)
                    // TriMet returns each block's live position nested inside its arrival object
                    // (only when showPosition/true is requested)
                    val bpObj = obj.optJSONObject("blockPosition")
                    if (bpObj != null) {
                        val bp = BlockPosition(
                            id = bpObj.optInt("id", 0),
                            at = bpObj.optLong("at", 0),
                            vehicleID = bpObj.optInt("vehicleID", 0),
                            feet = bpObj.optInt("feet", 0),
                            bearing = bpObj.optDouble("heading", 0.0).toFloat(),
                            lat = bpObj.optDouble("lat", 0.0),
                            lng = bpObj.optDouble("lng", 0.0),
                            routeNumber = bpObj.optInt("routeNumber", 0),
                            direction = bpObj.optInt("direction", 0),
                            tripID = bpObj.optString("tripID", ""),
                            isNewTrip = bpObj.optBoolean("newTrip", false)
                        )
                        parsedBlockPositions.add(bp)
                    }
                }
            }

            // Parse top-level detours
            val detourArr = resultSet.optJSONArray("detour")
            var detours = emptyList<Detour>()
            if (detourArr != null) {
                val detourList = mutableListOf<Detour>()
                for (i in 0 until detourArr.length()) {
                    val obj = detourArr.getJSONObject(i)
                    val routesArr = obj.optJSONArray("route")
                        ?: obj.optJSONArray("routes")
                    val routes = if (routesArr != null) {
                        // Each element is a route object per TriMet's docs; fall back to
                        // a plain int for robustness against legacy shapes.
                        MutableList(routesArr.length()) { k ->
                            when (val el = routesArr.opt(k)) {
                                is JSONObject -> el.optInt("route", 0)
                                else -> routesArr.optInt(k, 0)
                            }
                        }
                    } else {
                        emptyList()
                    }
                    detourList.add(
                        Detour(
                            id = obj.optInt("id", 0),
                            desc = obj.optString("desc", ""),
                            routes = routes
                        )
                    )
                }
                detours = detourList
            }

            // Parse location elements for stop coordinates
            var stopLat = 0.0
            var stopLng = 0.0
            val locationArr = resultSet.optJSONArray("location")
            if (locationArr != null && locationArr.length() > 0) {
                val loc = locationArr.getJSONObject(0)
                stopLat = loc.optDouble("lat", 0.0)
                stopLng = loc.optDouble("lng", 0.0)
            }

            ArrivalsResult(
                arrivals = arrivalList,
                blockPositions = parsedBlockPositions,
                detours = detours,
                stopLat = stopLat,
                stopLng = stopLng
            )
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
            val resultSet = json.getJSONObject("resultSet")
            val vehicleArr = resultSet.optJSONArray("vehicle")
            if (vehicleArr == null) return@withContext emptyList()

            val vehicles = mutableListOf<VehiclePosition>()
            for (i in 0 until vehicleArr.length()) {
                val obj = vehicleArr.getJSONObject(i)
                val vp = VehiclePosition(
                    vehicleID = obj.optInt("vehicleID", 0),
                    type = obj.optString("type", ""),
                    blockID = obj.optInt("blockID", 0),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    bearing = obj.optDouble("bearing", 0.0).toFloat(),
                    routeNumber = obj.optInt("routeNumber", 0),
                    direction = obj.optInt("direction", 0),
                    tripID = obj.optString("tripID", ""),
                    isNewTrip = obj.optBoolean("newTrip", false),
                    delay = obj.optInt("delay", 0),
                    signMessage = obj.optString("signMessage", ""),
                    signMessageLong = obj.optString("signMessageLong", ""),
                    nextLocID = obj.optInt("nextLocID", 0),
                    nextStopSeq = obj.optInt("nextStopSeq", 0),
                    lastLocID = obj.optInt("lastLocID", 0),
                    lastStopSeq = obj.optInt("lastStopSeq", 0),
                    serviceDate = obj.optLong("serviceDate", 0),
                    locationInScheduleDay = obj.optInt("locationInScheduleDay", 0),
                    time = obj.optLong("time", 0),
                    expires = obj.optLong("expires", 0),
                    isInCongestion = obj.optBoolean("inCongestion", false),
                    loadPercentage = obj.optInt("loadPercentage", 0),
                    garage = obj.optString("garage", ""),
                    extrablockID = obj.optString("extrablockID", ""),
                    isOffRoute = obj.optBoolean("offRoute", false)
                )
                vehicles.add(vp)
            }
            vehicles
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
            val resultSet = json.getJSONObject("resultSet")
            val locationArr = resultSet.optJSONArray("location")
            if (locationArr == null) return@withContext emptyList()

            val stops = mutableListOf<Stop>()
            for (i in 0 until locationArr.length()) {
                val obj = locationArr.getJSONObject(i)
                val routeArr = obj.optJSONArray("route")
                val routes = if (routeArr != null) {
                    buildList {
                        for (j in 0 until routeArr.length()) {
                            add(parseRoute(routeArr.getJSONObject(j)))
                        }
                    }
                } else {
                    emptyList()
                }
                stops.add(
                    Stop(
                        desc = obj.optString("desc", ""),
                        dirDesc = obj.optString("dir", ""),
                        latitude = obj.optDouble("lat", 0.0),
                        longitude = obj.optDouble("lng", 0.0),
                        transitType = computeTransitType(routes),
                        locId = obj.optInt("locid", 0),
                        routes = routes
                    )
                )
            }
            stops
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
            val resultSet = json.getJSONObject("resultSet")
            val locationArr = resultSet.optJSONArray("location")
            if (locationArr == null || locationArr.length() == 0) return@withContext null
            val obj = locationArr.getJSONObject(0)
            val routeArr = obj.optJSONArray("route")
            val routes = if (routeArr != null) {
                buildList {
                    for (j in 0 until routeArr.length()) {
                        add(parseRoute(routeArr.getJSONObject(j)))
                    }
                }
            } else {
                emptyList()
            }
            Stop(
                desc = obj.optString("desc", ""),
                dirDesc = obj.optString("dir", ""),
                latitude = obj.optDouble("lat", 0.0),
                longitude = obj.optDouble("lng", 0.0),
                transitType = computeTransitType(routes),
                locId = obj.optInt("locid", 0),
                routes = routes
            )
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
            val resultSet = json.optJSONObject("resultSet") ?: return@withContext null
            val routeArr = resultSet.optJSONArray("route") ?: return@withContext null

            // Accumulate a mutable description of each stop, then build immutable Stops.
            data class StopBuilder(
                var desc: String = "",
                var dirDesc: String = "",
                var latitude: Double = 0.0,
                var longitude: Double = 0.0,
                var routeNum: Int = 0,
                var locId: Int = 0,
                var routes: MutableList<Route> = mutableListOf()
            )

            val buildersById = LinkedHashMap<Int, StopBuilder>()
            for (ri in 0 until routeArr.length()) {
                val routeObj = routeArr.getJSONObject(ri)
                val dirArr = routeObj.optJSONArray("dir") ?: continue
                val routeNum = routeObj.optInt("route", 0)
                val route = parseRoute(routeObj)
                for (di in 0 until dirArr.length()) {
                    val dirObj = dirArr.getJSONObject(di)
                    val stopArr = dirObj.optJSONArray("stop") ?: continue
                    val dirDesc = dirObj.optString("desc", "")
                    for (si in 0 until stopArr.length()) {
                        val obj = stopArr.getJSONObject(si)
                        val locId = obj.optInt("locid", 0)
                        val builder = buildersById[locId]
                        if (builder == null) {
                            val stopDir = obj.optString("dir", "")
                            buildersById[locId] = StopBuilder(
                                desc = obj.optString("desc", ""),
                                dirDesc = if (stopDir == "") dirDesc else stopDir,
                                latitude = obj.optDouble("lat", 0.0),
                                longitude = obj.optDouble("lng", obj.optDouble("lon", 0.0)),
                                routeNum = routeNum,
                                locId = locId,
                                routes = mutableListOf(route)
                            )
                        } else {
                            builder.routes.add(route)
                        }
                    }
                }
            }
            buildersById.values
                .map { b ->
                    Stop(
                        desc = b.desc,
                        dirDesc = b.dirDesc,
                        latitude = b.latitude,
                        longitude = b.longitude,
                        transitType = computeTransitType(b.routes),
                        routeNum = b.routeNum,
                        locId = b.locId,
                        routes = b.routes
                    )
                }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch search stops")
            null
        }
    }
}
