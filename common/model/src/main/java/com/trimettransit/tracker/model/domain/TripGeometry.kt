package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Stop

/**
 * Pure helpers that turn route config stop sequences into drawable route
 * geometry for the trip planner map. Kept free of Android/map dependencies so
 * the matching and slicing logic is directly unit-testable.
 */

/**
 * Picks the route config direction that matches a trip planner leg's direction
 * label (e.g. "Southbound" matches "Southbound to Tualatin Park & Ride").
 * Returns null only when the list is empty; unknown labels fall back to the
 * first direction so callers can keep drawing.
 */
fun matchDirection(directions: List<Direction>, legDirection: String): Direction? {
    if (directions.isEmpty()) return null
    val needle = legDirection.trim().lowercase()
    if (needle.isEmpty()) return directions.first()
    return directions.firstOrNull {
        val haystack = it.desc.trim().lowercase()
        haystack.startsWith(needle) || haystack.contains(needle)
    } ?: directions.first()
}

/**
 * Slices a route's ordered stop sequence to the span between the leg's boarding
 * and alighting points (each matched by nearest stop). Order is always returned
 * in the sequence's own direction so the polyline never self-intersects; the
 * whole sequence is returned when the leg sits on a single stop.
 */
fun sliceStopsForLeg(
    stops: List<Stop>,
    fromLat: Double,
    fromLng: Double,
    toLat: Double,
    toLng: Double
): List<Stop> {
    if (stops.size < 2) return stops
    val fromIndex = nearestStopIndex(stops, fromLat, fromLng)
    val toIndex = nearestStopIndex(stops, toLat, toLng)
    if (fromIndex == toIndex) return stops
    val start = minOf(fromIndex, toIndex)
    val end = maxOf(fromIndex, toIndex)
    return stops.subList(start, end + 1)
}

private fun nearestStopIndex(stops: List<Stop>, lat: Double, lng: Double): Int =
    stops.indices.minByOrNull { index ->
        val stop = stops[index]
        val dLat = stop.latitude - lat
        val dLng = stop.longitude - lng
        dLat * dLat + dLng * dLng
    } ?: 0