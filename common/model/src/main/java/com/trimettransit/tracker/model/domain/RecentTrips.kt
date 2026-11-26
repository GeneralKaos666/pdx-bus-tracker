package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripPoint

/** How many recent destinations the trip planner remembers. */
const val MAX_RECENT_TRIPS = 5

private const val FIELD = '\t'
private const val LINE = '\n'

/**
 * Serializes remembered trip destinations as tab-separated `description, latitude,
 * longitude` lines. Descriptions are cleaned of the separators so a malformed or hostile
 * value can never shift the field layout.
 */
fun encodeRecentTrips(trips: List<TripPoint>): String =
    trips.take(MAX_RECENT_TRIPS).joinToString(LINE.toString()) { trip ->
        listOf(
            cleanField(trip.description),
            trip.latitude.toString(),
            trip.longitude.toString()
        ).joinToString(FIELD.toString())
    }

/** Reads [encodeRecentTrips] output, dropping lines that are malformed or not valid points. */
fun decodeRecentTrips(raw: String?): List<TripPoint> {
    if (raw.isNullOrBlank()) return emptyList()
    return raw.lineSequence()
        .mapNotNull { line ->
            val parts = line.split(FIELD)
            if (parts.size != 3) return@mapNotNull null
            val latitude = parts[1].toDoubleOrNull() ?: return@mapNotNull null
            val longitude = parts[2].toDoubleOrNull() ?: return@mapNotNull null
            TripPoint(latitude, longitude, parts[0]).takeIf { it.isValid }
        }
        .take(MAX_RECENT_TRIPS)
        .toList()
}

/** Most-recent-first, de-duplicated by coordinates, capped at [max]. */
fun addRecentTrip(
    trips: List<TripPoint>,
    trip: TripPoint,
    max: Int = MAX_RECENT_TRIPS
): List<TripPoint> {
    if (!trip.isValid || max <= 0) return trips
    val key = trip.latitude to trip.longitude
    return (listOf(trip) + trips.filterNot { it.latitude to it.longitude == key }).take(max)
}

private fun cleanField(text: String): String =
    text.replace(FIELD, ' ').replace(LINE, ' ').trim()
