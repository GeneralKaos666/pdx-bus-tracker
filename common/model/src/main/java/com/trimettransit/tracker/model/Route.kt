package com.trimettransit.tracker.model

/**
 * The single source of truth for the one-letter transit-type badges rendered in
 * route lists, stop markers, and trip-leg pills. Precedence for a route with
 * multiple flags: WES, MAX, bus, streetcar (see [Route.typeLetter]).
 */
object TransitTypeLetter {
    const val BUS = "B"
    const val MAX = "M"
    const val STREETCAR = "S"
    const val WES = "W"
    const val NONE = "Z"
}

data class Route(
    val desc: String = "",
    val routeId: Int = 0,
    val isBus: Boolean = false,
    val isMax: Boolean = false,
    val isStreetcar: Boolean = false,
    val isWes: Boolean = false
) {
    val typeLetter: String
        get() = when {
            isWes -> TransitTypeLetter.WES
            isMax || desc.contains("Vintage Trolley") -> TransitTypeLetter.MAX
            isBus -> TransitTypeLetter.BUS
            isStreetcar -> TransitTypeLetter.STREETCAR
            else -> TransitTypeLetter.NONE
        }
}
