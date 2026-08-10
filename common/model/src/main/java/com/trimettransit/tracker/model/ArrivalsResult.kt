package com.trimettransit.tracker.model

data class ArrivalsResult(
    var arrivals: MutableList<Arrival>? = mutableListOf(),
    var blockPositions: MutableList<BlockPosition>? = mutableListOf(),
    var detours: MutableList<Detour>? = mutableListOf(),
    var stopLat: Double = 0.0,
    var stopLng: Double = 0.0
)
