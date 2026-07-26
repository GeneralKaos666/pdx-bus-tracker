package com.trimettransit.tracker.data.model

import org.joda.time.DateTime

data class Arrival(
    var fullSign: String = "",
    var shortSign: String = "",
    var estimated: DateTime? = null,
    var scheduled: DateTime? = null,
    var detours: MutableList<Detour>? = null,
    var routeId: Int = 0,
    var status: String = "",
    var dropOffOnly: Boolean = false,
    var reason: String = "",
    var tripID: String = "",
    var blockID: Int = 0,
    var vehicleID: Int = 0,
    var feet: Int = 0,
    var dir: Int = 0,
    var estimatedMillis: Long = 0,
    var scheduledMillis: Long = 0
)
