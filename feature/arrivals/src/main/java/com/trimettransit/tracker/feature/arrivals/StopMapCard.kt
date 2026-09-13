package com.trimettransit.tracker.feature.arrivals

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.map.MapLibreMapHost
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.BlockPosition
import com.trimettransit.tracker.model.domain.displayTimeMillis
import com.trimettransit.tracker.util.minutesUntil
import com.trimettransit.tracker.ui.components.DotCircle
import com.trimettransit.tracker.ui.components.badgeBitmap
import com.trimettransit.tracker.ui.components.circleMarker
import com.trimettransit.tracker.ui.components.transitBadgeLetter
import com.trimettransit.tracker.ui.components.transitBadgeLetters
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.components.transitOnColor
import com.trimettransit.tracker.ui.theme.LocalCardStyle
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.appCardBorder
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.PropertyFactory.textAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

internal const val STOP_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
internal const val STOP_MAP_STYLE_URL_DARK = "https://tiles.openfreemap.org/styles/dark"

@Composable
internal fun StopMapCard(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
    blockPositions: List<BlockPosition> = emptyList(),
    arrivals: List<Arrival> = emptyList(),
    trackedVehicleId: Int = 0,
    isDark: Boolean = false
) {
    val mapState = remember { MapState() }
    // Resolve the drop-off label in the configuration-aware composable scope (the map's
    // getMapAsync callback is not configuration-aware, so it can't look the string up there).
    mapState.dropoffLabel = stringResource(R.string.arrival_dropoff_only)
    mapState.countdownDue = stringResource(R.string.due)
    mapState.countdownMinFormat = stringResource(R.string.minutes)
    val density = LocalDensity.current.density
    val scheme = MaterialTheme.colorScheme
    val overrides = LocalAppearanceStyle.current.transitTypeColors
    val badgeColors = remember(scheme) {
        transitBadgeLetters().associateWith { transitColor(it, scheme, overrides) }
    }
    val badgeGlyphColors = remember(scheme) {
        transitBadgeLetters().associateWith { transitOnColor(it, scheme, overrides) }
    }
    val context = LocalContext.current
    val mapStyleUrl = if (isDark) STOP_MAP_STYLE_URL_DARK else STOP_MAP_STYLE_URL
    // MapLibre halo/text colors are chosen for legibility against the basemap: light basemap
    // wants a light halo over dark glyphs, the dark basemap wants a dark halo over light glyphs.
    val countdownTextColor = scheme.onSurface.toArgb()
    val countdownHaloColor = if (isDark) scheme.surface.toArgb() else android.graphics.Color.WHITE

    fun applyStopMapStyle(style: Style) {
        style.addImage(
            "stop-dot",
            circleMarker(
                backDp = 17f,
                backColor = scheme.primary.toArgb(),
                fillDp = 17f,
                fillColor = scheme.primary.toArgb(),
                density = density,
                foreground = listOf(DotCircle(6f, scheme.onPrimary.toArgb()))
            )
        )
        badgeColors.forEach { (letter, color) ->
            style.addImage(
                "badge-$letter",
                badgeBitmap(
                    context,
                    color.toArgb(),
                    transitIconResource(letter),
                    density,
                    badgeGlyphColors[letter]?.toArgb() ?: android.graphics.Color.WHITE
                )
            )
        }
        style.addSource(
            GeoJsonSource(
                "stop-source",
                Feature.fromGeometry(Point.fromLngLat(lng, lat))
            )
        )
        style.addLayer(
            SymbolLayer("stop-layer", "stop-source").withProperties(
                iconImage("stop-dot"),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
        val busSource = GeoJsonSource("bus-source")
        style.addSource(busSource)
        style.addLayer(
            SymbolLayer("bus-layer", "bus-source").withProperties(
                iconImage(Expression.get("icon")),
                iconRotate(Expression.get("bearing")),
                iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
        mapState.busSource = busSource
        style.addLayer(
            SymbolLayer("countdown-layer", "bus-source").withProperties(
                textField(Expression.get("countdown")),
                textAnchor(Property.TEXT_ANCHOR_BOTTOM),
                textOffset(arrayOf(0f, -1.8f)),
                textSize(11f),
                textColor(countdownTextColor),
                textHaloColor(countdownHaloColor),
                textHaloWidth(2f),
                textAllowOverlap(true),
                textIgnorePlacement(true),
                textFont(arrayOf("Noto Sans Bold"))
            )
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = appCardShape(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = appCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        MapLibreMapHost(
            styleUrl = mapStyleUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            consumeSingleFingerTouches = true,
            onStyleReady = { map, style, isReapply ->
                applyStopMapStyle(style)
                if (!isReapply) {
                    map.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16.0)
                    )
                }
                mapState.applyPositions()   // in case update ran before style load
            },
            onUpdate = { view, map ->
                mapState.positions = blockPositions
                mapState.arrivals = arrivals
                mapState.applyPositions()
                // Follow the tracked bus instead of framing the stop together with it,
                // so the camera stays centered on the vehicle and its "N min" label never
                // clips at the map's top edge. The stop marker still renders but simply
                // scrolls out of frame once a bus position is available.
                if (map != null && view.width > 0 && view.height > 0) {
                    trackedTarget(blockPositions, trackedVehicleId)?.let { target ->
                        keepBusCentered(map, target, view.width, view.height, density)
                    }
                }
            }
        )
    }
}

private class MapState {
    var map: MapLibreMap? = null
    var busSource: GeoJsonSource? = null
    var positions: List<BlockPosition> = emptyList()
    var arrivals: List<Arrival> = emptyList()

    /** Resolved "Dropoff Only" label, set when the map is configured. */
    var dropoffLabel: String = ""

    /** Resolved countdown labels, set when the map is configured. */
    var countdownDue: String = ""
    var countdownMinFormat: String = ""

    /** Pushes the latest bus positions into the GeoJsonSource (no-op until style is ready). */
    fun applyPositions() {
        val source = busSource ?: return
        val features = positions
            .filter { it.lat != 0.0 || it.lng != 0.0 }
            .map { bp ->
            val letter = transitBadgeLetter(bp.routeNumber).ifBlank { "B" }
            val feature = Feature.fromGeometry(Point.fromLngLat(bp.lng, bp.lat))
            feature.addStringProperty("icon", "badge-$letter")
            feature.addNumberProperty("bearing", bp.bearing)
            // Time-left label shown above the icon: the tracked arrival for this vehicle,
            // phrased exactly like the list rows. Drop-off-only arrivals show the label
            // instead of a countdown. Empty string renders nothing on the map.
            val match = arrivals.firstOrNull { it.vehicleID == bp.vehicleID }
            val label = if (match?.dropOffOnly == true) {
                dropoffLabel
            } else {
                val atMillis = match?.displayTimeMillis
                if (atMillis != null) {
                    val mins = minutesUntil(atMillis)
                    if (mins <= 0) countdownDue else countdownMinFormat.format(mins)
                } else ""
            }
            feature.addStringProperty("countdown", label)
            feature
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}

/** The bus to follow: the tracked vehicle, else the first live position on that route. */
private fun trackedTarget(
    blockPositions: List<BlockPosition>,
    trackedVehicleId: Int
): LatLng? {
    val valid = blockPositions.filter { it.lat != 0.0 || it.lng != 0.0 }
    if (valid.isEmpty()) return null
    return valid.firstOrNull { it.vehicleID == trackedVehicleId }
        ?.let { LatLng(it.lat, it.lng) }
        ?: LatLng(valid.first().lat, valid.first().lng)
}

/**
 * Pans the camera back onto the bus only when it drifts outside a centered band.
 * The top margin is larger so the "N min" label above the icon stays on screen;
 * user zoom is preserved and the stop is no longer kept in frame.
 */
private fun keepBusCentered(
    map: MapLibreMap,
    target: LatLng,
    viewWidth: Int,
    viewHeight: Int,
    density: Float
) {
    val marginPx = (24 * density).toInt()
    val topMarginPx = (72 * density).toInt()
    val p = map.projection.toScreenLocation(target)
    val outside = p.x < marginPx || p.x > viewWidth - marginPx ||
            p.y < topMarginPx || p.y > viewHeight - marginPx
    if (outside) {
        map.easeCamera(CameraUpdateFactory.newLatLng(target), 400)
    }
}