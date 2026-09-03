package com.trimettransit.tracker.model

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
            isWes -> "W"
            isMax || desc.contains("Vintage Trolley") -> "M"
            isBus -> "B"
            isStreetcar -> "S"
            else -> "Z"
        }
}
