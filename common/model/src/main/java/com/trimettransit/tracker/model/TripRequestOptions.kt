package com.trimettransit.tracker.model

/**
 * Service-wide travel mode for a Trip Planner request, mapped from the tripplanner WS
 * `mode` parameter: All Modes (A), Bus Only (B), Train Only (T).
 */
enum class TripPlannerMode(val wsCode: String) {
    ALL("A"),
    BUS("B"),
    TRAIN("T")
}

/**
 * Tunable options for a trip-plan request, serialized into the tripplanner WS URL path.
 * Defaults reproduce the parameters the app has always sent: all modes, 0.5 mile walking
 * distance, up to 3 itineraries.
 */
data class TripRequestOptions(
    val mode: TripPlannerMode = TripPlannerMode.ALL,
    /** Maximum walking distance in miles (the WS accepts 0.01..0.999). */
    val maxWalkMiles: Float = 0.5f,
    /** How many itineraries to request (the WS accepts 1..6). */
    val itineraryCount: Int = 3
) {
    init {
        require(maxWalkMiles in 0.01f..0.999f) {
            "maxWalkMiles must be within 0.01..0.999, got $maxWalkMiles"
        }
        require(itineraryCount in 1..6) {
            "itineraryCount must be within 1..6, got $itineraryCount"
        }
    }
}