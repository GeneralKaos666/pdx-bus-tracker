package com.trimettransit.tracker.map

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

/**
 * Shared MapLibre `MapView` scaffolding for the phone screens (trip-planner map and
 * arrivals stop map). Owns the AndroidView lifecycle: map creation, basemap style load,
 * light/dark in-place re-apply, single-finger touch swallowing (configurable), and
 * onStart/onPause/onStop/onDestroy wiring. Callers supply only their layer drawing
 * ([onStyleReady]) and per-frame data pushes ([onUpdate]).
 */
@Composable
fun MapLibreMapHost(
    styleUrl: String,
    modifier: Modifier = Modifier,
    consumeSingleFingerTouches: Boolean = true,
    onStyleReady: (map: MapLibreMap, style: Style, isReapply: Boolean) -> Unit,
    onUpdate: (view: MapView, map: MapLibreMap?) -> Unit
) {
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var viewRef by remember { mutableStateOf<MapView?>(null) }
    var appliedStyleUrl by remember { mutableStateOf<String?>(null) }

    fun applyStyle(map: MapLibreMap, isReapply: Boolean) {
        map.setStyle(styleUrl) { style -> onStyleReady(map, style, isReapply) }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                getMapAsync { map ->
                    mapRef = map
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isAttributionEnabled = true
                    map.setMaxZoomPreference(18.0)
                    applyStyle(map, isReapply = false)
                    appliedStyleUrl = styleUrl
                }
                // Consume single-finger touches at View level to prevent propagation to
                // Compose parent gesture handlers (pull-to-refresh, nav drawer, pager).
                // Multi-touch zoom unaffected. consumeSingleFingerTouches=false still
                // disallows parent intercept so the map can pan.
                setOnTouchListener { v, event ->
                    if (event.pointerCount < 2) {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                        if (consumeSingleFingerTouches) return@setOnTouchListener true
                    }
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                    false
                }
                // MapLibre requires onStart() before it activates its file source
                // (network). post() guarantees the view is attached first.
                post { onStart() }
                viewRef = this
            }
        },
        update = { view ->
            view.onStart()   // idempotent; also covers the factory's post() ordering
            view.onResume()
            // If the resolved basemap (light/dark) changed since it was last applied,
            // reload the style and re-invoke the caller's style setup before pushing data.
            val map = mapRef
            if (map != null && appliedStyleUrl != styleUrl) {
                appliedStyleUrl = styleUrl
                applyStyle(map, isReapply = true)
            }
            onUpdate(view, map)
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            viewRef?.onStop()
            viewRef?.onPause()
            viewRef?.onDestroy()
            viewRef = null
            mapRef = null
        }
    }
}