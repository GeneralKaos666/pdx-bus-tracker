package com.trimettransit.tracker.model

data class ArrivalsResult(
    val arrivals: List<Arrival> = emptyList(),
    val blockPositions: List<BlockPosition> = emptyList(),
    val detours: List<Detour> = emptyList(),
    val stopLat: Double = 0.0,
    val stopLng: Double = 0.0
)
