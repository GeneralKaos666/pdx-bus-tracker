package com.trimettransit.tracker.activities

import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Adds or removes a favorite, returning whether the write succeeded. Lives in the app shell because
 * it is the only consumer; feature code should call [FavoritesRepository.addFavorite] /
 * [FavoritesRepository.removeFavorite] directly.
 *
 * It deliberately returns no user-facing text. The arrivals heart is the same control the four lists
 * use, and that one shows no snackbar — a failure is logged and the heart simply does not flip.
 */
suspend fun toggleFavorite(
    favoritesRepository: FavoritesRepository,
    locId: Int,
    stopName: String,
    currentlyFavorite: Boolean,
    routeId: Int = -1,
    lat: Double = 0.0,
    lng: Double = 0.0
): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            if (currentlyFavorite) {
                favoritesRepository.removeFavorite(locId)
            } else {
                val stop = Stop(
                    desc = stopName,
                    latitude = lat,
                    longitude = lng,
                    routeNum = if (routeId > 0) routeId else 0,
                    locId = locId
                )
                favoritesRepository.addFavorite(stop)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle favorite")
            false
        }
    }
}
