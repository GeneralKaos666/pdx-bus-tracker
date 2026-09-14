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
    val db = DatabaseHelper(this)
    return Repos(
        favorites = FavoritesRepositoryImpl(db),
        recentStops = RecentStopsRepositoryImpl(db),
        transit = TransitRepositoryImpl(this)
    )
}