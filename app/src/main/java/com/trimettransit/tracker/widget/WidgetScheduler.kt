package com.trimettransit.tracker.widget

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
 * Owns the widget's background freshness. A 30-minute periodic refresh keeps the snapshot
 * reasonably current (adds resume as an event when placed on launchers that pause us), and
 * [refreshNow] enqueues an immediate one-shot for "user just did something" moments, e.g.
 * after an info refresh or a favorite list change. WorkManager dedupes and coalesces these.
 */
object WidgetScheduler {
    private const val PERIODIC_NAME = "pdxbus_widget_refresh"
    private const val ONE_SHOT_NAME = "pdxbus_widget_refresh_now"
    private const val PERIOD_MINUTES = 30L

    /** Called once from the Application/Activity; idempotent. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(PERIOD_MINUTES, TimeUnit.MINUTES)
            .setConstraints(connectedConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** Immediate refresh; replaces any pending one-shot so coalescing stays natural. */
    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setConstraints(connectedConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_SHOT_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun connectedConstraints() =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}