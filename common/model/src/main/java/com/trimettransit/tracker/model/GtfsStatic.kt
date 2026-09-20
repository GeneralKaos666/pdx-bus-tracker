package com.trimettransit.tracker.model

data class GtfsRoute(
    val id: String,
    val shortName: String,
    val longName: String,
    val type: Int
)

data class GtfsStop(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

data class GtfsShapePoint(
    val shapeId: String,
    val latitude: Double,
    val longitude: Double,
    val sequence: Int
)

data class GtfsStaticSnapshot(
    val routes: List<GtfsRoute> = emptyList(),
    val stops: List<GtfsStop> = emptyList(),
    val shapes: List<GtfsShapePoint> = emptyList(),
    val fetchedAtMillis: Long = 0L
)
