package com.trimettransit.tracker.widget

import android.content.Context
import androidx.core.content.edit
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.util.minutesUntil
import org.json.JSONArray
import org.json.JSONObject

/**
 * SharedPreferences snapshot backing the "Next arrivals" home-screen widget. The widget
 * provider runs on the launcher's render thread and must return fast, so this cache hands
 * it pre-fetched arrival data without network calls or SQLite (mirrors the Wear tile's
 * [com.trimettransit.tracker.wear.tile.TileCache]). Grouping stays per stop: each row
 * carries its own arrivals, refreshed one request per stop by [WidgetRefreshWorker].
 */
object WidgetSnapshotCache {
    private const val PREF_NAME = "widget_cache"
    private const val KEY_JSON = "widget_snapshot"

    /** Per-stop widget row, ready to render. */
    data class ArrivalOnScreen(
        val sign: String,
        val atMillis: Long,
        val dropOffOnly: Boolean = false,
        val status: String = ""
    )

    /** Detour alert attached to a stop row, deduped by [id]. */
    data class WidgetDetour(
        val id: Int = 0,
        val desc: String = "",
        val routeIds: List<Int> = emptyList()
    )

    data class Row(
        val stop: Stop,
        val arrivals: List<ArrivalOnScreen>,
        val detours: List<WidgetDetour> = emptyList()
    ) {
        /** Whole minutes until this arrival (floor, matching the phone); 0 means "due". */
        fun minutesFrom(nowMillis: Long, arrival: ArrivalOnScreen): Long =
            minutesUntil(arrival.atMillis, nowMillis).coerceAtLeast(0L)
    }

    data class Snapshot(
        val rows: List<Row>,
        val hasFavorites: Boolean,
        val updatedAtMillis: Long
    )

    fun snapshot(context: Context): Snapshot {
        val json = prefs(context).getString(KEY_JSON, null) ?: return Snapshot(emptyList(), false, 0L)
        return runCatching {
            val root = JSONObject(json)
            val arr = root.optJSONArray("rows") ?: JSONArray()
            val rows = (0 until arr.length()).map { i -> rowFromJson(arr.getJSONObject(i)) }
            Snapshot(
                rows = rows.filter { it.arrivals.isNotEmpty() },
                hasFavorites = root.optBoolean("hasFavorites", false),
                updatedAtMillis = root.optLong("updated", 0L)
            )
        }.getOrDefault(Snapshot(emptyList(), false, 0L))
    }

    fun update(context: Context, favorites: List<Stop>, rows: List<Row>) {
        val arr = JSONArray()
        rows.forEach { row -> arr.put(rowToJson(row)) }
        val root = JSONObject()
            .put("hasFavorites", favorites.isNotEmpty())
            .put("updated", System.currentTimeMillis())
            .put("rows", arr)
        prefs(context).edit { putString(KEY_JSON, root.toString()) }
    }

    private fun rowToJson(row: Row) = JSONObject()
        .put("locId", row.stop.locId)
        .put("name", row.stop.desc)
        .put("dir", row.stop.dirDesc)
        .put("route", row.stop.routeNum)
        .put("type", row.stop.transitType)
        .put("lat", row.stop.latitude)
        .put("lng", row.stop.longitude)
        .put(
            "arrivals",
            JSONArray().apply {
                row.arrivals.forEach { a ->
                    put(
                        JSONObject()
                            .put("sign", a.sign)
                            .put("at", a.atMillis)
                            .put("dropOffOnly", a.dropOffOnly)
                            .put("status", a.status)
                    )
                }
            }
        )
        .put(
            "detours",
            JSONArray().apply {
                row.detours.forEach { d ->
                    put(
                        JSONObject()
                            .put("id", d.id)
                            .put("desc", d.desc)
                            .put(
                                "routes",
                                JSONArray().apply {
                                    d.routeIds.forEach { put(it) }
                                }
                            )
                    )
                }
            }
        )

    private fun rowFromJson(o: JSONObject): Row {
        val stop = Stop(
            desc = o.optString("name", ""),
            dirDesc = o.optString("dir", ""),
            latitude = o.optDouble("lat", 0.0),
            longitude = o.optDouble("lng", 0.0),
            transitType = o.optString("type", "bus"),
            locId = o.optInt("locId", 0),
            routeNum = o.optInt("route", 0)
        )
        val arr = o.optJSONArray("arrivals") ?: JSONArray()
        val arrivals = (0 until arr.length()).map { i ->
            val a = arr.getJSONObject(i)
            ArrivalOnScreen(
                a.optString("sign", ""),
                a.optLong("at", 0L),
                a.optBoolean("dropOffOnly", false),
                a.optString("status", "")
            )
        }
        val detours = o.optJSONArray("detours") ?: JSONArray()
        val parsedDetours = (0 until detours.length()).map { i ->
            val d = detours.getJSONObject(i)
            val routes = d.optJSONArray("routes") ?: JSONArray()
            WidgetDetour(
                id = d.optInt("id", 0),
                desc = d.optString("desc", ""),
                routeIds = (0 until routes.length()).map { routes.getInt(it) }
            )
        }
        return Row(stop, arrivals, parsedDetours)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** Sorts and caps an API result to the two soonest non-past arrivals. */
    fun cleanArrivals(result: List<Arrival>): List<ArrivalOnScreen> =
        result
            .mapNotNull { a ->
                val at = a.estimatedMillis.takeIf { it > 0L } ?: a.scheduledMillis.takeIf { it > 0L }
                at?.let {
                    ArrivalOnScreen(
                        sign = a.shortSign.ifBlank { a.fullSign },
                        atMillis = it,
                        dropOffOnly = a.dropOffOnly,
                        status = a.status
                    )
                }
            }
            .distinctBy { it.atMillis }
            .sortedBy { it.atMillis }
            .take(4)

    /** Collapses the arrival response's detour list to the first entry per id. */
    fun dedupeDetours(detours: List<Detour>): List<WidgetDetour> {
        val seen = mutableSetOf<Int>()
        return detours.mapNotNull { d ->
            if (!seen.add(d.id)) return@mapNotNull null
            WidgetDetour(
                id = d.id,
                desc = d.desc,
                routeIds = d.routes.orEmpty()
            )
        }
    }
}