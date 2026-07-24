package com.trimettransit.tracker.util

import com.trimettransit.tracker.BuildConfig

object Constants2 {
    @JvmStatic
    fun getTrimetApiKey(): String {
        return BuildConfig.TRIMET_API_KEY?.trim() ?: ""
    }

    @JvmStatic
    fun hasTrimetApiKey(): Boolean = getTrimetApiKey().isNotEmpty()
}
