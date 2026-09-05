package com.trimettransit.tracker.model

import org.joda.time.DateTime

/** A geographic point used as a trip origin or destination (from/to in a trip plan). */
data class TripPoint(
    val latitude: Double,
    val longitude: Double,
    val description: String = ""
)

/** Transit mode of a single trip leg, mapped from the Trip Planner WS mode code. */
enum class TripLegMode {
    WALK, BUS, LIGHT_RAIL, COMMUTER_RAIL, STREETCAR, RAIL;

    companion object {
        fun fromCode(mode: String): TripLegMode {
            val trimmed = mode.trim()
            trimmed.toIntOrNull()?.let { code ->
                return when (code) {
                    8 -> WALK
                    3 -> BUS
                    10, 0, 4 -> LIGHT_RAIL
                    13 -> STREETCAR
                    11, 2 -> COMMUTER_RAIL
                    else -> RAIL
                }
            }
            return when (trimmed.lowercase()) {
                "walk" -> WALK
                "bus" -> BUS
                "streetcar" -> STREETCAR
                "light rail" -> LIGHT_RAIL
                "commuter rail" -> COMMUTER_RAIL
                else -> RAIL
            }
        }
    }

    /** The badge letter ("B"/"M"/"S") this mode renders as, for route-look coloring. */
    fun transitTypeLetter(): String = when (this) {
        BUS -> "B"
        LIGHT_RAIL, COMMUTER_RAIL, RAIL -> "M"
        STREETCAR -> "S"
        WALK -> "Z"
    }
}

/** One leg of an itinerary: a ride on a route or a stretch of walking. */
data class TripLeg(
    val mode: TripLegMode,
    val routeNumber: String? = null,
    val routeName: String? = null,
    val direction: String = "",
    val from: TripPoint,
    val to: TripPoint,
    /** Null when the WS omits scheduled times (walk legs have none). */
    val departure: DateTime?,
    /** Null when the WS omits scheduled times (walk legs have none). */
    val arrival: DateTime?,
    /** True when this leg continues aboard the same vehicle as the previous one (interline). */
    val stayOnBoard: Boolean = false
) {
    val isWalk: Boolean get() = mode == TripLegMode.WALK
}

/** A single itinerary option returned by the Trip Planner WS. */
data class TripItinerary(
    val id: String = "",
    /** Null for walk-only itineraries, which the WS returns without scheduled times. */
    val departure: DateTime?,
    /** Null for walk-only itineraries, which the WS returns without scheduled times. */
    val arrival: DateTime?,
    val durationMillis: Long,
    val distanceMeters: Double,
    val numberOfTransfers: Int,
    val walkTimeMillis: Long,
    val transitTimeMillis: Long,
    val waitingTimeMillis: Long,
    val fare: String? = null,
    val legs: List<TripLeg> = emptyList()
)

/** The full set of itinerary options for one from→to request. */
data class TripPlan(
    val from: TripPoint,
    val to: TripPoint,
    val itineraries: List<TripItinerary> = emptyList()
)

/** Scheduling choice for a trip request: depart at a time (default = now) or arrive by one. */
data class TripRequestTime(
    val arriveBy: Boolean = false,
    val timeMillis: Long? = null
)

/** Domain errors reported by the Trip Planner WS `<error code="...">`. */
enum class TripPlannerError {
    NO_STOPS_NEAR_ORIGIN, NO_STOPS_NEAR_DESTINATION,
    NO_SERVICE_AT_ORIGIN, NO_SERVICE_AT_DESTINATION,
    TRIP_NOT_POSSIBLE, TRIVIAL_DISTANCE,
    AMBIGUOUS_ORIGIN, AMBIGUOUS_DESTINATION, ORIGIN_NOT_FOUND, DESTINATION_NOT_FOUND,
    OUTSIDE_DISTRICT, SYSTEM_OUTAGE, UNKNOWN;

    companion object {
        fun fromCode(code: String): TripPlannerError {
            return when (code.toIntOrNull()) {
                20003 -> NO_STOPS_NEAR_ORIGIN
                20004 -> NO_STOPS_NEAR_DESTINATION
                20005 -> NO_SERVICE_AT_ORIGIN
                20006 -> NO_SERVICE_AT_DESTINATION
                20007 -> TRIP_NOT_POSSIBLE
                20020, 20021, 20022 -> AMBIGUOUS_ORIGIN
                20023 -> AMBIGUOUS_DESTINATION
                20024 -> ORIGIN_NOT_FOUND
                20025 -> DESTINATION_NOT_FOUND
                20026 -> TRIVIAL_DISTANCE
                21000, 21001 -> OUTSIDE_DISTRICT
                20001, 20002 -> SYSTEM_OUTAGE
                else -> UNKNOWN
            }
        }
    }
}

/** Outcome of a trip-plan request. [plan] is null when the planner returned no itineraries. */
sealed interface TripPlanResult {
    data class Success(val plan: TripPlan?) : TripPlanResult
    data class Error(val error: TripPlannerError) : TripPlanResult
}