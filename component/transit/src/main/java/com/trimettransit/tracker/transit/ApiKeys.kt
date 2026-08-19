package com.trimettransit.tracker.transit

import com.trimettransit.tracker.transit.BuildConfig

object ApiKeys {
    @JvmStatic
    fun getTrimetApiKey(): String {
        return BuildConfig.TRIMET_API_KEY.trim()
    }

    @JvmStatic
    fun hasTrimetApiKey(): Boolean = getTrimetApiKey().isNotEmpty()
}
