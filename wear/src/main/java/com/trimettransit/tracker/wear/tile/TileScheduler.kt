package com.trimettransit.tracker.wear.tile

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.trimettransit.tracker.wear.WearPrefs
import java.util.concurrent.TimeUnit

/**
 * Schedules background freshness for the "Next departure" tile. The tile itself is
 * self-refreshing inside its timeline window; this keeps that window full.
 */
object TileScheduler {
    private const val PERIODIC_NAME = "pdxbus_tile_refresh"
    private const val ONE_SHOT_NAME = "pdxbus_tile_refresh_now"

    /** Called from Activity launch; idempotent. Follows the user's refresh-interval
     *  setting ([WearPrefs.refreshIntervalMinutes]). */
    fun schedulePeriodic(context: Context) {
        schedulePeriodic(context, WearPrefs.refreshIntervalMinutes(context))
    }

    /** Re-arms periodic work at [intervalMinutes]; used when the setting changes. */
    fun schedulePeriodic(context: Context, intervalMinutes: Int) {
        val request = PeriodicWorkRequestBuilder<TileRefreshWorker>(intervalMinutes.toLong(), TimeUnit.MINUTES)
            .setConstraints(connectedConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /** Immediate refresh (e.g. after the user changes favorites or views a stop). */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<TileRefreshWorker>()
            .setConstraints(connectedConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(ONE_SHOT_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    private fun connectedConstraints() =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}