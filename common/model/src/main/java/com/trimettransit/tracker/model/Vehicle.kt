package com.trimettransit.tracker.model

/**
 * A single vehicle position from TriMet's Vehicle location webservice
 * (`/ws/v2/vehicles`). Field names mirror the API's JSON attributes.
 */
data class Vehicle(
    val vehicleID: Int = 0,
    val type: String = "",
    val blockID: Int = 0,
    val tripID: String = "",
    val routeNumber: Int = 0,
    val direction: Int = 0,
    val newTrip: Boolean = false,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val bearing: Float = 0f,
    val time: Long = 0,
    val expires: Long = 0,
    val serviceDate: Long = 0,
    val locationInScheduleDay: Int = 0,
    val delay: Int = 0,
    val signMessage: String = "",
    val nextLocID: Int = 0,
    val lastLocID: Int = 0,
    val garage: String = "",
    val inCongestion: Boolean = false,
    val loadPercentage: Int = -1
)
