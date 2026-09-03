package com.trimettransit.tracker.model

import org.joda.time.DateTime

data class Arrival(
    val fullSign: String = "",
    val shortSign: String = "",
    val estimated: DateTime? = null,
    val scheduled: DateTime? = null,
    val routeId: Int = 0,
    val status: String = "",
    val dropOffOnly: Boolean = false,
    val reason: String = "",
    val tripID: String = "",
    val blockID: Int = 0,
    val vehicleID: Int = 0,
    val feet: Int = 0,
    val dir: Int = 0,
    val estimatedMillis: Long = 0,
    val scheduledMillis: Long = 0
)
