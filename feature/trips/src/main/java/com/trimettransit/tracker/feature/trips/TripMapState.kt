package com.trimettransit.tracker.feature.trips

import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripLeg
import com.trimettransit.tracker.model.TripPoint
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * Holds the trip-planning map's GeoJSON sources and pushes render data into them. The
 * style/images/layers are registered once by the composable; this class only swaps
 * feature geometry. [letterColors] maps a transit badge letter ("B"/"M"/"S"/"W") to the
 * "#rrggbb" line color resolved from the current M3 scheme, set on style load.
 */
internal class TripMapState {
    var mapView: MapView? = null
    var map: MapLibreMap? = null
    var letterColors: Map<String, String> = emptyMap()
    var originSource: GeoJsonSource? = null
    var destSource: GeoJsonSource? = null
    var transitSource: GeoJsonSource? = null
    var walkSource: GeoJsonSource? = null
    var stopSource: GeoJsonSource? = null
    var boardSource: GeoJsonSource? = null
    var meSource: GeoJsonSource? = null
    var lastFitTag: FitTag? = null

    /**
     * Real route polyline coordinates (lat/lng), keyed by itinerary leg index. The Trip
     * Planner WS returns no geometry, so transit legs normally render as straight "sticks";
     * when a leg's route+direction stop sequence is known we slice it between boarding and
     * alighting and draw the actual road-following line instead.
     */
    var legGeometries: Map<Int, List<GeoPoint>> = emptyMap()

    /** Identity of the plan the camera was last fitted to; lets the composable skip re-fitting
     *  on recompositions that don't change the trip (location fixes, picker toggles, theme). */
    data class FitTag(val origin: TripPoint?, val dest: TripPoint?, val itinerary: TripItinerary?)

    fun applyMe(lat: Double, lng: Double) {
        meSource?.setGeoJson(
            FeatureCollection.fromFeatures(listOf(pointFeature(lng, lat)))
        )
    }

    private data class GeoRawPoint(val lng: Double, val lat: Double)

    /** Pushes origin/destination markers and the selected itinerary's route lines. The Trip
     *  Planner WS returns no geometry, so transit legs render as straight "sticks" between their
* boarding and alighting points by default; when a leg's route geometry was resolved, the
     * polyline spanning board→alight is drawn through the route's real stop sequence. */
    fun push(origin: TripPoint?, dest: TripPoint?, itinerary: TripItinerary?) {
        originSource?.let { source ->
            source.setGeoJson(
                FeatureCollection.fromFeatures(
                    listOfNotNull(origin?.let { pointFeature(it.longitude, it.latitude) })
                )
            )
        }
        destSource?.let { source ->
            source.setGeoJson(
                FeatureCollection.fromFeatures(
                    listOfNotNull(dest?.let { pointFeature(it.longitude, it.latitude) })
                )
            )
        }
        val legs = itinerary?.legs.orEmpty()
        transitSource?.let { source ->
            source.setGeoJson(
                FeatureCollection.fromFeatures(
                    legs.mapIndexedNotNull { index, leg ->
                        if (!leg.isWalk) transitLineFeature(leg, legGeometries[index]) else null
                    }
                )
            )
        }
        walkSource?.let { source ->
            source.setGeoJson(
                FeatureCollection.fromFeatures(
                    legs.filter { it.isWalk }.mapNotNull { walkLineFeature(it) }
                )
            )
        }
        stopSource?.let { source ->
            // Boarding points carry a badge marker, so plain stop dots cover walk segment
            // endpoints, alighting points, and the intermediate stops of the drawn geometry.
            val rawPoints = mutableListOf<GeoRawPoint>()
            legs.forEachIndexed { index, leg ->
                if (leg.isWalk) {
                    rawPoints += GeoRawPoint(leg.from.longitude, leg.from.latitude)
                    rawPoints += GeoRawPoint(leg.to.longitude, leg.to.latitude)
                } else {
                    rawPoints += GeoRawPoint(leg.to.longitude, leg.to.latitude)
                }
                legGeometries[index]?.forEach { point ->
                    if (point.latitude != 0.0 || point.longitude != 0.0) {
                        rawPoints += GeoRawPoint(point.longitude, point.latitude)
                    }
                }
            }
            source.setGeoJson(
                FeatureCollection.fromFeatures(
                    rawPoints
                        .filter { it.lat != 0.0 || it.lng != 0.0 }
                        .distinct()
                        .map { pointFeature(it.lng, it.lat) }
                )
            )
        }
        boardSource?.let { source ->
            source.setGeoJson(
                FeatureCollection.fromFeatures(
                    legs.filter { !it.isWalk }
                        .filter { it.from.latitude != 0.0 || it.from.longitude != 0.0 }
                        .mapNotNull { boardFeature(it) }
                )
            )
        }
    }

    private fun transitLineFeature(leg: TripLeg, geometry: List<GeoPoint>?): Feature? {
        val polygon = geometry?.map { Point.fromLngLat(it.longitude, it.latitude) }
        val feature = if (polygon != null && polygon.size >= 2) {
            Feature.fromGeometry(LineString.fromLngLats(polygon))
        } else if (leg.hasUsableEndpoints()) {
            Feature.fromGeometry(lineSegment(leg))
        } else {
            return null
        }
        feature.addStringProperty("color", letterColors[leg.mode.transitTypeLetter()] ?: "#888888")
        return feature
    }

    private fun walkLineFeature(leg: TripLeg): Feature? {
        if (!leg.hasUsableEndpoints()) return null
        return Feature.fromGeometry(lineSegment(leg))
    }

    /** A map leg is only drawable when at least one endpoint has real coordinates. */
    private fun TripLeg.hasUsableEndpoints(): Boolean =
        (from.latitude != 0.0 || from.longitude != 0.0) || (to.latitude != 0.0 || to.longitude != 0.0)

    private fun lineSegment(leg: TripLeg): LineString =
        LineString.fromLngLats(
            listOf(
                Point.fromLngLat(leg.from.longitude, leg.from.latitude),
                Point.fromLngLat(leg.to.longitude, leg.to.latitude)
            )
        )

    private fun boardFeature(leg: TripLeg): Feature {
        val feature = pointFeature(leg.from.longitude, leg.from.latitude)
        feature.addStringProperty("icon", "badge-${leg.mode.transitTypeLetter()}")
        return feature
    }

    private fun pointFeature(lng: Double, lat: Double): Feature =
        Feature.fromGeometry(Point.fromLngLat(lng, lat))
}

/** A plain lat/lng pair used to carry route geometry to the map (model-agnostic of maplibre). */
internal data class GeoPoint(val latitude: Double, val longitude: Double)