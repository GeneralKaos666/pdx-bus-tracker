package com.trimettransit.tracker.model

data class Detour(
    val id: Int = 0,
    val desc: String = "",
    val routes: List<Int> = emptyList()
)
