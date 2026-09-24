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
    /**
     * Deliberately never populated. TriMet's `shapes.txt` is ~1.08 million rows / 44 MB, and
     * building a `Map` per row exhausts the app's 256 MB heap — so [GtfsStaticParser] does not
     * read that table at all. Nothing consumes this field today; if shape geometry is ever
     * wanted, parse it streaming and keep only what a screen needs.
     */
    val shapes: List<GtfsShapePoint> = emptyList(),
    val fetchedAtMillis: Long = 0L
)
