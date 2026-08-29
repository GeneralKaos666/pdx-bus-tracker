package com.trimettransit.tracker

import android.app.Application
import com.google.android.material.color.DynamicColors
import com.trimettransit.tracker.wear.WearDataSync
import org.maplibre.android.MapLibre
import timber.log.Timber

class TrimetTransitTracker : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        DynamicColors.applyToActivitiesIfAvailable(this)
        MapLibre.getInstance(this)
        // Placeholder: 13.x requires a non-empty key, but the OpenFreeMap style URL needs no
        // token and the key is only ever attached to MapTiler-style canonical URLs.
        MapLibre.setApiKey("trimet-bus-tracker")
        // Keep the paired Wear device's favorites/recent stops up to date.
        runCatching { WearDataSync.start(this) }
    }
}
