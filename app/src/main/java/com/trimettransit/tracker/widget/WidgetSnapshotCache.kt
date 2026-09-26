package com.trimettransit.tracker.widget

import android.content.Context
import androidx.core.content.edit
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.domain.dedupeArrivals
import com.trimettransit.tracker.util.minutesUntil
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber

/**
 * SharedPreferences snapshot backing the "Next arrivals" home-screen widget. The widget
 * provider runs on the launcher's render thread and must return fast, so this cache hands
 * it pre-fetched arrival data without network calls or SQLite. Grouping stays per stop: each row
 * carries its own arrivals, refreshed by one batched [WidgetRefreshWorker] request and split
 * per stop on arrival locid.
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
    ) {
        /**
         * Human age of this snapshot for settings/config UI, or null when it was never
         * refreshed (missing/corrupt cache reads back as 0 — never render that as epoch).
         * Same under-a-minute rule as the trip planner's data-age chip.
         */
        fun ageText(nowMillis: Long, minutesFmt: String, justNow: String): String? {
            if (updatedAtMillis <= 0L) return null
            val mins = ((nowMillis - updatedAtMillis) / 60_000L).coerceAtLeast(0)
            return if (mins < 1) justNow else minutesFmt.format(mins)
        }
    }

    fun snapshot(context: Context): Snapshot {
        val json = prefs(context).getString(KEY_JSON, null) ?: return Snapshot(emptyList(), false, 0L)
        return parseSnapshotLenient(json)
    }

    /**
     * Lenient, Context-free snapshot parse, kept pure so it is unit-testable without
     * Android framework calls. A corrupt row is skipped instead of voiding the whole
     * snapshot, and rows with no arrivals are kept so stops never vanish from the
     * widget just because every bus just left.
     */
    fun parseSnapshotLenient(json: String): Snapshot {
        return runCatching {
            val root = JSONObject(json)
            val arr = root.optJSONArray("rows") ?: JSONArray()
            val rows = buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    runCatching { rowFromJson(o) }.getOrNull()?.let { add(it) }
                }
            }
            Snapshot(
                rows = rows,
                hasFavorites = root.optBoolean("hasFavorites", false),
                updatedAtMillis = root.optLong("updated", 0L)
            )
        }.getOrElse { e ->
            // A stale or schema-mismatched snapshot shouldn't crash the launcher's render
            // thread, but it must not be invisible either — the widget would otherwise stay
            // blank on "refreshing…" with no signal.
            Timber.w(e, "Failed to parse the widget snapshot")
            Snapshot(emptyList(), false, 0L)
        }
    }

    /**
     * Pure snapshot serializer — the write half of [parseSnapshotLenient], kept
     * Context-free so unit tests pin the real persisted shape (including the
     * "updated" timestamp) without Android framework calls.
     */
    fun snapshotToJson(
        favorites: List<Stop>,
        rows: List<Row>,
        nowMillis: Long = System.currentTimeMillis()
    ): String {
        val arr = JSONArray()
        rows.forEach { row -> arr.put(rowToJson(row)) }
        return JSONObject()
            .put("hasFavorites", favorites.isNotEmpty())
            .put("updated", nowMillis)
            .put("rows", arr)
            .toString()
    }

    fun update(context: Context, favorites: List<Stop>, rows: List<Row>) {
        prefs(context).edit { putString(KEY_JSON, snapshotToJson(favorites, rows)) }
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
        val arrivals = buildList {
            for (i in 0 until arr.length()) {
                val a = arr.optJSONObject(i) ?: continue
                add(
                    ArrivalOnScreen(
                        a.optString("sign", ""),
                        a.optLong("at", 0L),
                        a.optBoolean("dropOffOnly", false),
                        a.optString("status", "")
                    )
                )
            }
        }
        val detours = o.optJSONArray("detours") ?: JSONArray()
        val parsedDetours = buildList {
            for (i in 0 until detours.length()) {
                val d = detours.optJSONObject(i) ?: continue
                val routes = d.optJSONArray("routes") ?: JSONArray()
                add(
                    WidgetDetour(
                        id = d.optInt("id", 0),
                        desc = d.optString("desc", ""),
                        routeIds = (0 until routes.length()).map { routes.optInt(it) }
                    )
                )
            }
        }
        return Row(stop, arrivals, parsedDetours)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    /** Sorts and caps an API result to the two soonest non-past arrivals. */
    fun cleanArrivals(result: List<Arrival>): List<ArrivalOnScreen> =
        dedupeArrivals(result)
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
            // Same-time arrivals on different signs are different buses — dedupe on both.
            .distinctBy { it.atMillis to it.sign }
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