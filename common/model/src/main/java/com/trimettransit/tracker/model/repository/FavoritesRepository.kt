package com.trimettransit.tracker.model.repository

import com.trimettransit.tracker.model.Stop

/**
 * Data-access boundary for stops the user has favorited. Implementations live in
 * `component:localdata`.
 */
interface FavoritesRepository {
    suspend fun getFavorites(): List<Stop>
    suspend fun addFavorite(stop: Stop): Boolean
    suspend fun removeFavorite(locId: Int): Boolean
    suspend fun isFavorite(locId: Int): Boolean
}
