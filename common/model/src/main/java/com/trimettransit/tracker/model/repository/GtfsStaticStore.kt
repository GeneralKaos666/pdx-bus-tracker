package com.trimettransit.tracker.model.repository

import com.trimettransit.tracker.model.GtfsStaticSnapshot

interface GtfsStaticStore {
    suspend fun read(): GtfsStaticSnapshot?
    suspend fun refresh(): GtfsStaticSnapshot
    suspend fun refreshIfStale(maxAgeMillis: Long): GtfsStaticSnapshot?
}
