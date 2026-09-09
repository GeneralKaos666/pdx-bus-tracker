package com.trimettransit.tracker.wear

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.MaterialTheme
import com.trimettransit.tracker.R
import com.trimettransit.tracker.map.MapLibreMapHost
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.BlockPosition
import com.trimettransit.tracker.util.minutesUntil
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
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

/** Basemap styles — see THIRD-PARTY-NOTICES. OpenFreeMap Liberty (light) & Dark. */
private const val STOP_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val STOP_MAP_STYLE_URL_DARK = "https://tiles.openfreemap.org/styles/dark"
private val TRANSIT_BADGE_LETTERS = listOf("B", "M", "S", "W")

/**
 * Live bus map for one stop, ported from the phone's arrivals `StopMapCard` onto the shared
 * `common:map` host with wearable colors. Shows the stop marker and every live vehicle near it,
 * tracking the tapped departure by keeping its bus centered. Runs standalone on the watch —
 * positions come from the same [ArrivalsResult] the arrivals list was built from.
 */
@Composable
fun WearStopMapCard(
    lat: Double,
    lng: Double,
    modifier: Modifier = Modifier,
    blockPositions: List<BlockPosition> = emptyList(),
    arrivals: List<Arrival> = emptyList(),
    trackedVehicleId: Int = 0
) {
    val mapState = remember { WearStopMapState() }
    mapState.dropoffLabel = stringResource(R.string.arrival_dropoff_only)
    mapState.countdownDue = stringResource(R.string.due)
    mapState.countdownMinFormat = stringResource(R.string.countdown_min)
    val density = LocalDensity.current.density
    val scheme = MaterialTheme.colorScheme
    val badgeColors = remember(scheme) {
        TRANSIT_BADGE_LETTERS.associateWith { wearTransitColor(it, scheme) }
    }
    val badgeGlyphColors = remember(scheme) {
        TRANSIT_BADGE_LETTERS.associateWith { wearTransitOnColor(it, scheme) }
    }
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val mapStyleUrl = if (isDark) STOP_MAP_STYLE_URL_DARK else STOP_MAP_STYLE_URL
    val countdownTextColor = scheme.onSurface.toArgb()
    val countdownHaloColor = if (isDark) scheme.background.toArgb() else android.graphics.Color.WHITE

    fun applyStopMapStyle(style: Style) {
        style.addImage(
            "stop-dot",
            wearStopDotBitmap(context, scheme.primary.toArgb(), scheme.onPrimary.toArgb(), density)
        )
        badgeColors.forEach { (letter, color) ->
            style.addImage(
                "badge-$letter",
                wearBadgeBitmap(
                    context,
                    color.toArgb(),
                    wearTransitIconResource(letter),
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
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        MapLibreMapHost(
            styleUrl = mapStyleUrl,
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp),
            consumeSingleFingerTouches = true,
            onStyleReady = { map, style, isReapply ->
                applyStopMapStyle(style)
                if (!isReapply) {
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16.0))
                }
                mapState.applyPositions()
            },
            onUpdate = { view, map ->
                mapState.positions = blockPositions
                mapState.arrivals = arrivals
                mapState.applyPositions()
                if (map != null && view.width > 0 && view.height > 0) {
                    wearTrackedTarget(blockPositions, trackedVehicleId)?.let { target ->
                        wearKeepBusCentered(map, target, view.width, view.height, density)
                    }
                }
            }
        )
    }
}

private class WearStopMapState {
    var busSource: GeoJsonSource? = null
    var positions: List<BlockPosition> = emptyList()
    var arrivals: List<Arrival> = emptyList()

    /** Resolved label strings, set when the card is configured. */
    var dropoffLabel: String = ""
    var countdownDue: String = ""
    var countdownMinFormat: String = ""

    /** Pushes the latest bus positions into the GeoJsonSource (no-op until style is ready). */
    fun applyPositions() {
        val source = busSource ?: return
        val features = positions
            .filter { it.lat != 0.0 || it.lng != 0.0 }
            .map { bp ->
                val letter = wearTransitBadgeLetter(bp.routeNumber).ifBlank { "B" }
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
                    val displayTime = match?.let { a ->
                        if (a.status == "estimated" && a.estimated != null) a.estimated else a.scheduled
                    }
                    if (displayTime != null) {
                        val mins = minutesUntil(displayTime)
                        if (mins <= 0) countdownDue else {
                            runCatching { countdownMinFormat.format(mins) }.getOrElse { "$mins" }
                        }
                    } else ""
                }
                feature.addStringProperty("countdown", label)
                feature
            }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}