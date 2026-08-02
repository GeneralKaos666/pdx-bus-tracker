package com.trimettransit.tracker.model

data class Direction(
    var dir: Int = 0,
    var desc: String = "",
    var route: Route? = null
)
