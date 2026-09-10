package com.trimettransit.tracker.activities

import android.content.Context
import com.trimettransit.tracker.R
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Toggles a stop's favorite state. Lives in the app shell because it is the only
 * consumer and it resolves user-facing messages from Android resources; feature
 * code should call [FavoritesRepository.addFavorite]/[removeFavorite] directly.
 *
 * Returns (changed, message): `changed=false` only on an unrecoverable DB error.
 */
suspend fun toggleFavorite(
    favoritesRepository: FavoritesRepository,
    context: Context,
    locId: Int,
    stopName: String,
    currentlyFavorite: Boolean,
    routeId: Int = -1,
    lat: Double = 0.0,
    lng: Double = 0.0
): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            if (currentlyFavorite) {
                if (favoritesRepository.removeFavorite(locId)) {
                    true to context.getString(R.string.favorite_deleted_text)
                } else {
                    true to context.getString(R.string.favorite_does_not_exist_text)
                }
            } else {
                val stop = Stop(
                    desc = stopName,
                    latitude = lat,
                    longitude = lng,
                    routeNum = if (routeId > 0) routeId else 0,
                    locId = locId
                )
                if (favoritesRepository.addFavorite(stop)) {
                    true to context.getString(R.string.favorite_added_text)
                } else {
                    true to context.getString(R.string.favorite_exists_text)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle favorite")
            false to context.getString(R.string.failed_to_update_favorite)
        }
    }
}