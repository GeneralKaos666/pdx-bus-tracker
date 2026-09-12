package com.trimettransit.tracker.transit

import android.content.Context
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import org.json.JSONArray
import org.json.JSONObject
import java.io.FileNotFoundException

/**
 * Serializes the full-network stop dump to and from JSON using the platform's
 * org.json classes. Pure so round-trip fidelity is unit-testable; [StopSearchStore]
 * handles the file I/O around it.
 */
internal fun serializeStops(stops: List<Stop>): String {
    val arr = JSONArray()
    for (stop in stops) {
        val routes = JSONArray()
        for (route in stop.routes) {
            routes.put(
                JSONObject()
                    .put("desc", route.desc)
                    .put("routeId", route.routeId)
                    .put("isBus", route.isBus)
                    .put("isMax", route.isMax)
                    .put("isStreetcar", route.isStreetcar)
                    .put("isWes", route.isWes)
            )
        }
        arr.put(
            JSONObject()
                .put("desc", stop.desc)
                .put("dirDesc", stop.dirDesc)
                .put("latitude", stop.latitude)
                .put("longitude", stop.longitude)
                .put("transitType", stop.transitType)
                .put("routeNum", stop.routeNum)
                .put("locId", stop.locId)
                .put("routes", routes)
        )
    }
    return arr.toString()
}

/** Null when the payload is not a well-formed stop dump (corrupt cache → treat as no cache). */
internal fun deserializeStops(json: String): List<Stop>? {
    val arr = try {
        JSONArray(json)
    } catch (e: Exception) {
        return null
    }
    return buildList(arr.length()) {
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: return null
            val routesArr = obj.optJSONArray("routes")
            val routes = if (routesArr == null) {
                emptyList()
            } else {
                buildList(routesArr.length()) {
                    for (j in 0 until routesArr.length()) {
                        val r = routesArr.optJSONObject(j) ?: return null
                        add(
                            Route(
                                desc = r.optString("desc", ""),
                                routeId = r.optInt("routeId", 0),
                                isBus = r.optBoolean("isBus", false),
                                isMax = r.optBoolean("isMax", false),
                                isStreetcar = r.optBoolean("isStreetcar", false),
                                isWes = r.optBoolean("isWes", false)
                            )
                        )
                    }
                }
            }
            add(
                Stop(
                    desc = obj.optString("desc", ""),
                    dirDesc = obj.optString("dirDesc", ""),
                    latitude = obj.optDouble("latitude", 0.0),
                    longitude = obj.optDouble("longitude", 0.0),
                    transitType = obj.optString("transitType", ""),
                    routeNum = obj.optInt("routeNum", 0),
                    locId = obj.optInt("locId", 0),
                    routes = routes
                )
            )
        }
    }
}

/** Persists and restores the stop-search dump in app-internal storage. */
internal object StopSearchStore {
    private const val FILE_NAME = "all_stops_cache.json"

    /** Writes the dump; never overwrites a good cache with an empty one. */
    fun write(context: Context, stops: List<Stop>) {
        if (stops.isEmpty()) return
        context.openFileOutput(FILE_NAME, Context.MODE_PRIVATE).use {
            it.write(serializeStops(stops).toByteArray(Charsets.UTF_8))
        }
    }

    /** Returns the cached dump, or null when there is none (or it is corrupt). */
    fun read(context: Context): List<Stop>? {
        val raw = try {
            context.openFileInput(FILE_NAME).reader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            return null
        } catch (e: Exception) {
            return null
        }
        return deserializeStops(raw)
    }
}