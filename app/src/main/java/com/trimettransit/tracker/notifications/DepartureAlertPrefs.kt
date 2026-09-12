package com.trimettransit.tracker.notifications

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.model.Stop

/**
 * SharedPreferences backing for departure alerts. Everything here defaults off —
 * the feature stays inert until the user opts in from Settings.
 */
object DepartureAlertPrefs {
    const val KEY_ENABLED = "pref_key_departure_alerts_enabled"
    const val KEY_WINDOW_MIN = "pref_key_departure_alerts_window_min"
    const val KEY_FIRED = "pref_key_departure_alerts_fired"
    private const val KEY_STOP_PREFIX = "pref_key_departure_alerts_stop_"
    private val allowedWindows = setOf(5, 10, 15)
    const val DEFAULT_WINDOW_MIN = 10

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_ENABLED, enabled) }
    }

    fun windowMinutes(context: Context): Int =
        prefs(context).getInt(KEY_WINDOW_MIN, DEFAULT_WINDOW_MIN)
            .let { if (it in allowedWindows) it else DEFAULT_WINDOW_MIN }

    fun setWindowMinutes(context: Context, minutes: Int) {
        prefs(context).edit { putInt(KEY_WINDOW_MIN, minutes) }
    }

    fun isStopAlerted(context: Context, locId: Int): Boolean =
        prefs(context).getBoolean(KEY_STOP_PREFIX + locId, false)

    fun setStopAlerted(context: Context, locId: Int, enabled: Boolean) {
        prefs(context).edit { putBoolean(KEY_STOP_PREFIX + locId, enabled) }
    }

    /** Favorites with alerts on — the worker's monitoring list. */
    fun monitoredStops(context: Context, favorites: List<Stop>): List<Stop> =
        favorites.filter { isStopAlerted(context, it.locId) }

    fun getFired(context: Context): MutableSet<String> =
        prefs(context).getStringSet(KEY_FIRED, emptySet())?.toMutableSet() ?: mutableSetOf()

    fun setFired(context: Context, fired: Set<String>) {
        prefs(context).edit { putStringSet(KEY_FIRED, fired) }
    }

    fun prefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)
}