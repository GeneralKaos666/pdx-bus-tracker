package com.trimettransit.tracker.wear.tile

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.transit.TransitApi
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
        if (!ConnectionUtils.isOnline(app)) {
            return@withContext Result.retry()
        }

        val favorite = DatabaseHelper(app).favorites.firstOrNull()
        if (favorite == null) {
            // No favorite to feature; keep whatever the tile last showed.
            return@withContext Result.success()
        }

        val result = TransitApi.fetchArrivals(app, listOf(favorite.locId), minutes = 45, maxArrivals = 8)
            ?: return@withContext Result.retry()

        TileCache.update(app, favorite, result.arrivals.orEmpty())
        TileService.getUpdater(app).requestUpdate(FavoriteArrivalsTileService::class.java)
        Result.success()
    }
}