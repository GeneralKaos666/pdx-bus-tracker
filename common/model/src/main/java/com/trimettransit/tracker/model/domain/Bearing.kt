package com.trimettransit.tracker.model.domain

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * The eight compass points, declared in sector order starting at north. Order matters:
 * [compassDirection] indexes straight into [entries].
 */
enum class CompassDirection { N, NE, E, SE, S, SW, W, NW }

/**
 * Initial great-circle bearing from one coordinate to another, in degrees clockwise from
 * true north, normalised to `0.0 until 360.0`.
 *
 * Note that the bearing is undefined when the two coordinates coincide; that returns `0.0`.
 * Callers should treat "no distance" as "no bearing" rather than printing a compass point
 * for a stop the user is standing on.
 */
fun bearingDegrees(
    fromLatitude: Double,
    fromLongitude: Double,
    toLatitude: Double,
    toLongitude: Double
): Double {
    val fromLatRadians = Math.toRadians(fromLatitude)
    val toLatRadians = Math.toRadians(toLatitude)
    val deltaLongitudeRadians = Math.toRadians(toLongitude - fromLongitude)

    val y = sin(deltaLongitudeRadians) * cos(toLatRadians)
    val x = cos(fromLatRadians) * sin(toLatRadians) -
        sin(fromLatRadians) * cos(toLatRadians) * cos(deltaLongitudeRadians)

    val degrees = Math.toDegrees(atan2(y, x))
    return (degrees + 360.0) % 360.0
}

/**
 * The compass point a [bearingDegrees] value points at. Each point owns the 45° sector centred
 * on it, so north spans `337.5` up to but not including `22.5` and no bearing falls between
 * two points. Bearings outside one turn are wrapped rather than rejected.
 */
fun compassDirection(bearingDegrees: Double): CompassDirection {
    val normalised = (bearingDegrees % 360.0 + 360.0) % 360.0
    val index = ((normalised / SECTOR_DEGREES) + 0.5).toInt() % CompassDirection.entries.size
    return CompassDirection.entries[index]
}

private const val SECTOR_DEGREES = 45.0
