package com.trimettransit.tracker.model

data class Stop(
    val desc: String = "",
    val dirDesc: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val transitType: String = "",
    val routeNum: Int = 0,
    val locId: Int = 0,
    val routes: List<Route> = emptyList()
)

/**
 * Derives a stop's transit type letter from its routes, matching the behavior of
 * the former `Stop.computeTransitType()`. Returns "Z" (unknown) if no route matches.
 */
fun computeTransitType(routes: List<Route>): String {
    for (route in routes) {
        when {
            route.isStreetcar -> return "S"
            route.isBus && !route.desc.contains("Shuttle") -> return "B"
            route.isMax || route.desc.contains("Vintage Trolley") -> return "M"
            route.isWes -> return "W"
        }
    }
    return "Z"
}
