package com.trimettransit.tracker.widget

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Owns the widget's background freshness. A periodic refresh keeps the snapshot reasonably
 * current (adds resume as an event when placed on launchers that pause us), and [refreshNow]
 * enqueues an immediate one-shot for "user just did something" moments, e.g. after an info
 * refresh or a favorite list change. WorkManager dedupes and coalesces these. The period is a
 * global setting read from [KEY_REFRESH_INTERVAL_MIN] (default 30 minutes).
 */
object WidgetScheduler {
    private const val PERIODIC_NAME = "pdxbus_widget_refresh"
    private const val ONE_SHOT_NAME = "pdxbus_widget_refresh_now"

    /** Global widget refresh interval in minutes (allowed values: 15, 30, 45, 60). */
    const val KEY_REFRESH_INTERVAL_MIN = "widget_refresh_interval_min"

    private const val DEFAULT_REFRESH_MINUTES = 30

    private val allowedRefreshIntervals = setOf(15, 30, 45, 60)

    /** Called once from the Application/Activity; idempotent. */
    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
            resolveRefreshMinutes(context).toLong(),
            TimeUnit.MINUTES
        )
            .setConstraints(connectedConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    /**
     * Idempotent re-schedule reading the current interval; call after the user changes it.
     * UPDATE policy makes a repeat of [schedulePeriodic] cheap, so this just forwards to it.
     */
    fun ensureScheduled(context: Context) = schedulePeriodic(context)

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

    /** Stored interval clamped to the allowed set (Settings only writes valid values). */
    private fun resolveRefreshMinutes(context: Context): Int {
        val stored = PreferenceManager.getDefaultSharedPreferences(context)
            .getInt(KEY_REFRESH_INTERVAL_MIN, DEFAULT_REFRESH_MINUTES)
        return if (stored in allowedRefreshIntervals) stored else DEFAULT_REFRESH_MINUTES
    }

    private fun connectedConstraints() =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}