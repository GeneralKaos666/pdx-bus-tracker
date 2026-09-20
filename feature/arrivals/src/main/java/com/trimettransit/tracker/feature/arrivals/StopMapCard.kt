package com.trimettransit.tracker.feature.arrivals

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.map.MapLibreMapHost
import com.trimettransit.tracker.map.MapCameraUpdates as CameraUpdateFactory
import com.trimettransit.tracker.map.MapController as MapLibreMap
import com.trimettransit.tracker.map.MapCoordinate as LatLng
import com.trimettransit.tracker.map.MapExpression as Expression
import com.trimettransit.tracker.map.MapFeature as Feature
import com.trimettransit.tracker.map.MapFeatureCollection as FeatureCollection
import com.trimettransit.tracker.map.MapGeoJsonSource as GeoJsonSource
import com.trimettransit.tracker.map.MapPoint as Point
import com.trimettransit.tracker.map.MapPropertyConstants as Property
import com.trimettransit.tracker.map.MapProperties.iconAllowOverlap
import com.trimettransit.tracker.map.MapProperties.iconAnchor
import com.trimettransit.tracker.map.MapProperties.iconIgnorePlacement
import com.trimettransit.tracker.map.MapProperties.iconImage
import com.trimettransit.tracker.map.MapProperties.iconRotate
import com.trimettransit.tracker.map.MapProperties.iconRotationAlignment
import com.trimettransit.tracker.map.MapProperties.textAllowOverlap
import com.trimettransit.tracker.map.MapProperties.textAnchor
import com.trimettransit.tracker.map.MapProperties.textColor
import com.trimettransit.tracker.map.MapProperties.textField
import com.trimettransit.tracker.map.MapProperties.textFont
import com.trimettransit.tracker.map.MapProperties.textHaloColor
import com.trimettransit.tracker.map.MapProperties.textHaloWidth
import com.trimettransit.tracker.map.MapProperties.textIgnorePlacement
import com.trimettransit.tracker.map.MapProperties.textOffset
import com.trimettransit.tracker.map.MapProperties.textSize
import com.trimettransit.tracker.map.MapStyle as Style
import com.trimettransit.tracker.map.MapSymbolLayer as SymbolLayer
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.BlockPosition
import com.trimettransit.tracker.model.domain.displayTimeMillis
import com.trimettransit.tracker.util.minutesUntil
import com.trimettransit.tracker.util.systemReduceMotion
import com.trimettransit.tracker.ui.components.DotCircle
import com.trimettransit.tracker.ui.components.badgeBitmap
import com.trimettransit.tracker.ui.components.circleMarker
import com.trimettransit.tracker.ui.components.transitBadgeLetter
import com.trimettransit.tracker.ui.components.transitBadgeLetters
import com.trimettransit.tracker.ui.appearance.AppearancePrefs
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.appearance.MapStyles
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.components.transitOnColor
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.appCardBorder
import com.trimettransit.tracker.ui.theme.AppMotion

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
    // Capture the glide scope here (the map's onUpdate callback can't remember one), and
    // read the reduce-motion preference so glideTracked knows whether to degrade to a
    // plain teleport. Both are configuration-aware, matching the neighboring label lookups.
    // Bridge the app-wide value (set at app root) with a live system read so a mid-session
    // "Remove animations" toggle takes effect on the next recomposition.
    val glideScope = rememberCoroutineScope()
    val reduceMotion = AppMotion.reduceMotion || systemReduceMotion(context)
    // Remembered on (context, isDark) so bus-position refreshes don't re-hit
    // SharedPreferences and recompute the style URL on every recomposition.
    val mapPreset = remember(context, isDark) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .getString(AppearancePrefs.MAP_STYLE, MapStyles.DEFAULT) ?: MapStyles.DEFAULT
    }
    val mapStyleUrl = remember(mapPreset, isDark) {
        MapStyles.styleUrlFor(mapPreset, isDark)
    }
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

    DisposableEffect(mapState) {
        onDispose { mapState.glideJob?.cancel() }
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
                mapState.trackedVehicleId = trackedVehicleId
                if (map != null && view.width > 0 && view.height > 0) {
                    val glideTarget = glideTarget(blockPositions, trackedVehicleId)
                    if (glideTarget != null) {
                        // Ease the tracked bus's marker to its fresh fix; every other bus still
                        // teleports (they are not being followed). glideTracked degrades to a plain
                        // teleport whenever the glide is unsafe or unwanted (reduce motion, first
                        // fix, unchanged target, source not ready), exactly matching old behavior.
                        mapState.glideTracked(trackedVehicleId, glideTarget, reduceMotion, glideScope)
                    } else {
                        mapState.clearTracking()
                        mapState.applyPositions()
                    }
                    // Follow the tracked bus instead of framing the stop together with it,
                    // so the camera stays centered on the vehicle and its "N min" label never
                    // clips at the map's top edge. The stop marker still renders but simply
                    // scrolls out of frame once a bus position is available.
                    cameraTarget(blockPositions, trackedVehicleId)?.let { target ->
                        keepBusCentered(map, target, view.width, view.height, density)
                    }
                } else {
                    mapState.clearTracking()
                    mapState.applyPositions()
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

    /** The bus whose marker currently eases toward its live fix (0 = none / teleport). */
    var trackedVehicleId: Int = 0

    /** The vehicle [trackedOrigin] belongs to; a re-track must not glide from the old bus. */
    var lastTrackedId: Int = 0

    /** The last tracked live fix, used as the glide origin so the marker eases from it. */
    var trackedOrigin: LatLng? = null

    /** In-flight eased-tracking job (cancelled on every new position push). */
    var glideJob: Job? = null

    /** Resolved "Dropoff Only" label, set when the map is configured. */
    var dropoffLabel: String = ""

    /** Resolved countdown labels, set when the map is configured. */
    var countdownDue: String = ""
    var countdownMinFormat: String = ""

    /** Pushes the latest bus positions into the GeoJsonSource (no-op until style is ready). */
    fun applyPositions() {
        writeFeatures(null)
    }

    /**
     * Forgets the glide origin (and cancels any in-flight glide) when nothing trackable is
     * on screen, so the next appearance teleports instead of easing from a stale fix.
     */
    fun clearTracking() {
        glideJob?.cancel()
        glideJob = null
        trackedOrigin = null
        lastTrackedId = 0
    }

    /**
     * Eases the tracked bus's marker from its last live fix to a fresh one instead of
     * teleporting, so the vehicle you're following visibly moves instead of snapping.
     * Every other bus still teleports (they are not being followed). Degrades to a plain
     * teleport — exactly today's behavior — whenever the animation is unsafe or unwanted:
     * reduce-motion preference, missing scope, first fix (no origin to ease from), an
     * unchanged target, or a source that still isn't ready.
     */
    fun glideTracked(vehicleId: Int, to: LatLng, reduceMotion: Boolean, scope: CoroutineScope) {
        // A re-track must teleport: easing from the previous bus's fix would streak across
        // the map on the first frame batch after switching rows.
        if (vehicleId != lastTrackedId) {
            lastTrackedId = vehicleId
            trackedOrigin = to
            glideJob?.cancel()
            glideJob = null
            writeFeatures(null)
            return
        }
        val source = busSource
        val origin = trackedOrigin
        glideJob?.cancel()
        if (reduceMotion || source == null || origin == null || origin == to) {
            trackedOrigin = to
            writeFeatures(null)
            return
        }
        glideJob = scope.launch {
            val start = withFrameNanos { it }
            val duration = 600_000_000L
            while (true) {
                val now = withFrameNanos { it }
                val t = ((now - start).toFloat() / duration).coerceIn(0f, 1f)
                val eased = easeOutCubic(t)
                val lat = origin.latitude + (to.latitude - origin.latitude) * eased
                val lng = origin.longitude + (to.longitude - origin.longitude) * eased
                writeFeatures(LatLng(lat, lng))
                if (t >= 1f) break
            }
            trackedOrigin = to
            writeFeatures(null)
        }
    }

    /**
     * Builds the FeatureCollection from the current [positions], substituting [trackedAt]
     * as the tracked vehicle's coordinates so a glide can repaint just that one marker.
     */
    private fun writeFeatures(trackedAt: LatLng?) {
        val source = busSource ?: return
        val features = positions
            .filter { it.lat != 0.0 || it.lng != 0.0 }
            .map { bp ->
            val at = if (bp.vehicleID == trackedVehicleId && trackedAt != null) {
                trackedAt
            } else {
                LatLng(bp.lat, bp.lng)
            }
            val letter = transitBadgeLetter(bp.routeNumber).ifBlank { "B" }
            val feature = Feature.fromGeometry(Point.fromLngLat(at.longitude, at.latitude))
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

/** Ease-out-cubic 0..1 curve for the tracked-marker glide (fast start, soft landing). */
private fun easeOutCubic(t: Float): Float {
    val u = 1f - t
    return 1f - u * u * u
}

/** Camera target: the tracked vehicle when present, else the first live position. */
private fun cameraTarget(
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
 * Marker-glide target: non-null only when the requested vehicle is actually on screen.
 * Unlike [cameraTarget] there is no first-bus fallback — gliding toward the wrong bus's
 * fix would repaint invisible frames and pollute the glide origin.
 */
private fun glideTarget(
    blockPositions: List<BlockPosition>,
    trackedVehicleId: Int
): LatLng? {
    if (trackedVehicleId == 0) return null
    return blockPositions.firstOrNull {
        it.vehicleID == trackedVehicleId && (it.lat != 0.0 || it.lng != 0.0)
    }?.let { LatLng(it.lat, it.lng) }
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
    val p = map.screenLocation(target)
    val outside = p.x < marginPx || p.x > viewWidth - marginPx ||
            p.y < topMarginPx || p.y > viewHeight - marginPx
    if (outside) {
        map.easeCamera(CameraUpdateFactory.newLatLng(target), 400)
    }
}