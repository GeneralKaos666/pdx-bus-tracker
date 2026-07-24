package com.trimettransit.tracker

import android.app.Application
import com.google.android.material.color.DynamicColors

class TrimetTransitTracker : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
