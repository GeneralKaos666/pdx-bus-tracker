package com.trimettransit.tracker.feature.trips

import android.view.MotionEvent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.viewinterop.AndroidView
import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.ui.components.badgeBitmap
import com.trimettransit.tracker.ui.components.transitBadgeLetters
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.components.transitOnColor
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import java.util.Locale
private const val TRIP_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val TRIP_MAP_STYLE_URL_DARK = "https://tiles.openfreemap.org/styles/dark"
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
    myLocation: LatLng?,
    picking: PickSlot,
    onMapTap: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false
) {
    val currentOnMapTap by rememberUpdatedState(onMapTap)
    val pickingActive = picking != PickSlot.NONE
    val currentPickingActive by rememberUpdatedState(pickingActive)
    val mapState = remember { TripMapState() }
    val fitSize = remember { intArrayOf(-1, -1) }
    val density = LocalDensity.current.density
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val mapStyleUrl = if (isDark) TRIP_MAP_STYLE_URL_DARK else TRIP_MAP_STYLE_URL
    var appliedStyleUrl by remember { mutableStateOf<String?>(null) }

    // Guarantee the route markers and lines track the selected itinerary even if the
    // AndroidView update pass is skipped on a future recomposition.
    LaunchedEffect(origin, dest, itinerary) {
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
                        ?: LatLng(45.5189, -122.6795)
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

    fun applyTripStyle(style: Style) {
        val letters = transitBadgeLetters()
        letters.forEach { letter ->
            style.addImage(
                "badge-$letter",
                badgeBitmap(
                    context,
                    transitColor(letter, scheme).toArgb(),
                    transitIconResource(letter),
                    density,
                    transitOnColor(letter, scheme).toArgb()
                )
            )
        }
        mapState.letterColors = letters.associateWith {
            String.format(Locale.US, "#%06X", 0xFFFFFF and transitColor(it, scheme).toArgb())
        }
        style.addImage(
            "origin-dot",
            originDotBitmap(transitColor("B", scheme).toArgb(), density)
        )
        style.addImage(
            "dest-dot",
            destDotBitmap(transitColor("R", scheme).toArgb(), density)
        )
        style.addImage("stop-dot", stopDotBitmap(scheme.secondary.toArgb(), scheme.onSecondary.toArgb(), density))
        style.addImage("me-dot", meDotBitmap(scheme.primary.toArgb(), density))

        fun addSource(name: String): GeoJsonSource {
            val source = GeoJsonSource(name)
            style.addSource(source)
            return source
        }

        // Transit stick lines: color driven per-feature from the badge-letter color.
        mapState.transitSource = addSource("transit-source")
        style.addLayer(
            LineLayer("transit-layer", "transit-source").withProperties(
                PropertyFactory.lineColor(Expression.get("color")),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
        // Walk segments: dashed outline-colored line.
        mapState.walkSource = addSource("walk-source")
        style.addLayer(
            LineLayer("walk-layer", "walk-source").withProperties(
                PropertyFactory.lineColor(scheme.outline.toArgb()),
                PropertyFactory.lineWidth(3f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineDasharray(arrayOf(2f, 2f))
            )
        )
        // Boarding/alighting dots and route badges.
        mapState.stopSource = addSource("stop-source")
        style.addLayer(
            SymbolLayer("stop-layer", "stop-source").withProperties(
                iconImage("stop-dot"),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
        mapState.boardSource = addSource("board-source")
        style.addLayer(
            SymbolLayer("board-layer", "board-source").withProperties(
                iconImage(Expression.get("icon")),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
        mapState.originSource = addSource("origin-source")
        style.addLayer(
            SymbolLayer("origin-layer", "origin-source").withProperties(
                iconImage("origin-dot"),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
        mapState.destSource = addSource("dest-source")
        style.addLayer(
            SymbolLayer("dest-layer", "dest-source").withProperties(
                iconImage("dest-dot"),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
        mapState.meSource = addSource("me-source")
        style.addLayer(
            SymbolLayer("me-layer", "me-source").withProperties(
                iconImage("me-dot"),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                getMapAsync { map ->
                    mapState.map = map
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isAttributionEnabled = true
                    map.setMaxZoomPreference(18.0)
                    map.setStyle(mapStyleUrl) { style ->
                        applyTripStyle(style)
                        appliedStyleUrl = mapStyleUrl
                        mapState.push(origin, dest, itinerary)
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(45.5189, -122.6795), 12.0
                            )
                        )
                    }
                    map.addOnMapClickListener { latLng ->
                        if (currentPickingActive) {
                            currentOnMapTap(latLng)
                            true
                        } else {
                            false
                        }
                    }
                }
                setOnTouchListener { v, event ->
                    if (event.pointerCount < 2) {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                    false
                }
                post { onStart() }
                mapState.mapView = this
            }
        },
        update = { view ->
            view.onStart()
            view.onResume()
            val vmap = mapState.map
            if (vmap != null && appliedStyleUrl != mapStyleUrl) {
                appliedStyleUrl = mapStyleUrl
                vmap.setStyle(mapStyleUrl) { style ->
                    applyTripStyle(style)
                    mapState.push(origin, dest, itinerary)
                }
            }
            mapState.push(origin, dest, itinerary)
            val location = myLocation
            if (location != null) {
                mapState.applyMe(location.latitude, location.longitude)
            }
            fitPlanCameraIfReady(view, mapState, origin, dest, itinerary, fitSize)
        },
        modifier = modifier.then(mapSemantics)
    )

    DisposableEffect(Unit) {
        onDispose {
            mapState.mapView?.onStop()
            mapState.mapView?.onPause()
            mapState.mapView?.onDestroy()
            mapState.map = null
            mapState.mapView = null
        }
    }
}

/** Fits the camera to the current plan once the viewport size has settled. */
internal fun fitPlanCameraIfReady(
    view: MapView,
    state: TripMapState,
    origin: TripPoint?,
    dest: TripPoint?,
    itinerary: TripItinerary?,
    fitSize: IntArray,
    attempts: Int = 0
) {
    val map = state.map ?: return
    val points = buildList {
        origin?.let { add(LatLng(it.latitude, it.longitude)) }
        dest?.let { add(LatLng(it.latitude, it.longitude)) }
        itinerary?.legs?.forEach { leg ->
            if (leg.from.latitude != 0.0 || leg.from.longitude != 0.0) {
                add(LatLng(leg.from.latitude, leg.from.longitude))
            }
            if (leg.to.latitude != 0.0 || leg.to.longitude != 0.0) {
                add(LatLng(leg.to.latitude, leg.to.longitude))
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
                fitPlanCameraIfReady(view, state, origin, dest, itinerary, fitSize, attempts + 1)
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
            CameraUpdateFactory.newLatLngZoom(points.first(), PLAN_CAMERA_ZOOM), 400
        )
        return
    }
    val bounds = LatLngBounds.from(
        points.maxOf { it.latitude }, points.maxOf { it.longitude },
        points.minOf { it.latitude }, points.minOf { it.longitude }
    )
    val cam = map.getCameraForLatLngBounds(bounds, intArrayOf(96, 180, 96, 96))
    if (cam == null) {
        map.easeCamera(
            CameraUpdateFactory.newLatLngZoom(points.first(), PLAN_CAMERA_ZOOM), 400
        )
    } else {
        map.easeCamera(CameraUpdateFactory.newCameraPosition(cam), 400)
    }
}