package com.trimettransit.tracker.model

enum class TripStopStatus {
    NORMAL,
    CANCELED,
    DROP_OFF_ONLY;

    companion object {
        fun fromApiValue(value: String): TripStopStatus = when (value.trim().lowercase()) {
            "canceled", "cancelled" -> CANCELED
            "dropoffonly", "drop_off_only", "drop-off-only" -> DROP_OFF_ONLY
            else -> NORMAL
        }
    }
}

data class TripStopStatusInfo(
    val locId: Int = 0,
    val stopSequence: Int = 0,
    val description: String = "",
    val direction: String = "",
    val isPassed: Boolean = false,
    val passengerAccessible: Boolean = true,
    val distanceFeet: Double = 0.0,
    val aimedArrivalMillis: Long = 0L,
    val aimedDepartureMillis: Long = 0L,
    val arrivalDelaySeconds: Int? = null,
    val departureDelaySeconds: Int? = null,
    val estimated: Boolean? = null,
    val adjustedArrivalMillis: Long? = null,
    val adjustedDepartureMillis: Long? = null,
    val status: TripStopStatus = TripStopStatus.NORMAL
) {
    val passed: Boolean get() = isPassed
    val canceled: Boolean get() = isCanceled
    val dropOffOnly: Boolean get() = isDropOffOnly
    val isCanceled: Boolean get() = status == TripStopStatus.CANCELED
    val isDropOffOnly: Boolean get() = status == TripStopStatus.DROP_OFF_ONLY
    val effectiveArrivalMillis: Long
        get() = adjustedArrivalMillis ?: aimedArrivalMillis
    val effectiveDepartureMillis: Long
        get() = adjustedDepartureMillis ?: aimedDepartureMillis
}

data class TripStatus(
    val tripId: String = "",
    val blockId: Int = 0,
    val routeNumber: Int = 0,
    val distanceFeet: Double = 0.0,
    val progressFeet: Double? = null,
    val pattern: Int = 0,
    val destination: String = "",
    val blockSchedulePositionSeconds: Int? = null,
    val extra: Boolean = false,
    val tripBeginMillis: Long = 0L,
    val tripEndMillis: Long = 0L,
    val scheduleDayMillis: Long = 0L,
    val modified: Boolean = false,
    val direction: Int = 0,
    val stops: List<TripStopStatusInfo> = emptyList()
) {
    val tripID: String get() = tripId
    val blockID: Int get() = blockId
    val extraTrip: Boolean get() = extra
    val isModified: Boolean get() = modified
    val progress: Float?
        get() = progressFeet?.let { progress ->
            if (distanceFeet > 0.0) (progress / distanceFeet).coerceIn(0.0, 1.0).toFloat() else null
        }
}

data class TripStatusResult(
    val queryTimeMillis: Long = 0L,
    val trips: List<TripStatus> = emptyList()
)

data class BlockStatus(
    val blockId: Int = 0,
    val currentTripId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val bearing: Float? = null,
    val positionTimestampMillis: Long? = null,
    val deviationSeconds: Int? = null,
    val vehicleId: Int? = null,
    val schedulePositionMillis: Long? = null,
    val trips: List<TripStatus> = emptyList()
) {
    val currentTripID: String? get() = currentTripId
    val vehicleID: Int? get() = vehicleId
    val positionTimestamp: Long? get() = positionTimestampMillis
}

data class BlockStatusResult(
    val queryTimeMillis: Long = 0L,
    val blocks: List<BlockStatus> = emptyList()
)
