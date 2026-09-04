package com.trimettransit.tracker

import android.app.Application
import org.maplibre.android.MapLibre
import timber.log.Timber

class TrimetTransitTracker : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // Dynamic color is applied by the Compose theme (TriMetGoTheme) so it can honor
        // the user's dynamic-Color preference toggle. Applying it app-wide here too would
        // override the XML window theme unconditionally and fight the Compose side.
        MapLibre.getInstance(this)
        // Placeholder: 13.x requires a non-empty key, but the OpenFreeMap style URL needs no
        // token and the key is only ever attached to MapTiler-style canonical URLs.
        MapLibre.setApiKey("trimet-bus-tracker")
    }
}
