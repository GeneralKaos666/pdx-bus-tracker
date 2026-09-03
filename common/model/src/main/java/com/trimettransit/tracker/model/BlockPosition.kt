package com.trimettransit.tracker.model

data class BlockPosition(
    val id: Int = 0,
    val at: Long = 0,
    val vehicleID: Int = 0,
    val feet: Int = 0,
    val bearing: Float = 0f,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val routeNumber: Int = 0,
    val direction: Int = 0,
    val tripID: String = "",
    val isNewTrip: Boolean = false
)
