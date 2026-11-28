package com.trimettransit.tracker.widget

import com.trimettransit.tracker.model.Stop

/** Intent-extras contract between widget taps and [com.trimettransit.tracker.activities.MainActivity]. */
object WidgetLaunch {
    const val EXTRA_STOP_ID = "widget_stop_id"
    const val EXTRA_STOP_NAME = "widget_stop_name"
    const val EXTRA_ROUTE_ID = "widget_route_id"
    const val EXTRA_LAT = "widget_lat"
    const val EXTRA_LNG = "widget_lng"
}

/**
 * Pure parse half of the widget-tap pipeline: validates the stop id range and
 * builds the [Stop], or null when the extras carry no launchable stop. Kept
 * Intent-free so plain JVM tests pin the range rules.
 */
fun stopFromLaunchExtras(
    stopId: Long,
    name: String?,
    routeId: Int,
    lat: Double,
    lng: Double
): Stop? {
    if (stopId <= 0L || stopId > Int.MAX_VALUE.toLong()) return null
    // Extras come from any app that can start MainActivity: NaN/Inf coordinates or an
    // unbounded name would land in SQLite and the map.
    if (!lat.isFinite() || !lng.isFinite()) return null
    return Stop(
        desc = name.orEmpty().take(MAX_STOP_NAME_CHARS),
        latitude = lat,
        longitude = lng,
        transitType = "bus",
        locId = stopId.toInt(),
        routeNum = routeId
    )
}

/** Longest widget-supplied stop name accepted; longer strings are truncated. */
private const val MAX_STOP_NAME_CHARS = 64