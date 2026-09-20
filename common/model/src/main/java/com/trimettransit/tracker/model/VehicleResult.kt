package com.trimettransit.tracker.model

/**
 * Parsed result of a vehicle location fetch. [queryTime] is TriMet's `@queryTime`
 * (epoch millis), suitable as the `since` value for a subsequent poll.
 */
data class VehicleResult(
    val vehicles: List<Vehicle> = emptyList(),
    val queryTime: Long = 0
)
