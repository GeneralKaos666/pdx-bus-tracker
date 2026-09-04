package com.trimettransit.tracker.feature.trips

import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripLeg
import com.trimettransit.tracker.model.TripPoint
import org.maplibre.android.geometry.LatLng
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
    var lastMe: LatLng? = null

    fun applyMe(lat: Double, lng: Double) {
        lastMe = LatLng(lat, lng)
        meSource?.setGeoJson(
            FeatureCollection.fromFeatures(listOf(pointFeature(lng, lat)))
        )
    }

    /** Pushes origin/destination markers and the selected itinerary's route lines. The Trip
     *  Planner WS returns no geometry, so transit legs render as straight "sticks" between their
     *  boarding and alighting points (dashed for walks) with badge markers at each boarding point. */
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
                    legs.filter { !it.isWalk }.mapNotNull { transitLineFeature(it) }
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
            source.setGeoJson(
                FeatureCollection.fromFeatures(
                    legs.flatMap { listOf(it.from, it.to) }
                        .filter { it.latitude != 0.0 || it.longitude != 0.0 }
                        .map { pointFeature(it.longitude, it.latitude) }
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

    private fun transitLineFeature(leg: TripLeg): Feature? {
        if (leg.from.latitude == 0.0 && leg.from.longitude == 0.0 &&
            leg.to.latitude == 0.0 && leg.to.longitude == 0.0
        ) return null
        val feature = Feature.fromGeometry(lineSegment(leg))
        feature.addStringProperty("color", letterColors[leg.mode.transitTypeLetter()] ?: "#888888")
        return feature
    }

    private fun walkLineFeature(leg: TripLeg): Feature? {
        if (leg.from.latitude == 0.0 && leg.from.longitude == 0.0 &&
            leg.to.latitude == 0.0 && leg.to.longitude == 0.0
        ) return null
        return Feature.fromGeometry(lineSegment(leg))
    }

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