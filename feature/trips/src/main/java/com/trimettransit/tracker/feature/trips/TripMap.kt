package com.trimettransit.tracker.feature.trips

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.map.MapCameraUpdates
import com.trimettransit.tracker.map.MapCoordinate
import com.trimettransit.tracker.map.MapExpression
import com.trimettransit.tracker.map.MapGeoJsonSource
import com.trimettransit.tracker.map.MapLineLayer
import com.trimettransit.tracker.map.MapLibreMapHost
import com.trimettransit.tracker.map.MapPropertyConstants
import com.trimettransit.tracker.map.MapProperties
import com.trimettransit.tracker.map.MapStyle
import com.trimettransit.tracker.map.MapSymbolLayer
import com.trimettransit.tracker.map.MapViewport
import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.ui.appearance.AppearancePrefs
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.appearance.MapStyles
import com.trimettransit.tracker.ui.components.badgeBitmap
import com.trimettransit.tracker.ui.components.transitBadgeLetters
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.components.transitOnColor
import java.util.Locale
private const val PLAN_CAMERA_ZOOM = 14.0
private const val MAX_CAMERA_FIT_ATTEMPTS = 3

internal enum class PickSlot { NONE, ORIGIN, DEST }
/**
 * MapLibre view for the trip planner: origin/destination markers, route "stick" lines
 * (solid transit, dashed walk), boarding badges, the me-dot, and map-tap pin dropping.
 */
