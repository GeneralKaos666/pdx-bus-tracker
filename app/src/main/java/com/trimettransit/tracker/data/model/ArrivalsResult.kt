package com.trimettransit.tracker.data.model

data class ArrivalsResult(
    var arrivals: List<Arrival>? = null,
    var isQueryError: Boolean = false,
    var blockPositions: List<BlockPosition>? = null,
    var detours: List<Detour>? = null,
    var stopLat: Double = 0.0,
    var stopLng: Double = 0.0
)
