package com.trimettransit.tracker.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.repos
import com.trimettransit.tracker.retryFetch
import com.trimettransit.tracker.transit.ApiKeys
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class WidgetRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (ApiKeys.getTrimetApiKey().isBlank()) return Result.success()
        val app = applicationContext
        return withContext(Dispatchers.IO) {
            val favoritesRepository = app.repos().favorites
            val transitRepository = app.repos().transit
            val favorites = favoritesRepository.getFavorites().take(MAX_STOPS)
            if (favorites.isEmpty()) {
                WidgetSnapshotCache.update(app, emptyList(), emptyList())
                NextArrivalsWidget().updateAll(app)
                return@withContext Result.success()
            }

            val rows = coroutineScope {
                favorites.map { stop -> async { fetchRow(transitRepository, stop) } }.awaitAll()
            }
            WidgetSnapshotCache.update(app, favorites, rows)
            NextArrivalsWidget().updateAll(app)
            Result.success()
        }
    }

    private suspend fun fetchRow(
        transitRepository: TransitRepository,
        stop: Stop
    ): WidgetSnapshotCache.Row {
        val result = retryFetch(attempts = MAX_ATTEMPTS, label = "Widget") {
            transitRepository.getArrivals(
                locIds = listOf(stop.locId),
                minutes = WINDOW_MINUTES,
                maxArrivals = ARRIVALS_PER_STOP
            )
        }
        return WidgetSnapshotCache.Row(
            stop = stop,
            arrivals = WidgetSnapshotCache.cleanArrivals(result?.arrivals.orEmpty()),
            detours = WidgetSnapshotCache.dedupeDetours(result?.detours.orEmpty())
        )
    }

    companion object {
        const val MAX_STOPS = 12
        const val WINDOW_MINUTES = 30
        const val ARRIVALS_PER_STOP = 4
        const val MAX_ATTEMPTS = 3
    }
}