@Composable
internal fun TripMap(
    origin: TripPoint?,
    dest: TripPoint?,
    itinerary: TripItinerary?,
    myLocation: MapCoordinate?,
    picking: PickSlot,
    onMapTap: (MapCoordinate) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    legGeometries: Map<Int, List<GeoPoint>> = emptyMap()
) {
    val currentOnMapTap by rememberUpdatedState(onMapTap)
    val pickingActive = picking != PickSlot.NONE
    val currentPickingActive by rememberUpdatedState(pickingActive)
    val currentLegGeometries by rememberUpdatedState(legGeometries)
    val mapState = remember { TripMapState() }
    val fitSize = remember { intArrayOf(-1, -1) }
    val density = LocalDensity.current.density
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val overrides = LocalAppearanceStyle.current.transitTypeColors
    val mapPreset = PreferenceManager.getDefaultSharedPreferences(context)
        .getString(AppearancePrefs.MAP_STYLE, MapStyles.DEFAULT) ?: MapStyles.DEFAULT
    val mapStyleUrl = MapStyles.styleUrlFor(mapPreset, isDark)

    // Guarantee the route markers and lines track the selected itinerary even if the
    // AndroidView update pass is skipped on a future recomposition.
    LaunchedEffect(origin, dest, itinerary, legGeometries) {
        mapState.legGeometries = legGeometries
        mapState.push(origin, dest, itinerary)
    }

    // Expose the map to assistive tech: a plain label when idle, plus an action that drops a
    // pin at the map center while a slot is being picked (the tap-only flow has no keyboard
    // equivalent otherwise).
    val mapLabel = stringResource(R.string.trip_map)
    val pickingHint = when (picking) {
        PickSlot.ORIGIN -> stringResource(R.string.tap_map_to_set_origin)
        PickSlot.DEST -> stringResource(R.string.tap_map_to_set_destination)
        PickSlot.NONE -> null
    }
    val pinAtCenterLabel = when (picking) {
        PickSlot.ORIGIN -> stringResource(R.string.set_pin_origin_at_center)
        PickSlot.DEST -> stringResource(R.string.set_pin_destination_at_center)
        PickSlot.NONE -> null
    }
    val mapSemantics = if (pickingActive) {
        Modifier.semantics(mergeDescendants = true) {
            role = Role.Image
            contentDescription = pickingHint.orEmpty()
            onClick(label = pinAtCenterLabel) {
                onMapTap(
                    mapState.map?.cameraPosition?.target
                        ?: FALLBACK_MAP_CENTER
                )
                true
            }
        }
    } else {
        Modifier.semantics(mergeDescendants = true) {
            role = Role.Image
            contentDescription = mapLabel
        }
    }

    fun applyTripStyle(style: MapStyle) {
        val letters = transitBadgeLetters()
        letters.forEach { letter ->
            style.addImage(
                "badge-$letter",
                badgeBitmap(
                    context,
                    transitColor(letter, scheme, overrides).toArgb(),
                    transitIconResource(letter),
                    density,
                    transitOnColor(letter, scheme, overrides).toArgb()
                )
            )
        }
        mapState.letterColors = letters.associateWith {
            String.format(Locale.US, "#%06X", 0xFFFFFF and transitColor(it, scheme, overrides).toArgb())
        }
        style.addImage(
            "origin-dot",
            originDotBitmap(transitColor("B", scheme, overrides).toArgb(), density)
        )
        style.addImage(
            "dest-dot",
            destDotBitmap(transitColor("R", scheme, overrides).toArgb(), density)
        )
        style.addImage("stop-dot", stopDotBitmap(scheme.secondary.toArgb(), scheme.onSecondary.toArgb(), density))
        style.addImage("me-dot", meDotBitmap(scheme.primary.toArgb(), density))

        fun addSource(name: String): MapGeoJsonSource {
            val source = MapGeoJsonSource(name)
            style.addSource(source)
            return source
        }

        // Transit stick lines: color driven per-feature from the badge-letter color.
        mapState.transitSource = addSource("transit-source")
        style.addLayer(
            MapLineLayer("transit-layer", "transit-source").withProperties(
                MapProperties.lineColor(MapExpression.get("color")),
                MapProperties.lineWidth(4f),
                MapProperties.lineCap(MapPropertyConstants.LINE_CAP_ROUND),
                MapProperties.lineJoin(MapPropertyConstants.LINE_JOIN_ROUND)
            )
        )
        // Walk segments: dashed outline-colored line.
        mapState.walkSource = addSource("walk-source")
        style.addLayer(
            MapLineLayer("walk-layer", "walk-source").withProperties(
                MapProperties.lineColor(scheme.outline.toArgb()),
                MapProperties.lineWidth(3f),
                MapProperties.lineCap(MapPropertyConstants.LINE_CAP_ROUND),
                MapProperties.lineJoin(MapPropertyConstants.LINE_JOIN_ROUND),
                MapProperties.lineDasharray(arrayOf(2f, 2f))
            )
        )
        // Boarding/alighting dots and route badges.
        mapState.stopSource = addSource("stop-source")
        style.addLayer(
            MapSymbolLayer("stop-layer", "stop-source").withProperties(
                MapProperties.iconImage("stop-dot"),
                MapProperties.iconAnchor(MapPropertyConstants.ICON_ANCHOR_CENTER),
                MapProperties.iconAllowOverlap(true),
                MapProperties.iconIgnorePlacement(true)
            )
        )
        mapState.boardSource = addSource("board-source")
        style.addLayer(
            MapSymbolLayer("board-layer", "board-source").withProperties(
                MapProperties.iconImage(MapExpression.get("icon")),
                MapProperties.iconAnchor(MapPropertyConstants.ICON_ANCHOR_CENTER),
                MapProperties.iconAllowOverlap(true),
                MapProperties.iconIgnorePlacement(true)
            )
        )
        mapState.originSource = addSource("origin-source")
        style.addLayer(
            MapSymbolLayer("origin-layer", "origin-source").withProperties(
                MapProperties.iconImage("origin-dot"),
                MapProperties.iconAnchor(MapPropertyConstants.ICON_ANCHOR_CENTER),
                MapProperties.iconAllowOverlap(true),
                MapProperties.iconIgnorePlacement(true)
            )
        )
        mapState.destSource = addSource("dest-source")
        style.addLayer(
            MapSymbolLayer("dest-layer", "dest-source").withProperties(
                MapProperties.iconImage("dest-dot"),
                MapProperties.iconAnchor(MapPropertyConstants.ICON_ANCHOR_CENTER),
                MapProperties.iconAllowOverlap(true),
                MapProperties.iconIgnorePlacement(true)
            )
        )
        mapState.meSource = addSource("me-source")
        style.addLayer(
            MapSymbolLayer("me-layer", "me-source").withProperties(
                MapProperties.iconImage("me-dot"),
                MapProperties.iconAnchor(MapPropertyConstants.ICON_ANCHOR_CENTER),
                MapProperties.iconAllowOverlap(true),
                MapProperties.iconIgnorePlacement(true)
            )
        )
    }

    MapLibreMapHost(
        styleUrl = mapStyleUrl,
        modifier = modifier.then(mapSemantics),
        consumeSingleFingerTouches = false,
        onStyleReady = { map, style, isReapply ->
            mapState.map = map
            applyTripStyle(style)
            mapState.legGeometries = currentLegGeometries
            mapState.push(origin, dest, itinerary)
            if (!isReapply) {
                // Tap-to-drop-pin and the falling-back camera only need setup once; style
                // re-applies reuse the existing listener and camera position.
                map.addOnMapClickListener { latLng ->
                    if (currentPickingActive) {
                        currentOnMapTap(latLng)
                        true
                    } else {
                        false
                    }
                }
                map.moveCamera(
                    MapCameraUpdates.coordinateZoom(FALLBACK_MAP_CENTER, DEFAULT_MAP_ZOOM)
                )
            }
        },
        onUpdate = { view, map ->
            mapState.legGeometries = currentLegGeometries
            mapState.push(origin, dest, itinerary)
            myLocation?.let { mapState.applyMe(it.latitude, it.longitude) }
            if (map != null) {
                fitPlanCameraIfReady(view, map, mapState, origin, dest, itinerary, fitSize)
            }
        }
    )
}

