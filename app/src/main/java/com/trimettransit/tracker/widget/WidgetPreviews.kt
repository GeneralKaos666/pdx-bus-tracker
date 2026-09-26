package com.trimettransit.tracker.widget

import android.content.Context
import android.os.Build
import androidx.glance.appwidget.GlanceAppWidgetManager

/**
 * Launcher-picker previews (Android 15+): `providePreview` renders the
 * composition; this pushes it to the picker. The platform rate-limits pushes
 * (~2/hour), so this is throttled to one push per hour — call it on the
 * data-ready path (post-fetch), never in a refresh loop.
 */
object WidgetPreviews {
    internal const val PREF_NAME = "widget_previews"
    internal const val KEY_LAST_PUSH_MILLIS = "last_push_millis"
    internal const val MIN_INTERVAL_MILLIS = 60 * 60 * 1000L

    suspend fun pushIfDue(context: Context, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (nowMillis - prefs.getLong(KEY_LAST_PUSH_MILLIS, 0L) < MIN_INTERVAL_MILLIS) return false
        // Generated picker previews exist only on Android 15+; older launchers
        // fall back to previewLayout/previewImage from the provider XML.
        if (Build.VERSION.SDK_INT < 35) return false
        runCatching {
            GlanceAppWidgetManager(context).setWidgetPreviews(NextArrivalsWidgetReceiver::class)
        }
        prefs.edit().putLong(KEY_LAST_PUSH_MILLIS, nowMillis).apply()
        return true
    }
}
