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

    /**
     * Every favourited stop id, as a set, so list rows can render a favourite toggle without a
     * query per row. Re-read after a toggle rather than cached across screens.
     */
    suspend fun favoriteIds(): Set<Int>

    suspend fun setOrder(idsInOrder: List<Int>)
}