/** Fits the camera to the current plan once the viewport size has settled. */
internal fun fitPlanCameraIfReady(
    view: MapViewport,
    map: com.trimettransit.tracker.map.MapController,
    state: TripMapState,
    origin: TripPoint?,
    dest: TripPoint?,
    itinerary: TripItinerary?,
    fitSize: IntArray,
    attempts: Int = 0
) {
    val points = buildList {
        origin?.let { add(MapCoordinate(it.latitude, it.longitude)) }
        dest?.let { add(MapCoordinate(it.latitude, it.longitude)) }
        itinerary?.legs?.forEach { leg ->
            if (leg.from.latitude != 0.0 || leg.from.longitude != 0.0) {
                add(MapCoordinate(leg.from.latitude, leg.from.longitude))
            }
            if (leg.to.latitude != 0.0 || leg.to.longitude != 0.0) {
                add(MapCoordinate(leg.to.latitude, leg.to.longitude))
            }
        }
    }
    if (points.isEmpty()) return

    val settled = view.width > 0 && view.height > 0 &&
        view.width == fitSize[0] && view.height == fitSize[1]
    if (!settled) {
        fitSize[0] = view.width
        fitSize[1] = view.height
        if (attempts >= MAX_CAMERA_FIT_ATTEMPTS) return
        view.postDelayed({
            if (view.isAttachedToWindow) {
                fitPlanCameraIfReady(view, map, state, origin, dest, itinerary, fitSize, attempts + 1)
            }
        }, 150)
        return
    }

    // The camera belongs to the user once it has been fitted: location fixes, endpoint
    // picker toggles, and theme changes all recompose the map, but none of them should
    // yank the view back to the plan. Only re-fit when the trip itself changed.
    val planTag = TripMapState.FitTag(origin, dest, itinerary)
    if (state.lastFitTag == planTag) return
    state.lastFitTag = planTag

    if (points.size == 1) {
        map.easeCamera(
            MapCameraUpdates.coordinateZoom(points.first(), PLAN_CAMERA_ZOOM), 400
        )
        return
    }
    val cam = map.getCameraForBounds(points, intArrayOf(96, 180, 96, 96))
    if (cam == null) {
        map.easeCamera(
            MapCameraUpdates.coordinateZoom(points.first(), PLAN_CAMERA_ZOOM), 400
        )
    } else {
        map.easeCamera(cam, 400)
    }
}