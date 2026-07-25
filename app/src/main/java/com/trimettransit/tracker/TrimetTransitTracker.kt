package com.trimettransit.tracker

import android.app.Application
import com.google.android.material.color.DynamicColors
import org.osmdroid.config.Configuration

class TrimetTransitTracker : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        Configuration.getInstance().userAgentValue = packageName
    }
}
