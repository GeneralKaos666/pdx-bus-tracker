package com.trimettransit.tracker

import android.app.Application
import com.google.android.material.color.DynamicColors
import org.maplibre.android.MapLibre

class TrimetTransitTracker : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        MapLibre.getInstance(this)
        // Placeholder: 13.x requires a non-empty key, but the OpenFreeMap style URL needs no
        // token and the key is only ever attached to MapTiler-style canonical URLs.
        MapLibre.setApiKey("trimet-bus-tracker")
    }
}
