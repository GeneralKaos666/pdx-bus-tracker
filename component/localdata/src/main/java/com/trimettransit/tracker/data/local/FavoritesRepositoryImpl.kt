package com.trimettransit.tracker.data.local

import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Adapter exposing [DatabaseHelper]'s favorites operations behind the
 * [FavoritesRepository] boundary.
 */
class FavoritesRepositoryImpl(
    private val dbHelper: DatabaseHelper
) : FavoritesRepository {

    override suspend fun getFavorites(): List<Stop> = withContext(Dispatchers.IO) {
        dbHelper.favorites
    }

    override suspend fun addFavorite(stop: Stop): Boolean = withContext(Dispatchers.IO) {
        dbHelper.addFavorite(stop)
    }

    override suspend fun removeFavorite(locId: Int): Boolean = withContext(Dispatchers.IO) {
        dbHelper.removeFavorite(locId)
    }

    override suspend fun isFavorite(locId: Int): Boolean = withContext(Dispatchers.IO) {
        dbHelper.isFavorite(locId)
    }
}
