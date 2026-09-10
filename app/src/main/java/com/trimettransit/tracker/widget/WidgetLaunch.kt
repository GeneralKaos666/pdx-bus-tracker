package com.trimettransit.tracker.widget

/** Intent-extras contract between widget taps and [com.trimettransit.tracker.activities.MainActivity]. */
object WidgetLaunch {
    const val EXTRA_STOP_ID = "widget_stop_id"
    const val EXTRA_STOP_NAME = "widget_stop_name"
    const val EXTRA_ROUTE_ID = "widget_route_id"
    const val EXTRA_LAT = "widget_lat"
    const val EXTRA_LNG = "widget_lng"
}