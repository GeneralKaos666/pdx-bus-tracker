package com.trimettransit.tracker

import android.content.Context
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.FavoritesRepositoryImpl
import com.trimettransit.tracker.data.local.RecentStopsRepositoryImpl
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.RecentStopsRepository
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.transit.TransitRepositoryImpl

/** The app's data-access repositories, built from the application [Context]. */
data class Repos(
    val favorites: FavoritesRepository,
    val recentStops: RecentStopsRepository,
    val transit: TransitRepository
)

/** Builds the standard app repositories, sharing one [DatabaseHelper]. */
fun Context.repos(): Repos {
    val appContext = applicationContext
    val db = DatabaseHelper(appContext)
    return Repos(
        favorites = FavoritesRepositoryImpl(db),
        recentStops = RecentStopsRepositoryImpl(db),
        transit = sharedTransitRepository(appContext)
    )
}

/**
 * One [TransitRepositoryImpl] per process, so the stop-search cache (15-min TTL
 * on a several-MB dump) is genuinely shared between the Home list and the trip
 * planner instead of being rebuilt per screen.
 */
private val transitRepoLock = Any()
private var transitRepository: TransitRepository? = null

private fun sharedTransitRepository(context: Context): TransitRepository =
    transitRepository ?: synchronized(transitRepoLock) {
        transitRepository ?: TransitRepositoryImpl(context).also { transitRepository = it }
    }