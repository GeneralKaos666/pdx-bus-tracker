package com.trimettransit.tracker.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.repos
import com.trimettransit.tracker.retryFetch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

class WidgetRefreshWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        return withContext(Dispatchers.IO) {
            val favoritesRepository = app.repos().favorites
            val transitRepository = app.repos().transit
            if (!transitRepository.isConfigured()) return@withContext Result.success()
            val favorites = favoritesRepository.getFavorites().take(MAX_STOPS)
            if (favorites.isEmpty()) {
                WidgetSnapshotCache.update(app, emptyList(), emptyList())
                NextArrivalsWidget().updateAll(app)
                return@withContext Result.success()
            }

            // One batched request for all stops instead of N per-stop requests: the
            // arrivals endpoint accepts comma-joined locIDs and each arrival carries
            // its stop's locid, so rows split client-side below.
            val ids = favorites.map { it.locId }
            val result = retryFetch(attempts = MAX_ATTEMPTS, label = "Widget") {
                transitRepository.getArrivals(
                    locIds = ids,
                    minutes = WINDOW_MINUTES,
                    maxArrivals = ARRIVALS_PER_STOP * ids.size
                )
            }
            val arrivals = result?.arrivals.orEmpty()
            val detours = result?.detours.orEmpty()
            val rows = favorites.map { stop -> buildRow(stop, arrivals, detours) }
            WidgetSnapshotCache.update(app, favorites, rows)
            NextArrivalsWidget().updateAll(app)
            Result.success()
        }
    }

    private fun buildRow(
        stop: Stop,
        arrivals: List<com.trimettransit.tracker.model.Arrival>,
        detours: List<com.trimettransit.tracker.model.Detour>
    ): WidgetSnapshotCache.Row {
        // Prefer locid-attributed arrivals; fall back to the full list when the
        // backend omits locid (single-stop responses, legacy shapes) so the widget
        // never renders an empty row it could have filled.
        val mine = arrivals.filter { it.locId == stop.locId }.ifEmpty { arrivals }
        return WidgetSnapshotCache.Row(
            stop = stop,
            arrivals = WidgetSnapshotCache.cleanArrivals(mine),
            detours = WidgetSnapshotCache.dedupeDetours(detours)
        )
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
        return buildRow(stop, result?.arrivals.orEmpty(), result?.detours.orEmpty())
    }

    companion object {
        const val MAX_STOPS = 12
        const val WINDOW_MINUTES = 30
        const val ARRIVALS_PER_STOP = 4
        const val MAX_ATTEMPTS = 3
    }
}