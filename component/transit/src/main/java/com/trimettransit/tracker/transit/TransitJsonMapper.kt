package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.BlockPosition
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.StopsWithArrivals
import com.trimettransit.tracker.model.TransitAlert
import com.trimettransit.tracker.model.Vehicle
import com.trimettransit.tracker.model.VehicleResult
import com.trimettransit.tracker.model.BlockStatus
import com.trimettransit.tracker.model.BlockStatusResult
import com.trimettransit.tracker.model.TripStatus
import com.trimettransit.tracker.model.TripStatusResult
import com.trimettransit.tracker.model.TripStopStatus
import com.trimettransit.tracker.model.TripStopStatusInfo
import com.trimettransit.tracker.model.computeTransitType
import com.trimettransit.tracker.model.domain.dedupeArrivals
import org.joda.time.DateTime
import org.json.JSONObject

/**
 * Pure JSON→model mapping for TriMet's JSON endpoints. Keeps [TransitApi] as a thin
 * network shell (online guard, API key, URL build, OkHttp fetch, error handling) and
 * makes the parsing logic directly unit-testable.
 */
internal object TransitJsonMapper {

    private fun JSONObject.optionalLong(name: String): Long? =
        if (has(name) && !isNull(name)) optLong(name) else null

    private fun JSONObject.optionalInt(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private fun JSONObject.optionalDouble(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name) else null

    private fun parseTripStop(obj: JSONObject, location: JSONObject?): TripStopStatusInfo =
        TripStopStatusInfo(
            locId = obj.optInt("locid", 0),
            stopSequence = obj.optInt("stopSequence", 0),
            description = location?.optString("desc", "").orEmpty(),
            direction = location?.optString("dir", "").orEmpty(),
            isPassed = obj.optBoolean("isPassed", false),
            passengerAccessible = obj.optBoolean("passengerAccessible", true),
            distanceFeet = obj.optDouble("distance", 0.0),
            aimedArrivalMillis = obj.optLong("aimedArrival", 0L),
            aimedDepartureMillis = obj.optLong("aimedDeparture", 0L),
            arrivalDelaySeconds = obj.optionalInt("arrivalDelay"),
            departureDelaySeconds = obj.optionalInt("departureDelay"),
            estimated = if (obj.has("estimated") && !obj.isNull("estimated")) {
                obj.optBoolean("estimated")
            } else {
                null
            },
            adjustedArrivalMillis = obj.optionalLong("adjustedArrival"),
            adjustedDepartureMillis = obj.optionalLong("adjustedDeparture"),
            status = TripStopStatus.fromApiValue(obj.optString("status", "normal"))
        )

    private fun parseTrip(obj: JSONObject, locations: Map<Int, JSONObject>): TripStatus {
        val stops = obj.optJSONArray("stop")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val stop = array.optJSONObject(index) ?: continue
                    add(parseTripStop(stop, locations[stop.optInt("locid", 0)]))
                }
            }
        }.orEmpty()
        return TripStatus(
            tripId = obj.optString("tripID", ""),
            blockId = obj.optInt("blockID", 0),
            routeNumber = obj.optInt("routeNumber", 0),
            distanceFeet = obj.optDouble("distance", 0.0),
            progressFeet = obj.optionalDouble("progressFeet"),
            pattern = obj.optInt("pattern", 0),
            destination = obj.optString("destination", ""),
            blockSchedulePositionSeconds = obj.optionalInt("blocksSchedulePosition"),
            extra = obj.optBoolean("extraTrip", false),
            tripBeginMillis = obj.optLong("tripBeginTime", 0L),
            tripEndMillis = obj.optLong("tripEndTime", 0L),
            scheduleDayMillis = obj.optLong("scheduleDay", 0L),
            modified = obj.optBoolean("modified", false),
            direction = obj.optInt("direction", 0),
            stops = stops
        )
    }

    private fun tripArray(obj: JSONObject): List<JSONObject> {
        val array = obj.optJSONArray("trip")
        if (array != null) {
            return buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(::add)
                }
            }
        }
        return obj.optJSONObject("trip")?.let(::listOf).orEmpty()
    }

    private fun objectArray(parent: JSONObject, name: String): List<JSONObject> {
        val array = parent.optJSONArray(name)
        if (array != null) {
            return buildList {
                for (index in 0 until array.length()) {
                    array.optJSONObject(index)?.let(::add)
                }
            }
        }
        return parent.optJSONObject(name)?.let(::listOf).orEmpty()
    }

    fun parseTripStatus(resultSet: JSONObject): TripStatusResult {
        val locations = buildMap {
            val array = resultSet.optJSONArray("location") ?: return@buildMap
            for (index in 0 until array.length()) {
                val location = array.optJSONObject(index) ?: continue
                put(location.optInt("locid", 0), location)
            }
        }
        val trips = objectArray(resultSet, "trip").map { parseTrip(it, locations) }
        return TripStatusResult(
            queryTimeMillis = resultSet.optLong("queryTime", 0L),
            trips = trips
        )
    }

    fun parseBlockStatus(resultSet: JSONObject): BlockStatusResult {
        val locations = buildMap {
            val array = resultSet.optJSONArray("location") ?: return@buildMap
            for (index in 0 until array.length()) {
                val location = array.optJSONObject(index) ?: continue
                put(location.optInt("locid", 0), location)
            }
        }
        val blocks = objectArray(resultSet, "blockStatus").map { obj ->
            BlockStatus(
                blockId = obj.optInt("blockID", 0),
                currentTripId = obj.optString("currentTripID").takeIf { it.isNotEmpty() },
                latitude = obj.optionalDouble("lat"),
                longitude = obj.optionalDouble("lng"),
                bearing = obj.optionalDouble("bearing")?.toFloat(),
                positionTimestampMillis = obj.optionalLong("positionTimestamp"),
                deviationSeconds = obj.optionalInt("deviation"),
                vehicleId = obj.optionalInt("vehicleID"),
                schedulePositionMillis = obj.optionalLong("schedulePosition"),
                trips = tripArray(obj).map { parseTrip(it, locations) }
            )
        }
        return BlockStatusResult(
            queryTimeMillis = resultSet.optLong("queryTime", 0L),
            blocks = blocks
        )
    }

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

    /**
     * Maps one raw arrival JSON element to [Arrival]. Shared by [parseArrivals] (root
     * "arrival" arrays, where each element carries its own "locid") and
     * [parseStopsWithArrivals] (arrivals embedded per stop location, where "locid" may
     * be implicit from the parent — [defaultLocId] fills that gap).
     */
    private fun parseArrivalObject(obj: JSONObject, defaultLocId: Int = 0): Arrival {
        val estimatedMs = obj.optLong("estimated", -1)
        val scheduledMs = obj.optLong("scheduled", -1)
        return Arrival(
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
            scheduledMillis = if (scheduledMs != -1L) scheduledMs else 0L,
            locId = obj.optInt("locid", defaultLocId)
        )
    }

    private fun parseBlockPosition(bpObj: JSONObject): BlockPosition = BlockPosition(
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

    fun parseArrivals(resultSet: JSONObject): ArrivalsResult {
        val arrivalArr = resultSet.optJSONArray("arrival")
        val arrivalList = mutableListOf<Arrival>()
        val parsedBlockPositions = mutableListOf<BlockPosition>()
        if (arrivalArr != null) {
            for (i in 0 until arrivalArr.length()) {
                // One malformed element must not void the whole payload: skip it and
                // keep the remaining arrivals (fail-open per element, fail-closed overall).
                val obj = arrivalArr.optJSONObject(i) ?: continue
                arrivalList.add(parseArrivalObject(obj))
                // TriMet returns each block's live position nested inside its arrival object
                // (only when showPosition/true is requested)
                val bpObj = obj.optJSONObject("blockPosition")
                if (bpObj != null) {
                    parsedBlockPositions.add(parseBlockPosition(bpObj))
                }
            }
        }

        // Parse top-level detours
        val detourArr = resultSet.optJSONArray("detour")
        var detours = emptyList<Detour>()
        if (detourArr != null) {
            val detourList = mutableListOf<Detour>()
            for (i in 0 until detourArr.length()) {
                val obj = detourArr.optJSONObject(i) ?: continue
                val routesArr = obj.optJSONArray("route")
                    ?.takeIf { it.length() > 0 }
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

        // Parse location elements for stop coordinates; ignore malformed or
        // out-of-range entries and keep the 0,0 default instead.
        var stopLat = 0.0
        var stopLng = 0.0
        val locationArr = resultSet.optJSONArray("location")
        if (locationArr != null && locationArr.length() > 0) {
            val loc = locationArr.optJSONObject(0)
            if (loc != null) {
                val lat = loc.optDouble("lat", 0.0)
                val lng = loc.optDouble("lng", 0.0)
                if (isValidCoordinate(lat, lng)) {
                    stopLat = lat
                    stopLng = lng
                }
            }
        }

        val deduped = dedupeArrivals(arrivalList)
        return ArrivalsResult(
            arrivals = deduped,
            blockPositions = parsedBlockPositions,
            detours = detours,
            stopLat = stopLat,
            stopLng = stopLng
        )
    }

    fun parseStopsByLocation(resultSet: JSONObject): List<Stop> {
        val locationArr = resultSet.optJSONArray("location")
        if (locationArr == null) return emptyList()

        val stops = mutableListOf<Stop>()
        for (i in 0 until locationArr.length()) {
            val obj = locationArr.optJSONObject(i) ?: continue
            val locId = obj.optInt("locid", 0)
            if (locId == 0) continue
            val latitude = obj.optDouble("lat", 0.0)
            val longitude = obj.optDouble("lng", 0.0)
            if (!isValidCoordinate(latitude, longitude)) continue
            val routeArr = obj.optJSONArray("route") ?: obj.optJSONArray("routes")
            val routes = if (routeArr != null) {
                buildList {
                    for (j in 0 until routeArr.length()) {
                        val routeObj = routeArr.optJSONObject(j) ?: continue
                        add(parseRoute(routeObj))
                    }
                }
            } else {
                emptyList()
            }
            stops.add(
                Stop(
                    desc = obj.optString("desc", ""),
                    dirDesc = obj.optString("dir", ""),
                    latitude = latitude,
                    longitude = longitude,
                    distanceFeet = obj.optDouble("distance", 0.0),
                    transitType = computeTransitType(routes),
                    locId = locId,
                    routes = routes
                )
            )
        }
        // Nearest first, with anything that reported no distance last. The sort is stable, so
        // stops that tie (including all the unknowns) keep the order the API sent them in.
        return stops.sortedBy { if (it.distanceFeet > 0.0) it.distanceFeet else Double.MAX_VALUE }
    }

    /**
     * Combined stops+arrivals parse for the nearby-stops fast path: [parseStopsByLocation]
     * for the stop list, plus each stop's already-embedded arrivals so the caller can skip
     * a second per-stop `/arrivals` round trip. TriMet's combined response can carry
     * arrivals either as a flat, "locid"-tagged root "arrival" array (the same shape
     * [parseArrivals] reads) or nested per "location" element — both are read and merged
     * so either response shape works.
     */
    fun parseStopsWithArrivals(resultSet: JSONObject): StopsWithArrivals {
        val stops = parseStopsByLocation(resultSet)
        val arrivalsByStop = mutableMapOf<Int, MutableList<Arrival>>()

        val rootArr = resultSet.optJSONArray("arrival")
        if (rootArr != null) {
            for (i in 0 until rootArr.length()) {
                val obj = rootArr.optJSONObject(i) ?: continue
                val arrival = parseArrivalObject(obj)
                if (arrival.locId == 0) continue
                arrivalsByStop.getOrPut(arrival.locId) { mutableListOf() }.add(arrival)
            }
        }

        val locationArr = resultSet.optJSONArray("location")
        if (locationArr != null) {
            for (i in 0 until locationArr.length()) {
                val locObj = locationArr.optJSONObject(i) ?: continue
                val locId = locObj.optInt("locid", 0)
                if (locId == 0) continue
                val embeddedArr = locObj.optJSONArray("arrival") ?: continue
                for (j in 0 until embeddedArr.length()) {
                    val obj = embeddedArr.optJSONObject(j) ?: continue
                    arrivalsByStop.getOrPut(locId) { mutableListOf() }
                        .add(parseArrivalObject(obj, defaultLocId = locId))
                }
            }
        }

        val deduped = arrivalsByStop.mapValues { (_, list) -> dedupeArrivals(list) }
        return StopsWithArrivals(stops = stops, arrivalsByStop = deduped)
    }

    fun parseStopById(resultSet: JSONObject): Stop? {
        val locationArr = resultSet.optJSONArray("location")
        if (locationArr == null || locationArr.length() == 0) return null
        val obj = locationArr.optJSONObject(0) ?: return null
        val locId = obj.optInt("locid", 0)
        if (locId == 0) return null
        val latitude = obj.optDouble("lat", 0.0)
        val longitude = obj.optDouble("lng", 0.0)
        if (!isValidCoordinate(latitude, longitude)) return null
        val routeArr = obj.optJSONArray("route") ?: obj.optJSONArray("routes")
        val routes = if (routeArr != null) {
            buildList {
                for (j in 0 until routeArr.length()) {
                    val routeObj = routeArr.optJSONObject(j) ?: continue
                    add(parseRoute(routeObj))
                }
            }
        } else {
            emptyList()
        }
        return Stop(
            desc = obj.optString("desc", ""),
            dirDesc = obj.optString("dir", ""),
            latitude = latitude,
            longitude = longitude,
            transitType = computeTransitType(routes),
            locId = locId,
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
            val routeObj = routeArr.optJSONObject(ri) ?: continue
            val dirArr = routeObj.optJSONArray("dir") ?: continue
            val routeNum = routeObj.optInt("route", 0)
            val route = parseRoute(routeObj)
            for (di in 0 until dirArr.length()) {
                val dirObj = dirArr.optJSONObject(di) ?: continue
                val stopArr = dirObj.optJSONArray("stop") ?: continue
                val dirDesc = dirObj.optString("desc", "")
                for (si in 0 until stopArr.length()) {
                    val obj = stopArr.optJSONObject(si) ?: continue
                    val locId = obj.optInt("locid", 0)
                    // A zero/missing locid is unaddressable; skip it rather than collapse
                    // multiple malformed rows onto key 0 and merge their route lists.
                    if (locId == 0) continue
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

    /**
     * Parses the Alerts V2 feed (`ws/v2/alerts`). Each alert's routes ship under
     * `"route"` (singular) with a `"routes"` fallback, mirroring the arrivals feed's
     * embedded-detour quirk. A per-element parse failure is skipped rather than
     * voiding the whole response.
     */
    fun parseAlerts(resultSet: JSONObject): List<TransitAlert> {
        val alertArr = resultSet.optJSONArray("alert") ?: return emptyList()
        val alerts = mutableListOf<TransitAlert>()
        for (i in 0 until alertArr.length()) {
            val obj = alertArr.optJSONObject(i) ?: continue
            val routesArr = obj.optJSONArray("route")
                ?.takeIf { it.length() > 0 }
                ?: obj.optJSONArray("routes")
            val routeIds = if (routesArr != null) {
                MutableList(routesArr.length()) { k ->
                    when (val el = routesArr.opt(k)) {
                        is JSONObject -> el.optInt("route", 0)
                        else -> routesArr.optInt(k, 0)
                    }
                }
            } else {
                emptyList()
            }
            alerts.add(
                TransitAlert(
                    id = obj.optInt("id", 0),
                    header = obj.optString("header_text", ""),
                    desc = obj.optString("desc", ""),
                    infoLinkUrl = obj.optString("info_link_url", "").takeIf { it.isNotBlank() },
                    systemWide = obj.optBoolean("system_wide_flag", false),
                    routeIds = routeIds
                )
            )
        }
        return alerts
    }

    /**
     * Parses `/ws/v2/vehicles` payloads. A per-element parse failure is skipped
     * rather than voiding the whole fleet; an empty/missing "vehicle" array still
     * yields a non-null [VehicleResult] with an empty list (as opposed to no
     * response at all, which the caller surfaces as null).
     */
    fun parseVehicles(resultSet: JSONObject): VehicleResult {
        val queryTime = resultSet.optLong("queryTime", 0)
        val arr = resultSet.optJSONArray("vehicle")
        if (arr == null) return VehicleResult(emptyList(), queryTime)
        val vehicles = mutableListOf<Vehicle>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            vehicles.add(
                Vehicle(
                    vehicleID = obj.optInt("vehicleID", 0),
                    type = obj.optString("type", ""),
                    blockID = obj.optInt("blockID", 0),
                    tripID = obj.optString("tripID", ""),
                    routeNumber = obj.optInt("routeNumber", 0),
                    direction = obj.optInt("direction", 0),
                    newTrip = obj.optBoolean("newTrip", false),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    bearing = obj.optDouble("bearing", 0.0).toFloat(),
                    time = obj.optLong("time", 0),
                    expires = obj.optLong("expires", 0),
                    serviceDate = obj.optLong("serviceDate", 0),
                    locationInScheduleDay = obj.optInt("locationInScheduleDay", 0),
                    delay = obj.optInt("delay", 0),
                    signMessage = obj.optString("signMessage", ""),
                    nextLocID = obj.optInt("nextLocID", 0),
                    lastLocID = obj.optInt("lastLocID", 0),
                    garage = obj.optString("garage", ""),
                    inCongestion = obj.optBoolean("inCongestion", false),
                    loadPercentage = obj.optInt("loadPercentage", -1)
                )
            )
        }
        return VehicleResult(vehicles, queryTime)
    }
}
