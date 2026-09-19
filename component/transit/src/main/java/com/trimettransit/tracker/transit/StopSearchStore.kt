package com.trimettransit.tracker.transit

import android.content.Context
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
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

/** Disk-cache TTL for the stop-search dump: a day-old dump is worse than no dump. */
internal const val STOP_CACHE_MAX_AGE_MILLIS = 24L * 60 * 60 * 1000

/**
 * Wraps the bare stop array with its write timestamp so [StopSearchStore.read] can
 * enforce [STOP_CACHE_MAX_AGE_MILLIS]. String-concatenated (not re-parsed) to avoid
 * doubling the multi-MB peak during writes.
 */
internal fun serializeWithTtl(stops: List<Stop>, updatedAt: Long = System.currentTimeMillis()): String =
    "{\"updatedAt\":" + updatedAt + ",\"stops\":" + serializeStops(stops) + "}"

/**
 * TTL-guarded counterpart to [deserializeStops]: stale payloads read as null (no
 * cache) so callers fall through to a fresh network fetch. Pure for unit-testing.
 */
internal fun deserializeWithTtl(
    json: String,
    updatedAt: Long,
    maxAgeMillis: Long = STOP_CACHE_MAX_AGE_MILLIS
): List<Stop>? {
    if (System.currentTimeMillis() - updatedAt > maxAgeMillis) return null
    return deserializeStops(json)
}

/** Persists and restores the stop-search dump in app-internal storage. */
internal object StopSearchStore {
    private const val FILE_NAME = "all_stops_cache.json"

    /**
     * Writes the dump to a temp file then atomically renames it into place, so a
     * kill mid-write can never leave a truncated cache (read treats corrupt payloads
     * as "no cache", but avoiding them entirely keeps the fallback fast).
     */
    fun write(context: Context, stops: List<Stop>, updatedAt: Long = System.currentTimeMillis()) {
        if (stops.isEmpty()) return
        val payload = serializeWithTtl(stops, updatedAt).toByteArray(Charsets.UTF_8)
        val target = File(context.filesDir, FILE_NAME)
        val tmp = File(context.filesDir, "${FILE_NAME}.tmp")
        try {
            tmp.outputStream().use { it.write(payload) }
            if (!tmp.renameTo(target)) {
                target.outputStream().use { it.write(payload) }
            }
        } finally {
            tmp.delete()
        }
    }

    /**
     * Returns the cached dump, or null when there is none, it is corrupt, or it is
     * older than [maxAgeMillis]. Understands both the current `{updatedAt, stops}`
     * envelope and legacy bare-array dumps (TTL from the file mtime).
     */
    fun read(context: Context, maxAgeMillis: Long = STOP_CACHE_MAX_AGE_MILLIS): List<Stop>? {
        val raw = try {
            context.openFileInput(FILE_NAME).reader().use { it.readText() }
        } catch (e: FileNotFoundException) {
            return null
        } catch (e: Exception) {
            return null
        }
        val firstContent = raw.indexOfFirst { !it.isWhitespace() }.let { if (it < 0) return null else raw[it] }
        if (firstContent == '{') {
            val envelope = try {
                JSONObject(raw)
            } catch (e: Exception) {
                return null
            }
            if (!envelope.has("stops")) return null
            val inner = try {
                envelope.getJSONArray("stops").toString()
            } catch (e: Exception) {
                return null
            }
            val updatedAt = envelope.optLong("updatedAt", -1L)
            // No timestamp (hand-written cache?) → fail open and serve it.
            if (updatedAt < 0) return deserializeStops(inner)
            return deserializeWithTtl(inner, updatedAt, maxAgeMillis)
        }
        // Legacy bare-array dump: TTL from the file mtime so ancient dumps expire.
        val updatedAt = try {
            File(context.filesDir, FILE_NAME).lastModified()
        } catch (e: Exception) {
            0L
        }
        if (updatedAt > 0) return deserializeWithTtl(raw, updatedAt, maxAgeMillis)
        return deserializeStops(raw)
    }
}