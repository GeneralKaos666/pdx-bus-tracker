package com.trimettransit.tracker.wear.tile

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.FavoritesRepositoryImpl
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.transit.ApiKeys
import com.trimettransit.tracker.transit.TransitRepositoryImpl
import com.trimettransit.tracker.util.ConnectionUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Scheduled one-shot that refreshes the "Next departure" tile off the watch's own
 * favorites (first favorite wins) and pushes a new timeline to the system. Runs
 * periodically while the watch has no saved favorites it no-ops.
 */
class TileRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext
        if (ApiKeys.getTrimetApiKey().isBlank()) {
            // Without a key every fetch will return null; retrying would loop forever.
            return@withContext Result.success()
        }
        if (!ConnectionUtils.isOnline(app)) {
            return@withContext if (runAttemptCount < MAX_CONSECUTIVE_RETRIES) Result.retry() else Result.success()
        }

        val transitRepository: TransitRepository = TransitRepositoryImpl(app)
        val favorite = FavoritesRepositoryImpl(DatabaseHelper(app)).getFavorites().firstOrNull()
        if (favorite == null) {
            // No favorite to feature; keep whatever the tile last showed.
            return@withContext Result.success()
        }

        val result = transitRepository.getArrivals(listOf(favorite.locId), minutes = 45, maxArrivals = 8)
            ?: return@withContext if (runAttemptCount < MAX_CONSECUTIVE_RETRIES) Result.retry() else Result.success()

        TileCache.update(app, favorite, result.arrivals.orEmpty())
        TileService.getUpdater(app).requestUpdate(FavoriteArrivalsTileService::class.java)
        Result.success()
    }

    companion object {
        private const val MAX_CONSECUTIVE_RETRIES = 3
    }
}