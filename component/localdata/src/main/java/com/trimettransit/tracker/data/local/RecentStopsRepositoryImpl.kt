package com.trimettransit.tracker.data.local

import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.RecentStopsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Adapter exposing [DatabaseHelper]'s recent-stops operations behind the
 * [RecentStopsRepository] boundary.
 */
class RecentStopsRepositoryImpl(
    private val dbHelper: DatabaseHelper
) : RecentStopsRepository {

    override suspend fun getRecentStops(): List<Stop> = withContext(Dispatchers.IO) {
        dbHelper.recentStops
    }

    override suspend fun addRecentStop(stop: Stop) = withContext(Dispatchers.IO) {
        dbHelper.addRecentStop(stop)
    }

    override suspend fun removeRecent(locId: Int): Boolean = withContext(Dispatchers.IO) {
        dbHelper.removeRecentStop(locId)
    }

    override suspend fun clearRecents() = withContext(Dispatchers.IO) {
        dbHelper.clearRecentStops()
    }
}
