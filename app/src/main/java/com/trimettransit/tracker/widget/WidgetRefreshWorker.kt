package com.trimettransit.tracker.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.FavoritesRepositoryImpl
import com.trimettransit.tracker.transit.TransitRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val app = applicationContext
        val favoritesRepository = FavoritesRepositoryImpl(DatabaseHelper(app))
        val transitRepository = TransitRepositoryImpl(app)
        val favorites = favoritesRepository.getFavorites().take(MAX_STOPS)
        if (favorites.isEmpty()) {
            WidgetSnapshotCache.update(app, emptyList(), emptyList())
            NextArrivalsWidget().updateAll(app)
            return@withContext Result.success()
        }

        val rows = mutableListOf<WidgetSnapshotCache.Row>()
        for (stop in favorites) {
            val result = transitRepository.getArrivals(
                locIds = listOf(stop.locId),
                minutes = WINDOW_MINUTES,
                maxArrivals = ARRIVALS_PER_STOP
            )
            rows.add(
                WidgetSnapshotCache.Row(
                    stop = stop,
                    arrivals = WidgetSnapshotCache.cleanArrivals(result?.arrivals.orEmpty())
                )
            )
        }
        WidgetSnapshotCache.update(app, favorites, rows)
        NextArrivalsWidget().updateAll(app)
        Result.success()
    }

    companion object {
        const val MAX_STOPS = 10
        const val WINDOW_MINUTES = 30
        const val ARRIVALS_PER_STOP = 2
    }
}
