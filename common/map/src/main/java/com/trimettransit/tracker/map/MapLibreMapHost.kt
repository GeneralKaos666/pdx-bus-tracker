package com.trimettransit.tracker.map

import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

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
    onStyleReady: (map: MapController, style: MapStyle, isReapply: Boolean) -> Unit,
    onUpdate: (viewport: MapViewport, map: MapController?) -> Unit
) {
    var mapRef by remember { mutableStateOf<MapLibreMap?>(null) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var appliedStyleUrl by remember { mutableStateOf<String?>(null) }

    // MapView lifecycle follows the host lifecycle, not recomposition. `update` used to
    // call onStart()/onResume() on every pass, which re-ran native resume work each time
    // the caller pushed new data.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val view = mapViewRef ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> view.onStart()
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> view.onPause()
                Lifecycle.Event.ON_STOP -> view.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun applyStyle(map: MapLibreMap, isReapply: Boolean) {
        map.setStyle(styleUrl) { style ->
            onStyleReady(MapController(map), MapStyle(style), isReapply)
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                mapViewRef = this
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
                // (network). post() guarantees the view is attached first. Guarded by
                // isAttachedToWindow: rapid navigation can dispose (and detach) this view
                // before the posted callback runs, and calling onStart() on an already
                // torn-down MapView races the render thread against native teardown,
                // producing a native SIGSEGV in libhwui's position-update callback.
                // The lifecycle observer is registered before this factory runs, so its
                // replayed ON_START/ON_RESUME hit a null view. Resume here as well.
                post {
                    if (isAttachedToWindow) {
                        onStart()
                        onResume()
                    }
                }
            }
        },
        update = { view ->
            // If the resolved basemap (light/dark) changed since it was last applied,
            // reload the style and re-invoke the caller's style setup before pushing data.
            val map = mapRef
            if (map != null && appliedStyleUrl != styleUrl) {
                appliedStyleUrl = styleUrl
                applyStyle(map, isReapply = true)
            }
            onUpdate(MapViewport(view), map?.let(::MapController))
        },
        onRelease = { view ->
            // Compose guarantees onRelease runs exactly once, correctly sequenced with
            // this View's detachment — unlike a same-composable DisposableEffect, which
            // can be disposed on a different pass than the AndroidView node itself during
            // rapid navigation, letting the render thread deliver a queued position-update
            // callback into a MapView that Java-side teardown hasn't caught up with yet.
            view.onStop()
            view.onPause()
            view.onDestroy()
            mapRef = null
            mapViewRef = null
        },
        modifier = modifier
    )
}