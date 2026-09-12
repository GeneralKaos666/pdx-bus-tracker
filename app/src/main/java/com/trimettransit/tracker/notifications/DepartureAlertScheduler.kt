package com.trimettransit.tracker.notifications

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Owns the departure-alert check chain. A unique, delayed one-time worker that
 * re-enqueues itself after every run; PeriodicWorkRequest floors at 15 minutes,
 * too coarse for a departure window, so a five-minute chain keeps checks near
 * real-time (WorkManager defers under Doze — the alerts are approximate by design).
 */
object DepartureAlertScheduler {
    const val UNIQUE_NAME = "pdxbus_departure_alerts"
    private const val CHECK_INTERVAL_MINUTES = 5L

    /** Enqueues (or replaces) the next check; also called by the worker to keep the chain alive. */
    fun ensureScheduled(context: Context) {
        val request = OneTimeWorkRequestBuilder<DepartureAlertWorker>()
            .setInitialDelay(CHECK_INTERVAL_MINUTES, TimeUnit.MINUTES)
            .setConstraints(connectedConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Immediate check — used when the user flips alerts on. */
    fun checkNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<DepartureAlertWorker>()
            .setConstraints(connectedConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    /** Cancels the chain when alerts are switched off. */
    fun stop(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_NAME)
    }

    private fun connectedConstraints() =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}