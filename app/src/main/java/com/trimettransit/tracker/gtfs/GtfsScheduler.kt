package com.trimettransit.tracker.gtfs

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object GtfsScheduler {
    private const val PERIODIC_NAME = "pdxbus_gtfs_static_refresh"
    private const val ON_DEMAND_NAME = "pdxbus_gtfs_static_refresh_now"

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<GtfsRefreshWorker>(7, TimeUnit.DAYS)
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun refreshNow(context: Context) {
        val request = OneTimeWorkRequestBuilder<GtfsRefreshWorker>()
            .setConstraints(networkConstraints())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            ON_DEMAND_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun networkConstraints() =
        Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
}
