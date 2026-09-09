package com.trimettransit.tracker.wear

import android.content.Context
import androidx.core.content.edit

/**
 * SharedPreferences store for watch-local settings. Kept deliberately tiny and
 * prefs-only (no Room, no ViewModels) mirroring the phone's settings pattern.
 */
object WearPrefs {
    private const val PREFS_NAME = "wear_settings"
    private const val KEY_REFRESH_INTERVAL_MIN = "tile_refresh_interval_min"
    private const val KEY_ONLY_SHOW_ROUTE = "only_show_selected_route"

    const val DEFAULT_REFRESH_INTERVAL_MIN = 30
    val TILE_REFRESH_OPTIONS = listOf(15, 30, 45, 60)

    fun refreshIntervalMinutes(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_REFRESH_INTERVAL_MIN, DEFAULT_REFRESH_INTERVAL_MIN)

    fun setRefreshIntervalMinutes(context: Context, minutes: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_REFRESH_INTERVAL_MIN, minutes) }
    }

    fun onlyShowSelectedRoute(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ONLY_SHOW_ROUTE, true)

    fun setOnlyShowSelectedRoute(context: Context, value: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_ONLY_SHOW_ROUTE, value) }
    }
}