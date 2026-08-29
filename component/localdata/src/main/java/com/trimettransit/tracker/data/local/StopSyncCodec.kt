package com.trimettransit.tracker.data.local

import com.trimettransit.tracker.model.Stop
import org.json.JSONArray
import org.json.JSONObject

private const val F_DESC = "desc"
private const val F_DIR = "dir_desc"
private const val F_TYPE = "transit_type"
private const val F_LOC = "loc_id"
private const val F_LON = "longitude"
private const val F_LAT = "latitude"
private const val F_ROUTE = "route_num"

/** Wearable Data Layer contract shared by the phone (publisher) and watch (consumer). */
object StopSyncContract {
    const val PATH_FAVORITES = "/stops/favorites"
    const val PATH_RECENT = "/stops/recent"
    const val PATH_ROOT = "/stops"
    const val KEY_STOPS = "stops"
}

/**
 * Compact, dependency-free serialization shared by both ends of the phone<->watch
 * sync: the phone app publishes it as Wearable Data Layer payloads; the watch app
 * parses the same format into its own local database.
 */
fun List<Stop>.toStopSyncJson(): String {
    val arr = JSONArray()
    forEach { stop ->
        arr.put(
            JSONObject()
                .put(F_DESC, stop.desc ?: "")
                .put(F_DIR, stop.dirDesc ?: "")
                .put(F_TYPE, stop.transitType ?: "")
                .put(F_LOC, stop.locId)
                .put(F_LON, stop.longitude)
                .put(F_LAT, stop.latitude)
                .put(F_ROUTE, stop.routeNum)
        )
    }
    return arr.toString()
}

fun String.parseStopSyncJson(): List<Stop> = runCatching {
    val arr = JSONArray(this)
    List(arr.length()) { i ->
        val o = arr.getJSONObject(i)
        Stop(
            desc = o.optString(F_DESC),
            dirDesc = o.optString(F_DIR),
            transitType = o.optString(F_TYPE),
            locId = o.optInt(F_LOC),
            longitude = o.optDouble(F_LON),
            latitude = o.optDouble(F_LAT),
            routeNum = o.optInt(F_ROUTE)
        )
    }
}.getOrElse { emptyList() }