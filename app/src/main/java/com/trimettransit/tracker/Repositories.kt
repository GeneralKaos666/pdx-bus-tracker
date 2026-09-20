package com.trimettransit.tracker

import android.content.Context
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.FavoritesRepositoryImpl
import com.trimettransit.tracker.data.local.RecentStopsRepositoryImpl
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.GtfsStaticStore
import com.trimettransit.tracker.model.repository.RecentStopsRepository
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.transit.TransitRepositoryImpl
import com.trimettransit.tracker.transit.createGtfsStaticStore

/** The app's data-access repositories, built from the application [Context]. */
data class Repos(
    val favorites: FavoritesRepository,
    val recentStops: RecentStopsRepository,
    val transit: TransitRepository,
    val gtfsStatic: GtfsStaticStore
)

/** Builds the standard app repositories over the shared singletons below. */
fun Context.repos(): Repos {
    val appContext = applicationContext
    val db = sharedDatabaseHelper(appContext)
    return Repos(
        favorites = FavoritesRepositoryImpl(db),
        recentStops = RecentStopsRepositoryImpl(db),
        transit = sharedTransitRepository(appContext),
        gtfsStatic = sharedGtfsStaticStore(appContext)
    )
}

/** One [DatabaseHelper] per process: [SQLiteOpenHelper] is thread-safe and built for
 * sharing, so UI + widget + alert workers reuse one connection pool instead of opening
 * a fresh helper (and fresh SQLite connections) per [repos] call. */
private val dbLock = Any()
@Volatile
private var databaseHelper: DatabaseHelper? = null

private fun sharedDatabaseHelper(context: Context): DatabaseHelper =
    databaseHelper ?: synchronized(dbLock) {
        databaseHelper ?: DatabaseHelper(context).also { databaseHelper = it }
    }

/**
 * One [TransitRepositoryImpl] per process, so the stop-search cache (15-min TTL
 * on a several-MB dump) is genuinely shared between the Home list and the trip
 * planner instead of being rebuilt per screen.
 */
private val transitRepoLock = Any()
@Volatile
private var transitRepository: TransitRepository? = null

private fun sharedTransitRepository(context: Context): TransitRepository =
    transitRepository ?: synchronized(transitRepoLock) {
        transitRepository ?: TransitRepositoryImpl(context).also { transitRepository = it }
    }

private val gtfsStoreLock = Any()
@Volatile
private var gtfsStaticStore: GtfsStaticStore? = null

private fun sharedGtfsStaticStore(context: Context): GtfsStaticStore =
    gtfsStaticStore ?: synchronized(gtfsStoreLock) {
        gtfsStaticStore ?: createGtfsStaticStore(context).also { gtfsStaticStore = it }
    }