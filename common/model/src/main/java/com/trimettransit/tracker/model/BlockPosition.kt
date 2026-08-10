package com.trimettransit.tracker.model

data class BlockPosition(
    var id: Int = 0,
    var at: Long = 0,
    var vehicleID: Int = 0,
    var feet: Int = 0,
    var bearing: Float = 0f,
    var lat: Double = 0.0,
    var lng: Double = 0.0,
    var routeNumber: Int = 0,
    var direction: Int = 0,
    var tripID: String = "",
    var isNewTrip: Boolean = false
)
