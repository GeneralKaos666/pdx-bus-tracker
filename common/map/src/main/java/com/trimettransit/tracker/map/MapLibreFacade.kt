package com.trimettransit.tracker.map

import android.content.Context
import android.graphics.Bitmap
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition as SdkCameraPosition
import org.maplibre.android.camera.CameraUpdateFactory as SdkCameraUpdateFactory
import org.maplibre.android.geometry.LatLng as SdkLatLng
import org.maplibre.android.geometry.LatLngBounds as SdkLatLngBounds
import org.maplibre.android.maps.MapLibreMap as SdkMap
import org.maplibre.android.maps.MapView as SdkMapView
import org.maplibre.android.maps.Style as SdkStyle
import org.maplibre.android.style.expressions.Expression as SdkExpression
import org.maplibre.android.style.layers.LineLayer as SdkLineLayer
import org.maplibre.android.style.layers.Property as SdkProperty
import org.maplibre.android.style.layers.PropertyFactory as SdkPropertyFactory
import org.maplibre.android.style.layers.PropertyValue
import org.maplibre.android.style.layers.SymbolLayer as SdkSymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource as SdkGeoJsonSource
import org.maplibre.geojson.Feature as SdkFeature
import org.maplibre.geojson.FeatureCollection as SdkFeatureCollection
import org.maplibre.geojson.LineString as SdkLineString
import org.maplibre.geojson.Point as SdkPoint

/** Initializes the SDK while keeping application modules free of MapLibre types. */
fun initializeMapRuntime(context: Context) {
    MapLibre.getInstance(context)
    MapLibre.setApiKey("trimet-bus-tracker")
}

data class MapCoordinate(val latitude: Double, val longitude: Double)

class MapPoint private constructor(internal val sdk: SdkPoint) {
    companion object {
        fun fromLngLat(longitude: Double, latitude: Double) =
            MapPoint(SdkPoint.fromLngLat(longitude, latitude))
    }
}

class MapLineString private constructor(internal val sdk: SdkLineString) {
    companion object {
        fun fromLngLats(points: List<MapPoint>) =
            MapLineString(SdkLineString.fromLngLats(points.map { it.sdk }))

        fun fromCoordinates(points: List<MapCoordinate>) =
            fromLngLats(points.map { MapPoint.fromLngLat(it.longitude, it.latitude) })
    }
}

class MapFeature private constructor(internal val sdk: SdkFeature) {
    fun addStringProperty(name: String, value: String) = sdk.addStringProperty(name, value)
    fun addNumberProperty(name: String, value: Number) = sdk.addNumberProperty(name, value)

    companion object {
        fun fromGeometry(point: MapPoint) = MapFeature(SdkFeature.fromGeometry(point.sdk))
        fun fromGeometry(line: MapLineString) = MapFeature(SdkFeature.fromGeometry(line.sdk))
    }
}

class MapFeatureCollection private constructor(internal val sdk: SdkFeatureCollection) {
    companion object {
        fun fromFeatures(features: List<MapFeature>) =
            MapFeatureCollection(SdkFeatureCollection.fromFeatures(features.map { it.sdk }))
    }
}

class MapGeoJsonSource internal constructor(internal val sdk: SdkGeoJsonSource) {
    constructor(id: String) : this(SdkGeoJsonSource(id))
    constructor(id: String, feature: MapFeature) : this(SdkGeoJsonSource(id, feature.sdk))

    fun setGeoJson(features: MapFeatureCollection) {
        sdk.setGeoJson(features.sdk)
    }
}

class MapExpression private constructor(internal val sdk: SdkExpression) {
    companion object {
        fun get(property: String) = MapExpression(SdkExpression.get(property))
    }
}

class MapProperty internal constructor(internal val sdk: PropertyValue<*>)

object MapPropertyConstants {
    const val ICON_ANCHOR_CENTER = SdkProperty.ICON_ANCHOR_CENTER
    const val ICON_ROTATION_ALIGNMENT_MAP = SdkProperty.ICON_ROTATION_ALIGNMENT_MAP
    const val TEXT_ANCHOR_BOTTOM = SdkProperty.TEXT_ANCHOR_BOTTOM
    const val LINE_CAP_ROUND = SdkProperty.LINE_CAP_ROUND
    const val LINE_JOIN_ROUND = SdkProperty.LINE_JOIN_ROUND
}

object MapProperties {
    fun iconImage(value: String) = MapProperty(SdkPropertyFactory.iconImage(value))
    fun iconImage(value: MapExpression) = MapProperty(SdkPropertyFactory.iconImage(value.sdk))
    fun iconAnchor(value: String) = MapProperty(SdkPropertyFactory.iconAnchor(value))
    fun iconAllowOverlap(value: Boolean) = MapProperty(SdkPropertyFactory.iconAllowOverlap(value))
    fun iconIgnorePlacement(value: Boolean) = MapProperty(SdkPropertyFactory.iconIgnorePlacement(value))
    fun iconRotate(value: MapExpression) = MapProperty(SdkPropertyFactory.iconRotate(value.sdk))
    fun iconRotationAlignment(value: String) = MapProperty(SdkPropertyFactory.iconRotationAlignment(value))
    fun textField(value: MapExpression) = MapProperty(SdkPropertyFactory.textField(value.sdk))
    fun textAnchor(value: String) = MapProperty(SdkPropertyFactory.textAnchor(value))
    fun textOffset(value: Array<Float>) = MapProperty(SdkPropertyFactory.textOffset(value))
    fun textSize(value: Float) = MapProperty(SdkPropertyFactory.textSize(value))
    fun textColor(value: Int) = MapProperty(SdkPropertyFactory.textColor(value))
    fun textHaloColor(value: Int) = MapProperty(SdkPropertyFactory.textHaloColor(value))
    fun textHaloWidth(value: Float) = MapProperty(SdkPropertyFactory.textHaloWidth(value))
    fun textAllowOverlap(value: Boolean) = MapProperty(SdkPropertyFactory.textAllowOverlap(value))
    fun textIgnorePlacement(value: Boolean) = MapProperty(SdkPropertyFactory.textIgnorePlacement(value))
    fun textFont(value: Array<String>) = MapProperty(SdkPropertyFactory.textFont(value))
    fun lineColor(value: Int) = MapProperty(SdkPropertyFactory.lineColor(value))
    fun lineColor(value: MapExpression) = MapProperty(SdkPropertyFactory.lineColor(value.sdk))
    fun lineWidth(value: Float) = MapProperty(SdkPropertyFactory.lineWidth(value))
    fun lineCap(value: String) = MapProperty(SdkPropertyFactory.lineCap(value))
    fun lineJoin(value: String) = MapProperty(SdkPropertyFactory.lineJoin(value))
    fun lineDasharray(value: Array<Float>) = MapProperty(SdkPropertyFactory.lineDasharray(value))
}

