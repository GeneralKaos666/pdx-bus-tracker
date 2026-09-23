package com.trimettransit.tracker.model

data class Stop(
    val desc: String = "",
    val dirDesc: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    /**
     * Distance in feet from the coordinates the stop was queried with, as reported by the
     * location endpoints. `0.0` means unknown (the stop did not come from a location query),
     * which is also why it is an ordering key that sorts last.
     */
    val distanceFeet: Double = 0.0,
    val transitType: String = "",
    val routeNum: Int = 0,
    val locId: Int = 0,
    val routes: List<Route> = emptyList()
)

/**
 * Derives a stop's transit type letter from its routes, matching the behavior of
 * the former `Stop.computeTransitType()`. A shuttle bus never masks a real rail
 * type, but a stop served only by a shuttle still reads "B" (matching the route's
 * own [Route.typeLetter]) instead of the generic "Z". Returns "Z" when no route
 * matches.
 */
fun computeTransitType(routes: List<Route>): String {
    var hasShuttleBus = false
    for (route in routes) {
        hasShuttleBus = hasShuttleBus || route.isBus
        when {
            route.isStreetcar -> return TransitTypeLetter.STREETCAR
            route.isBus && !route.desc.contains("Shuttle") -> return TransitTypeLetter.BUS
            route.isMax || route.desc.contains("Vintage Trolley") -> return TransitTypeLetter.MAX
            route.isWes -> return TransitTypeLetter.WES
        }
    }
    return if (hasShuttleBus) TransitTypeLetter.BUS else TransitTypeLetter.NONE
}
