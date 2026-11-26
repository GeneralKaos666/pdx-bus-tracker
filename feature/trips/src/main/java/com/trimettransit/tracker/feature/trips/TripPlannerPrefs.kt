package com.trimettransit.tracker.feature.trips

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.model.TripPlannerMin
import com.trimettransit.tracker.model.TripPlannerMode
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripRequestOptions
import com.trimettransit.tracker.model.domain.addRecentTrip
import com.trimettransit.tracker.model.domain.decodeRecentTrips
import com.trimettransit.tracker.model.domain.encodeRecentTrips

/**
 * Read/write the user's trip-planning options in the default SharedPreferences, mirroring
 * how the settings and arrivals screens persist their settings. Unknown or out-of-range
 * stored values fall back to the defaults, which match the app's historical request.
 *
 * Also remembers the last few planned destinations so the endpoint picker can offer them
 * without a network round trip.
 */
internal object TripPlannerPrefs {
    private const val KEY_MODE = "trip_planner.mode"
    private const val KEY_MIN = "trip_planner.min"
    private const val KEY_MAX_WALK = "trip_planner.max_walk"
    private const val KEY_ITINERARIES = "trip_planner.itineraries"
    private const val KEY_RECENT_TRIPS = "trip_planner.recent_destinations"

    fun load(context: Context): TripRequestOptions {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val mode = when (prefs.getString(KEY_MODE, null)) {
            TripPlannerMode.BUS.wsCode -> TripPlannerMode.BUS
            TripPlannerMode.TRAIN.wsCode -> TripPlannerMode.TRAIN
            else -> TripPlannerMode.ALL
        }
        val min = when (prefs.getString(KEY_MIN, null)) {
            TripPlannerMin.TRANSFERS.wsCode -> TripPlannerMin.TRANSFERS
            TripPlannerMin.WALKING.wsCode -> TripPlannerMin.WALKING
            else -> TripPlannerMin.TIME
        }
        val maxWalk = prefs.getFloat(KEY_MAX_WALK, 0.5f).coerceIn(0.01f, 0.999f)
        val itineraryCount = prefs.getInt(KEY_ITINERARIES, 3).coerceIn(1, 6)
        return TripRequestOptions(
            mode = mode,
            min = min,
            maxWalkMiles = maxWalk,
            itineraryCount = itineraryCount
        )
    }

    fun save(context: Context, options: TripRequestOptions) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_MODE, options.mode.wsCode)
            putString(KEY_MIN, options.min.wsCode)
            putFloat(KEY_MAX_WALK, options.maxWalkMiles)
            putInt(KEY_ITINERARIES, options.itineraryCount)
        }
    }

    /** Remembered destinations, newest first. */
    fun loadRecentTrips(context: Context): List<TripPoint> {
        val raw = PreferenceManager.getDefaultSharedPreferences(context)
            .getString(KEY_RECENT_TRIPS, null)
        return decodeRecentTrips(raw)
    }

    /** Moves [destination] to the front of the remembered list and returns the new list. */
    fun addRecentTrip(context: Context, destination: TripPoint): List<TripPoint> {
        val updated = addRecentTrip(loadRecentTrips(context), destination)
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            putString(KEY_RECENT_TRIPS, encodeRecentTrips(updated))
        }
        return updated
    }

    fun clearRecentTrips(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit {
            remove(KEY_RECENT_TRIPS)
        }
    }
}