class MapLineLayer internal constructor(internal val sdk: SdkLineLayer) {
    constructor(id: String, sourceId: String) : this(SdkLineLayer(id, sourceId))

    fun withProperties(vararg properties: MapProperty): MapLineLayer = apply {
        sdk.withProperties(*properties.map { it.sdk }.toTypedArray())
    }
}

class MapSymbolLayer internal constructor(internal val sdk: SdkSymbolLayer) {
    constructor(id: String, sourceId: String) : this(SdkSymbolLayer(id, sourceId))

    fun withProperties(vararg properties: MapProperty): MapSymbolLayer = apply {
        sdk.withProperties(*properties.map { it.sdk }.toTypedArray())
    }
}

class MapStyle internal constructor(internal val sdk: SdkStyle) {
    fun addImage(id: String, bitmap: Bitmap) = sdk.addImage(id, bitmap)
    fun addSource(source: MapGeoJsonSource) = sdk.addSource(source.sdk)
    fun addLayer(layer: MapLineLayer) = sdk.addLayer(layer.sdk)
    fun addLayer(layer: MapSymbolLayer) = sdk.addLayer(layer.sdk)
}

sealed interface MapCameraUpdate {
    data class CoordinateZoom(val coordinate: MapCoordinate, val zoom: Double) : MapCameraUpdate
    data class Position(internal val sdk: SdkCameraPosition) : MapCameraUpdate
}

object MapCameraUpdates {
    fun newLatLng(coordinate: MapCoordinate) = MapCameraUpdate.CoordinateZoom(coordinate, Double.NaN)
    fun coordinateZoom(coordinate: MapCoordinate, zoom: Double) =
        MapCameraUpdate.CoordinateZoom(coordinate, zoom)

    fun newLatLngZoom(coordinate: MapCoordinate, zoom: Double) =
        coordinateZoom(coordinate, zoom)
}

class MapCameraPosition internal constructor(internal val sdk: SdkCameraPosition) {
    val target: MapCoordinate? get() = sdk.target?.toMapCoordinate()
}

class MapController internal constructor(internal val sdk: SdkMap) {
    val cameraPosition: MapCameraPosition get() = MapCameraPosition(sdk.cameraPosition)

    fun moveCamera(update: MapCameraUpdate) {
        sdk.moveCamera(update.toSdk())
    }

    fun easeCamera(update: MapCameraUpdate, durationMs: Int) {
        sdk.easeCamera(update.toSdk(), durationMs)
    }

    fun getCameraForBounds(points: List<MapCoordinate>, padding: IntArray): MapCameraUpdate.Position? {
        if (points.isEmpty()) return null
        val bounds = SdkLatLngBounds.from(
            points.maxOf { it.latitude },
            points.maxOf { it.longitude },
            points.minOf { it.latitude },
            points.minOf { it.longitude }
        )
        return sdk.getCameraForLatLngBounds(bounds, padding)?.let(MapCameraUpdate::Position)
    }

    fun addOnMapClickListener(listener: (MapCoordinate) -> Boolean) {
        sdk.addOnMapClickListener { listener(it.toMapCoordinate()) }
    }

    fun screenLocation(coordinate: MapCoordinate): MapScreenPoint {
        val point = sdk.projection.toScreenLocation(coordinate.toSdk())
        return MapScreenPoint(point.x, point.y)
    }
}

data class MapScreenPoint(val x: Float, val y: Float)

class MapViewport internal constructor(private val sdk: SdkMapView) {
    val width: Int get() = sdk.width
    val height: Int get() = sdk.height
    val isAttachedToWindow: Boolean get() = sdk.isAttachedToWindow

    fun postDelayed(action: () -> Unit, delayMs: Long) {
        sdk.postDelayed(action, delayMs)
    }
}

private fun MapCameraUpdate.toSdk() = when (this) {
    is MapCameraUpdate.CoordinateZoom ->
        if (zoom.isNaN()) SdkCameraUpdateFactory.newLatLng(coordinate.toSdk())
        else SdkCameraUpdateFactory.newLatLngZoom(coordinate.toSdk(), zoom)
    is MapCameraUpdate.Position -> SdkCameraUpdateFactory.newCameraPosition(sdk)
}

private fun MapCoordinate.toSdk() = SdkLatLng(latitude, longitude)
private fun SdkLatLng.toMapCoordinate() = MapCoordinate(latitude, longitude)
