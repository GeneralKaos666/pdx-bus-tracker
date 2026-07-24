package com.trimettransit.tracker.ui

import android.content.Context
import android.util.Log
import com.trimettransit.tracker.data.model.Direction
import com.trimettransit.tracker.data.model.Route
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.network.JSONParser
import com.trimettransit.tracker.util.ConnectionUtils
import com.trimettransit.tracker.util.ApiKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.trimettransit.tracker.R
import com.trimettransit.tracker.data.model.Arrival
import com.trimettransit.tracker.data.model.ArrivalsResult
import com.trimettransit.tracker.data.model.BlockPosition
import com.trimettransit.tracker.data.model.Detour
import com.trimettransit.tracker.data.model.VehiclePosition

object TransitApi {
    private const val TAG = "TransitApi"
    private val parser = JSONParser

private fun Route.applyRouteType(type: String, desc: String) {
    when {
        type == "R" && desc.contains("WES") -> isWes = true
        type == "R" && desc.contains("MAX") -> isMax = true
        type == "B" -> isBus = true
    }
}

private fun Route.applyStreetcarType(desc: String, type: String) {
    if (desc.contains("Portland Streetcar") && type == "R") {
        isStreetcar = true
    }
}

    suspend fun fetchRoutes(context: Context, url: String): List<Route>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        try {
            val json = parser.fetch(url) ?: return@withContext null
            val routes = mutableListOf<Route>()
            val arr = json.getJSONObject("resultSet").getJSONArray("route")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val route = Route().apply {
                    desc = obj.getString("desc")
                    routeId = obj.getInt("route")
                    applyRouteType(obj.getString("type"), obj.getString("desc"))
                    applyStreetcarType(desc, obj.getString("type"))
                }
                if (route.desc != "Portland Aerial Tram") {
                    routes.add(route)
                }
            }
            routes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch routes", e)
            null
        }
    }

    suspend fun fetchDirections(context: Context, url: String): List<Direction>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        try {
            val json = parser.fetch(url) ?: return@withContext null
            val dirs = mutableListOf<Direction>()
            val routeObj = json.getJSONObject("resultSet").getJSONArray("route").getJSONObject(0)
            val routeDesc = routeObj.optString("desc", "")
            val routeId = routeObj.optInt("route", 0)
            val routeType = routeObj.optString("type", "")
            val route = Route().apply {
                desc = routeDesc
                this.routeId = routeId
                applyRouteType(routeType, routeDesc)
                applyStreetcarType(routeDesc, routeType)
            }
            val arr = routeObj.getJSONArray("dir")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val dir = Direction().apply {
                    this.dir = obj.getInt("dir")
                    desc = obj.optString("desc", "")
                    this.route = route
                }
                dirs.add(dir)
            }
            dirs
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch directions", e)
            null
        }
    }

    suspend fun fetchStops(context: Context, url: String): List<Stop>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        try {
            val json = parser.fetch(url) ?: return@withContext null
            val resultSet = json.optJSONObject("resultSet") ?: return@withContext null
            val routeArr = resultSet.optJSONArray("route")
            if (routeArr == null || routeArr.length() == 0) return@withContext null

            val route0 = routeArr.getJSONObject(0)
            val dirArr = route0.optJSONArray("dir")
            if (dirArr == null || dirArr.length() == 0) return@withContext null

            val dir0 = dirArr.getJSONObject(0)
            val stopArr = dir0.optJSONArray("stop")
            if (stopArr == null || stopArr.length() == 0) return@withContext emptyList()

            val routeDesc = route0.optString("desc", "")
            val routeId = route0.optInt("route", 0)
            val routeType = route0.optString("type", "")
            val route = Route().apply {
                desc = routeDesc
                this.routeId = routeId
                applyRouteType(routeType, routeDesc)
                applyStreetcarType(routeDesc, routeType)
            }
            val stops = mutableListOf<Stop>()
            for (i in 0 until stopArr.length()) {
                val obj = stopArr.getJSONObject(i)
                val stop = Stop().apply {
                    desc = obj.optString("desc", "")
                    val dirField = obj.optString("dir", "")
                    if (dirField == "") {
                        dirDesc = context.getString(R.string.stop_bidirectional_text)
                    } else {
                        dirDesc = dirField
                    }
                    locId = obj.optInt("locid", 0)
                    latitude = obj.optDouble("lat", 0.0)
                    longitude = obj.optDouble("lng", 0.0)
                    addRoute(route)
                    computeTransitType()
                }
                stops.add(stop)
            }
            stops
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch stops", e)
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
            Log.w(TAG, "TriMet API key not configured")
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
            val json = parser.fetch(url) ?: return@withContext null
            val resultSet = json.getJSONObject("resultSet")

            val arrivalArr = resultSet.optJSONArray("arrival")
            val arrivalList = mutableListOf<Arrival>()
            if (arrivalArr != null) {
                for (i in 0 until arrivalArr.length()) {
                    val obj = arrivalArr.getJSONObject(i)
                    val arrival = Arrival().apply {
                        fullSign = obj.optString("fullSign", "")
                        shortSign = obj.optString("shortSign", "")
                        routeId = obj.optInt("route", 0)
                        status = obj.optString("status", "")
                        tripID = obj.optString("tripID", "")
                        blockID = obj.optInt("blockID", 0)
                        vehicleID = obj.optInt("vehicleID", 0)
                        feet = obj.optInt("feet", 0)
                        dir = obj.optInt("dir", 0)
                        val estimatedMs = obj.optLong("estimated", -1)
                        if (estimatedMs != -1L) {
                            estimatedMillis = estimatedMs
                            estimated = org.joda.time.DateTime(estimatedMs)
                        }
                        val scheduledMs = obj.optLong("scheduled", -1)
                        if (scheduledMs != -1L) {
                            scheduledMillis = scheduledMs
                            scheduled = org.joda.time.DateTime(scheduledMs)
                    }
                        }
                    arrivalList.add(arrival)
                        }
            }

            val result = ArrivalsResult().apply {
                arrivals = arrivalList
                isQueryError = false
            }

            // Parse block positions if requested
            if (showPosition) {
                val blockPosArr = resultSet.optJSONArray("blockPosition")
                if (blockPosArr != null) {
                    val blockPositions = mutableListOf<BlockPosition>()
                    for (i in 0 until blockPosArr.length()) {
                        val obj = blockPosArr.getJSONObject(i)
                        val bp = BlockPosition().apply {
                            id = obj.optInt("id", 0)
                            at = obj.optLong("at", 0)
                            vehicleID = obj.optInt("vehicleID", 0)
                            feet = obj.optInt("feet", 0)
                            heading = obj.optDouble("heading", 0.0).toFloat()
                            lat = obj.optDouble("lat", 0.0)
                            lng = obj.optDouble("lng", 0.0)
                            routeNumber = obj.optInt("routeNumber", 0)
                            direction = obj.optInt("direction", 0)
                            tripID = obj.optString("tripID", "")
                            isNewTrip = obj.optBoolean("newTrip", false)
                        }
                        blockPositions.add(bp)
                    }
                    result.blockPositions = blockPositions
                }
            }

            // Parse top-level detours
            val detourArr = resultSet.optJSONArray("detour")
            if (detourArr != null) {
                val detourList = mutableListOf<Detour>()
                for (i in 0 until detourArr.length()) {
                    val obj = detourArr.getJSONObject(i)
                    val detour = Detour().apply {
                        id = obj.optInt("id", 0)
                        desc = obj.optString("desc", "")
                        val routesArr = obj.optJSONArray("routes")
                        if (routesArr != null) {
                            routes = List(routesArr.length()) { k -> routesArr.optInt(k, 0) }
                        }
                    }
                    detourList.add(detour)
                }
                result.detours = detourList
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch arrivals", e)
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
            Log.w(TAG, "TriMet API key not configured")
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
            val json = parser.fetch(url) ?: return@withContext null
            val resultSet = json.getJSONObject("resultSet")
            val vehicleArr = resultSet.optJSONArray("vehicle")
            if (vehicleArr == null) return@withContext emptyList()

            val vehicles = mutableListOf<VehiclePosition>()
            for (i in 0 until vehicleArr.length()) {
                val obj = vehicleArr.getJSONObject(i)
                val vp = VehiclePosition().apply {
                    vehicleID = obj.optInt("vehicleID", 0)
                    type = obj.optString("type", "")
                    blockID = obj.optInt("blockID", 0)
                    latitude = obj.optDouble("latitude", 0.0)
                    longitude = obj.optDouble("longitude", 0.0)
                    bearing = obj.optDouble("bearing", 0.0).toFloat()
                    routeNumber = obj.optInt("routeNumber", 0)
                    direction = obj.optInt("direction", 0)
                    tripID = obj.optString("tripID", "")
                            isNewTrip = obj.optBoolean("newTrip", false)
                    delay = obj.optInt("delay", 0)
                    signMessage = obj.optString("signMessage", "")
                    signMessageLong = obj.optString("signMessageLong", "")
                    nextLocID = obj.optInt("nextLocID", 0)
                    nextStopSeq = obj.optInt("nextStopSeq", 0)
                    lastLocID = obj.optInt("lastLocID", 0)
                    lastStopSeq = obj.optInt("lastStopSeq", 0)
                    serviceDate = obj.optLong("serviceDate", 0)
                    locationInScheduleDay = obj.optInt("locationInScheduleDay", 0)
                    time = obj.optLong("time", 0)
                    expires = obj.optLong("expires", 0)
                            isInCongestion = obj.optBoolean("inCongestion", false)
                    loadPercentage = obj.optInt("loadPercentage", 0)
                    garage = obj.optString("garage", "")
                    extrablockID = obj.optString("extrablockID", "")
                            isOffRoute = obj.optBoolean("offRoute", false)
                }
                vehicles.add(vp)
            }
            vehicles
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch vehicles", e)
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
            Log.w(TAG, "TriMet API key not configured")
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
            val json = parser.fetch(url) ?: return@withContext null
            val resultSet = json.getJSONObject("resultSet")
            val locationArr = resultSet.optJSONArray("location")
            if (locationArr == null) return@withContext emptyList()

            val stops = mutableListOf<Stop>()
            for (i in 0 until locationArr.length()) {
                val obj = locationArr.getJSONObject(i)
                val stop = Stop().apply {
                    desc = obj.optString("desc", "")
                    dirDesc = obj.optString("dir", "")
                    locId = obj.optInt("locid", 0)
                    latitude = obj.optDouble("lat", 0.0)
                    longitude = obj.optDouble("lng", 0.0)
                    // Parse routes if showRoutes=true
                    val routeArr = obj.optJSONArray("route")
                    if (routeArr != null) {
                        for (j in 0 until routeArr.length()) {
                            val routeObj = routeArr.getJSONObject(j)
                            val route = Route().apply {
                                desc = routeObj.optString("desc", "")
                                routeId = routeObj.optInt("route", 0)
                                val type = routeObj.optString("type", "")
                                applyRouteType(type, desc)
                                applyStreetcarType(desc, type)
                            }
                            addRoute(route)
                        }
                    }
                    computeTransitType()
                }
                stops.add(stop)
            }
            stops
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch stops by location", e)
            null
        }
    }

    suspend fun fetchStopById(context: Context, locId: Int): Stop? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) return@withContext null
        try {
            val baseUrl = context.getString(R.string.base_stop_location_v2_url)
            val url = "$baseUrl/appID/$apiKey/locIds/$locId"
            val json = parser.fetch(url) ?: return@withContext null
            val resultSet = json.getJSONObject("resultSet")
            val locationArr = resultSet.optJSONArray("location")
            if (locationArr == null || locationArr.length() == 0) return@withContext null
            val obj = locationArr.getJSONObject(0)
            Stop().apply {
                desc = obj.optString("desc", "")
                dirDesc = obj.optString("dir", "")
                this.locId = obj.optInt("locid", 0)
                latitude = obj.optDouble("lat", 0.0)
                longitude = obj.optDouble("lng", 0.0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch stop by ID", e)
            null
        }
    }

    suspend fun fetchAlerts(
        context: Context,
        routeIds: List<Int>? = null,
        locIds: List<Int>? = null,
        systemWideOnly: Boolean = false
    ): List<Detour>? = withContext(Dispatchers.IO) {
        if (!ConnectionUtils.isOnline(context)) return@withContext null
        val apiKey = ApiKeys.getTrimetApiKey()
        if (apiKey.isBlank()) {
            Log.w(TAG, "TriMet API key not configured")
            return@withContext null
        }
        try {
            val baseUrl = context.getString(R.string.base_alerts_url)
            val url = buildString {
                append(baseUrl)
                append("/appID/").append(apiKey)
                if (routeIds != null && routeIds.isNotEmpty()) {
                    append("/routes/").append(routeIds.joinToString(","))
                }
                if (locIds != null && locIds.isNotEmpty()) {
                    append("/locIDs/").append(locIds.joinToString(","))
                }
                if (systemWideOnly) append("/systemWideOnly/true")
            }
            val json = parser.fetch(url) ?: return@withContext null
            val resultSet = json.getJSONObject("resultSet")
            val detourArr = resultSet.optJSONArray("detour")
            if (detourArr == null) return@withContext emptyList()

            val detours = mutableListOf<Detour>()
            for (i in 0 until detourArr.length()) {
                val obj = detourArr.getJSONObject(i)
                val detour = Detour().apply {
                    id = obj.optInt("id", 0)
                    desc = obj.optString("desc", "")
                    val routesArr = obj.optJSONArray("routes")
                    if (routesArr != null) {
                        routes = List(routesArr.length()) { k -> routesArr.optInt(k, 0) }
                    }
                }
                detours.add(detour)
            }
            detours
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch alerts", e)
            null
        }
    }
}
