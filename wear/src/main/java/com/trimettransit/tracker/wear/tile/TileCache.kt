package com.trimettransit.tracker.wear.tile

import android.content.Context
import androidx.core.content.edit
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.Stop
import org.json.JSONArray
import org.json.JSONObject

/** Intent-extra keys the Tile uses to deep-link into a specific stop's arrivals. */
object TileExtras {
    const val KEY_LOC_ID = "tile_loc_id"
    const val KEY_NAME = "tile_name"
    const val KEY_ROUTE = "tile_route"
}

/**
 * Tiny SharedPreferences cache that hands the stand-alone "Next departure" tile
 * snapshot data without running network calls or touching SQLite on the system's
 * tile render thread (onTileRequest must return fast).
 */
object TileCache {
    private const val PREF_NAME = "tile_cache"
    private const val KEY_STOP = "tile_stop"
    private const val KEY_ARRIVALS = "tile_arrivals"
    private const val KEY_UPDATED = "tile_updated_ms"
    private const val KEY_FEATURED_LOCID = "tile_featured_locid"

    fun isFeatured(context: Context, locId: Int): Boolean =
        prefs(context).getInt(KEY_FEATURED_LOCID, -1) == locId

    /** Refreshes the shared cache when [stop] is the stop the tile features; true if updated. */
    fun updateIfFeatured(context: Context, stop: Stop, arrivals: List<Arrival>): Boolean {
        if (!isFeatured(context, stop.locId)) return false
        update(context, stop, arrivals)
        return true
    }

    fun update(context: Context, stop: Stop, arrivals: List<Arrival>) {
        prefs(context).edit {
            putString(KEY_STOP, stopToJson(stop))
            putString(KEY_ARRIVALS, arrivalsToJson(arrivals))
            putLong(KEY_UPDATED, System.currentTimeMillis())
            putInt(KEY_FEATURED_LOCID, stop.locId)
        }
    }

    fun snapshot(context: Context): Snapshot? {
        val p = prefs(context)
        val stopJson = p.getString(KEY_STOP, null) ?: return null
        return Snapshot(
            stop = stopFromJson(stopJson),
            arrivals = arrivalsFromJson(p.getString(KEY_ARRIVALS, "[]").orEmpty()),
            updatedAtMillis = p.getLong(KEY_UPDATED, 0L)
        )
    }

    data class Snapshot(
        val stop: Stop,
        val arrivals: List<TileArrival>,
        val updatedAtMillis: Long
    )

    data class TileArrival(val sign: String, val arrivalTimeMillis: Long) {
        /** Whole minutes until this arrival relative to [now]; 0 means "now/due". */
        fun minutesFrom(nowMillis: Long): Long {
            val remaining = arrivalTimeMillis - nowMillis
            return if (remaining <= 0) 0L else (remaining + 59_999) / 60_000
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun stopToJson(stop: Stop) = JSONObject()
        .put("desc", stop.desc)
        .put("locId", stop.locId)
        .put("route", stop.routeNum)
        .toString()

    private fun stopFromJson(json: String): Stop {
        val o = JSONObject(json)
        return Stop(
            desc = o.optString("desc", ""),
            locId = o.optInt("locId", 0),
            routeNum = o.optInt("route", 0)
        )
    }

    private fun arrivalsToJson(arrivals: List<Arrival>): String {
        val arr = JSONArray()
        for (a in arrivals) {
            val at = a.estimated?.millis ?: a.scheduled?.millis ?: continue
            arr.put(
                JSONObject()
                    .put("sign", a.shortSign.ifBlank { a.fullSign })
                    .put("at", at)
            )
        }
        return arr.toString()
    }

    private fun arrivalsFromJson(json: String): List<TileArrival> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            TileArrival(
                sign = o.optString("sign", ""),
                arrivalTimeMillis = o.optLong("at", 0L)
            )
        }
    }
}