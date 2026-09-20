package com.trimettransit.tracker

import android.app.Application
import com.trimettransit.tracker.gtfs.GtfsScheduler
import com.trimettransit.tracker.map.initializeMapRuntime
import timber.log.Timber

class TrimetTransitTracker : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        // Dynamic color is applied by the Compose theme (TriMetGoTheme) so it can honor
        // the user's dynamic-Color preference toggle. Applying it app-wide here too would
        // override the XML window theme unconditionally and fight the Compose side.
        initializeMapRuntime(this)
        GtfsScheduler.schedulePeriodic(this)
    }
}
