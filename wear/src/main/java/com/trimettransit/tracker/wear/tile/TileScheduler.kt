package com.trimettransit.tracker.wear.tile

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules background freshness for the "Next departure" tile. The tile itself is
 * self-refreshing inside its timeline window; this keeps that window full.
 */
object TileScheduler {
    private const val PERIODIC_NAME = "pdxbus_tile_refresh"
    private const val ONE_SHOT_NAME = "pdxbus_tile_refresh_now"
    private const val PERIOD_MINUTES = 30L

    /** Called from [android.app.Application]/Activity launch; idempotent. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<TileRefreshWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
            .setConstraints(connectedConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
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