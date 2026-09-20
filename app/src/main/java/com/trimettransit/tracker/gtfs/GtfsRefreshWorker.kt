package com.trimettransit.tracker.gtfs

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trimettransit.tracker.repos
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class GtfsRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            applicationContext.repos().gtfsStatic.refreshIfStale(MAX_CACHE_AGE_MILLIS)
        }.fold(
            onSuccess = { Result.success() },
            onFailure = {
                Timber.w(it, "GTFS static refresh failed")
                if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            }
        )
    }

    private companion object {
        const val MAX_CACHE_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000
        const val MAX_ATTEMPTS = 3
    }
}
