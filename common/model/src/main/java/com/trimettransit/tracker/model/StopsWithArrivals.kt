package com.trimettransit.tracker.model

/**
 * Result of a combined "stops near a point, with their arrivals" fetch. Pairs
 * [stops] with each stop's already-fetched, deduped arrivals in [arrivalsByStop]
 * (keyed by [Stop.locId]), so a caller like the nearby-stops list can show a next-
 * arrival preview without a second per-stop network round trip.
 *
 * A [Stop.locId] absent from [arrivalsByStop] means the combined response didn't
 * include arrivals for that stop (e.g. it had none within the requested window) —
 * callers should treat that as "no arrivals", not as a fetch failure.
 */
data class StopsWithArrivals(
    val stops: List<Stop>,
    val arrivalsByStop: Map<Int, List<Arrival>> = emptyMap()
)
