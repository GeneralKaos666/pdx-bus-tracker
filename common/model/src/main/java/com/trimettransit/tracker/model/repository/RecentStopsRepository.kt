package com.trimettransit.tracker.model.repository

import com.trimettransit.tracker.model.Stop

/**
 * Data-access boundary for the user's recently visited stops. Implementations
 * live in `component:localdata`.
 */
interface RecentStopsRepository {
    suspend fun getRecentStops(): List<Stop>
    suspend fun addRecentStop(stop: Stop)
}
