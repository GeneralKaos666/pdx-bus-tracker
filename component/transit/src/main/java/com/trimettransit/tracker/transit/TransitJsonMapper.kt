package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.BlockPosition
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.VehiclePosition
import com.trimettransit.tracker.model.computeTransitType
import org.joda.time.DateTime
import org.json.JSONObject

/**
 * Pure JSON→model mapping for TriMet's JSON endpoints. Keeps [TransitApi] as a thin
 * network shell (online guard, API key, URL build, OkHttp fetch, error handling) and
 * makes the parsing logic directly unit-testable.
 */
object TransitJsonMapper {

    fun parseRoute(desc: String, routeId: Int, type: String): Route {
        return Route(
            desc = desc,
            routeId = routeId,
            isBus = type == "B",
            isMax = type == "R" && desc.contains("MAX"),
            isStreetcar = desc.contains("Portland Streetcar") && type == "R",
            isWes = type == "R" && desc.contains("WES")
        )
    }

    fun parseRoute(obj: JSONObject): Route {
        val desc = obj.optString("desc", "")
        val routeId = obj.optInt("route", 0)
        val type = obj.optString("type", "")
        return parseRoute(desc, routeId, type)
    }

    /** TriMet serves coordinates in the Portland metro area; anything else is malformed/absent data. */
    fun isValidCoordinate(latitude: Double, longitude: Double): Boolean =
        latitude in -90.0..90.0 && longitude in -180.0..180.0 &&
            !(latitude == 0.0 && longitude == 0.0)

    fun parseArrivals(resultSet: JSONObject): ArrivalsResult {
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
                    estimated = if (estimatedMs != -1L) DateTime(estimatedMs) else null,
                    scheduled = if (scheduledMs != -1L) DateTime(scheduledMs) else null,
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

        return ArrivalsResult(
            arrivals = arrivalList,
            blockPositions = parsedBlockPositions,
            detours = detours,
            stopLat = stopLat,
            stopLng = stopLng
        )
    }

    fun parseVehicles(resultSet: JSONObject): List<VehiclePosition> {
        val vehicleArr = resultSet.optJSONArray("vehicle")
        if (vehicleArr == null) return emptyList()

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
                extraBlockID = obj.optString("extrablockID", ""),
                isOffRoute = obj.optBoolean("offRoute", false)
            )
            vehicles.add(vp)
        }
        return vehicles
    }

    fun parseStopsByLocation(resultSet: JSONObject): List<Stop> {
        val locationArr = resultSet.optJSONArray("location")
        if (locationArr == null) return emptyList()

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
        return stops
    }

    fun parseStopById(resultSet: JSONObject): Stop? {
        val locationArr = resultSet.optJSONArray("location")
        if (locationArr == null || locationArr.length() == 0) return null
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
        return Stop(
            desc = obj.optString("desc", ""),
            dirDesc = obj.optString("dir", ""),
            latitude = obj.optDouble("lat", 0.0),
            longitude = obj.optDouble("lng", 0.0),
            transitType = computeTransitType(routes),
            locId = obj.optInt("locid", 0),
            routes = routes
        )
    }

    /**
     * Accumulates a mutable description of each stop, then builds immutable Stops.
     * Returns null when the payload has no route listing at all (the caller treats
     * that as a failed request), and an empty list when routes yield no stops.
     */
    fun parseSearchStops(resultSet: JSONObject): List<Stop>? {
        data class StopBuilder(
            var desc: String = "",
            var dirDesc: String = "",
            var latitude: Double = 0.0,
            var longitude: Double = 0.0,
            var routeNum: Int = 0,
            var locId: Int = 0,
            var routes: MutableList<Route> = mutableListOf()
        )

        val routeArr = resultSet.optJSONArray("route") ?: return null

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
                        val lat = obj.optDouble("lat", 0.0)
                        val lng = obj.optDouble("lng", obj.optDouble("lon", 0.0))
                        if (!isValidCoordinate(lat, lng)) continue
                        buildersById[locId] = StopBuilder(
                            desc = obj.optString("desc", ""),
                            dirDesc = if (stopDir == "") dirDesc else stopDir,
                            latitude = lat,
                            longitude = lng,
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
        return buildersById.values
            .map { b ->
                val primaryRoute = b.routes.minByOrNull { it.routeId } ?: Route(
                    desc = b.desc, routeId = b.routeNum, isBus = true, isMax = false,
                    isStreetcar = false, isWes = false
                )
                Stop(
                    desc = b.desc,
                    dirDesc = b.dirDesc,
                    latitude = b.latitude,
                    longitude = b.longitude,
                    transitType = computeTransitType(b.routes),
                    routeNum = primaryRoute.routeId,
                    locId = b.locId,
                    routes = b.routes
                )
            }
    }
}