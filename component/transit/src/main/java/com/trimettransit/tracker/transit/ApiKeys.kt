package com.trimettransit.tracker.transit

import com.trimettransit.tracker.transit.BuildConfig

internal object ApiKeys {
    @JvmStatic
    fun getTrimetApiKey(): String {
        return BuildConfig.TRIMET_API_KEY.trim()
    }
}
