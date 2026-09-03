package com.trimettransit.tracker.model

data class Direction(
    val dir: Int = 0,
    val desc: String = "",
    val route: Route? = null
)
