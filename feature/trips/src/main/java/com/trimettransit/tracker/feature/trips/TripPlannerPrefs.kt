package com.trimettransit.tracker.feature.trips

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.model.TripPlannerMode
import com.trimettransit.tracker.model.TripRequestOptions

/**
 * Read/write the user's trip-planning options in the default SharedPreferences, mirroring
 * how the settings and arrivals screens persist their settings. Unknown or out-of-range
 * stored values fall back to the defaults, which match the app's historical request.
 */
internal object TripPlannerPrefs {
    private const val KEY_MODE = "trip_planner.mode"
    private const val KEY_MAX_WALK = "trip_planner.max_walk"
    private const val KEY_ITINERARIES = "trip_planner.itineraries"

    fun load(context: Context): TripRequestOptions {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val mode = when (prefs.getString(KEY_MODE, null)) {
            TripPlannerMode.BUS.wsCode -> TripPlannerMode.BUS
            TripPlannerMode.TRAIN.wsCode -> TripPlannerMode.TRAIN
            else -> TripPlannerMode.ALL
        }
        val maxWalk = prefs.getFloat(KEY_MAX_WALK, 0.5f).coerceIn(0.01f, 0.999f)
        val itineraryCount = prefs.getInt(KEY_ITINERARIES, 3).coerceIn(1, 6)
        return TripRequestOptions(
            mode = mode,
            maxWalkMiles = maxWalk,
            itineraryCount = itineraryCount
        )
    }

    fun save(context: Context, options: TripRequestOptions) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_MODE, options.mode.wsCode)
            putFloat(KEY_MAX_WALK, options.maxWalkMiles)
            putInt(KEY_ITINERARIES, options.itineraryCount)
        }
    }
}