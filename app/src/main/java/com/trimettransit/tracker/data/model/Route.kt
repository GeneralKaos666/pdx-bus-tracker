package com.trimettransit.tracker.data.model

data class Route(
    var desc: String = "",
    var routeId: Int = 0,
    var isBus: Boolean = false,
    var isMax: Boolean = false,
    var isStreetcar: Boolean = false,
    var isWes: Boolean = false
) {
    val typeLetter: String
        get() = when {
            isWes -> "W"
            isMax -> "M"
            isBus -> "B"
            isStreetcar -> "S"
            else -> "Z"
        }
}
