package com.trimettransit.tracker.feature.vehicles

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.VehiclePosition
import com.trimettransit.tracker.transit.TransitApi
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.LoadingState
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.ui.components.transitBadgeLetter
import com.trimettransit.tracker.ui.components.transitBadgeLetters
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.camera.CameraUpdate
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory.fillColor
import org.maplibre.android.style.layers.PropertyFactory.fillOpacity
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
import org.maplibre.android.style.layers.PropertyFactory.textIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.Polygon
import kotlin.math.cos
import kotlin.math.sin

private const val WHATS_NEARBY_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
/** Nearby stop search + map radius around the user, in feet (matches NearbyStopsScreen, which uses feet = 500). */
private const val NEARBY_RADIUS_FEET = 800
private const val FOOT_TO_METERS = 0.3048
/** Derived so the stop search and the map radius circle can never drift apart. */
private val NEARBY_RADIUS_METERS = NEARBY_RADIUS_FEET * FOOT_TO_METERS
/** Fallback camera zoom when bounds fitting is unavailable (16.0 ≈ city block level). */
private const val ME_CAMERA_ZOOM = 16.0
private const val LOCATION_FIX_TIMEOUT_MS = 10_000L
/** A fix younger than this is reused on resume; older ones (or none) trigger a fresh request. */
private const val LOCATION_REFRESH_INTERVAL_MS = 5 * 60_000L

@Composable
fun WhatsNearbyScreen(onNavigateToArrivals: (Stop, Int) -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var vehicles by remember { mutableStateOf<List<VehiclePosition>?>(null) }
    var stops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasLoaded by remember { mutableStateOf(false) }
    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    // Age (device time) of the current fix; 0 when no fix has been acquired yet.
    var myLocationAgeMs by remember { mutableLongStateOf(0L) }
    // In-flight jobs, deduped so resume/re-entry can't stack overlapping fetches.
    var vehiclesJob by remember { mutableStateOf<Job?>(null) }
    var stopsJob by remember { mutableStateOf<Job?>(null) }
    var locationJob by remember { mutableStateOf<Job?>(null) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
    }

    fun loadVehicles() {
        // Cancel any in-flight fetch first: resume can fire a second load while the
        // first is still running, and a stale completion must not overwrite newer data.
        vehiclesJob?.cancel()
        isLoading = true
        errorMessage = null
        vehiclesJob = coroutineScope.launch {
            val result = TransitApi.fetchVehicles(
                context = context,
                routes = null,
                onRouteOnly = true,
                showStale = false
            )
            // Keep the last-known positions when a fetch fails (e.g. offline mid-poll)
            // so the map doesn't blank; only drop to no-data when there was none before.
            if (result != null || vehicles == null) {
                vehicles = result
            }
            isLoading = false
            hasLoaded = true
            if (result == null && vehicles == null) {
                errorMessage = "Unable to load vehicle positions"
            }
        }
    }

    fun loadStopsNearby() {
        val location = myLocation ?: return
        // Cancel any in-flight fetch first so a slow older response can't overwrite newer stops.
        stopsJob?.cancel()
        stopsJob = coroutineScope.launch {
            stops = TransitApi.fetchStopsByLocation(
                context = context,
                ll = "${location.latitude},${location.longitude}",
                feet = NEARBY_RADIUS_FEET
            )
        }
    }

    fun refreshLocation() {
        // Re-request when there is no fix yet or the last one has gone stale; a single
        // in-flight probe is shared so concurrent resume calls can't stack duplicates.
        if (myLocation != null &&
            System.currentTimeMillis() - myLocationAgeMs < LOCATION_REFRESH_INTERVAL_MS
        ) {
            return
        }
        if (locationJob?.isActive == true) return
        locationJob = coroutineScope.launch {
            val fix = requestCurrentLocation(context)
            if (fix != null) {
                myLocation = fix
                myLocationAgeMs = System.currentTimeMillis()
            }
        }
    }

    // Ask for location once if not granted; reading it only matters for the map camera/marker.
    LaunchedEffect(Unit) {
        if (!locationPermissionGranted) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            // Fast path: last known fix, if any (carry its age so a stale cached fix
            // triggers a fresh request on the next resume). Otherwise wait for a fresh
            // fix so the "you are here" dot and centered camera appear even with no
            // prior location.
            val lastKnown = readLastKnownLocation(context)
            if (lastKnown != null) {
                myLocation = lastKnown.first
                myLocationAgeMs = lastKnown.second
            } else {
                val freshFix = requestCurrentLocation(context)
                if (freshFix != null) {
                    myLocation = freshFix
                    myLocationAgeMs = System.currentTimeMillis()
                }
            }
        }
    }

    LaunchedEffect(myLocation) {
        loadStopsNearby()
    }

    LaunchedEffect(Unit) {
        loadVehicles()
    }

    // Re-fetch on app re-entry
    RememberOnResume {
        if (hasLoaded) {
            loadVehicles()
            loadStopsNearby()
            if (locationPermissionGranted) {
                refreshLocation()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "What's Nearby",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Box(modifier = Modifier.weight(1f)) {
            VehicleMap(
                myLocation = myLocation,
                stops = stops ?: emptyList(),
                vehicles = vehicles ?: emptyList(),
                onStopClick = { stop -> onNavigateToArrivals(stop, -1) },
                modifier = Modifier.fillMaxSize()
            )
            Crossfade(
                targetState = when {
                    isLoading && vehicles == null -> 0
                    errorMessage != null && vehicles == null -> 1
                    else -> 2
                },
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "mapOverlay"
            ) { state ->
                when (state) {
                    0 -> {
                        LoadingState()
                    }
                    1 -> {
                        ErrorState(message = errorMessage ?: "Unknown error")
                    }
                    else -> {}
                }
            }
        }
    }
}

/** Last-known fix from any provider, with its device-time age; null when the device
 *  has no stored fix yet. The age lets the screen refresh a stale cached fix instead
 *  of trusting it forever. */
@android.annotation.SuppressLint("MissingPermission")
private fun readLastKnownLocation(context: Context): Pair<LatLng, Long>? {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFineLocation) return null
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        ?: return null
    return LatLng(location.latitude, location.longitude) to location.time
}

/**
 * Requests a fresh fix via LocationManager.getCurrentLocation, probing GPS then the network
 * provider, each with a bounded wait. getLastKnownLocation() is often null on devices with
 * no prior fix, so this is the path that actually produces the me-dot and camera center.
 */
@android.annotation.SuppressLint("MissingPermission")
private suspend fun requestCurrentLocation(context: Context): LatLng? {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFineLocation) return null
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val executor = ContextCompat.getMainExecutor(context)
    for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
        val deferred = CompletableDeferred<LatLng?>()
        val signal = CancellationSignal()
        try {
            // This overload returns Unit; the signal cancels the pending fix on timeout.
            locationManager.getCurrentLocation(provider, signal, executor) { location ->
                deferred.complete(location?.let { LatLng(it.latitude, it.longitude) })
            }
        } catch (e: SecurityException) {
            // Permission revoked between check and call — no fix from this provider.
            // Release the pending signal (mirroring the IllegalArgumentException branch)
            // so the timeout path can't fire on a dead request.
            signal.cancel()
            deferred.complete(null)
        } catch (e: IllegalArgumentException) {
            // Provider not present on this device — try the next one; don't leak the
            // pending signal or leave the deferred dangling.
            signal.cancel()
            deferred.complete(null)
            continue
        }
        val fix = withTimeoutOrNull(LOCATION_FIX_TIMEOUT_MS) { deferred.await() }
        signal.cancel()
        if (fix != null) return fix
    }
    return null
}

@Composable
private fun VehicleMap(
    myLocation: LatLng?,
    stops: List<Stop>,
    vehicles: List<VehiclePosition>,
    onStopClick: (Stop) -> Unit,
    modifier: Modifier = Modifier
) {
    // Re-read the latest stop-click callback on every recomposition so the once-registered
    // map click listener never fires a stale lambda.
    val currentOnStopClick by rememberUpdatedState(onStopClick)
    val mapState = remember { VehicleMapState() }
    // Size of the map view at the time of the last camera fit; a degenerate (0x0 or still
    // resizing) viewport yields a never-converging fit, so defer until the size is stable.
    val fitSize = remember { intArrayOf(-1, -1) }
    val density = LocalDensity.current.density
    val scheme = MaterialTheme.colorScheme
    val badgeColors = remember(scheme) {
        transitBadgeLetters().associateWith { transitColor(it, scheme) }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                getMapAsync { map ->
                    mapState.map = map
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isAttributionEnabled = true
                    map.setMaxZoomPreference(18.0)
                    map.setStyle(WHATS_NEARBY_MAP_STYLE_URL) { style ->
                        badgeColors.forEach { (letter, color) ->
                            style.addImage(
                                "badge-$letter",
                                badgeBitmap(ctx, color.toArgb(), transitIconResource(letter), density)
                            )
                        }
                        style.addImage("stop-dot", stopDotBitmap(ctx, scheme.secondary.toArgb(), density))
                        style.addImage("me-dot", meDotBitmap(ctx, scheme.primary.toArgb(), density))
                        val radiusSource = GeoJsonSource("radius-source")
                        style.addSource(radiusSource)
                        style.addLayer(
                            FillLayer("radius-layer", "radius-source").withProperties(
                                fillColor(scheme.primary.toArgb()),
                                fillOpacity(0.15f)
                            )
                        )
                        val stopsSource = GeoJsonSource("stop-source")
                        style.addSource(stopsSource)
                        style.addLayer(
                            SymbolLayer("stop-layer", "stop-source").withProperties(
                                iconImage("stop-dot"),
                                iconAnchor(Property.ICON_ANCHOR_CENTER),
                                iconAllowOverlap(true),
                                iconIgnorePlacement(true),
                                textField(Expression.get("name")),
                                textFont(arrayOf("Open Sans Regular")),
                                textSize(11f),
                                textColor(android.graphics.Color.BLACK),
                                textAnchor(Property.TEXT_ANCHOR_TOP),
                                textOffset(arrayOf(0f, 1.5f)),
                                textAllowOverlap(true),
                                textIgnorePlacement(true)
                            )
                        )
                        val vehiclesSource = GeoJsonSource("vehicles-source")
                        style.addSource(vehiclesSource)
                        style.addLayer(
                            SymbolLayer("vehicles-layer", "vehicles-source").withProperties(
                                iconImage(Expression.get("icon")),
                                iconRotate(Expression.get("heading")),
                                iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                                iconAnchor(Property.ICON_ANCHOR_CENTER),
                                iconAllowOverlap(true),
                                iconIgnorePlacement(true)
                            )
                        )
                        val meSource = GeoJsonSource("me-source")
                        style.addSource(meSource)
                        style.addLayer(
                            SymbolLayer("me-layer", "me-source").withProperties(
                                iconImage("me-dot"),
                                iconAnchor(Property.ICON_ANCHOR_CENTER),
                                iconAllowOverlap(true),
                                iconIgnorePlacement(true)
                            )
                        )
                        mapState.radiusSource = radiusSource
                        mapState.stopsSource = stopsSource
                        mapState.vehiclesSource = vehiclesSource
                        mapState.meSource = meSource
                        val location = myLocation
                        if (location != null) {
                            mapState.applyMe(location.latitude, location.longitude)
                            map.moveCamera(cameraFramingRadius(map, location))
                            mapState.lastCentered = location
                        } else {
                            // Downtown Portland default viewport until a fix arrives.
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(45.5189, -122.6795), 12.0))
                        }
                        mapState.applyStops()      // in case update ran before style load
                        mapState.applyVehicles()   // in case update ran before style load
                    }
                    // Tap a stop dot to open that stop's arrivals screen. Registered once per map
                    // lifetime; the map is destroyed with the view (DisposableEffect below), so no
                    // removeOnMapClickListener is needed. Returning false for a miss lets the map
                    // pan normally.
                    map.addOnMapClickListener { latLng ->
                        val screen = map.projection.toScreenLocation(latLng)
                        val hits = map.queryRenderedFeatures(screen, "stop-layer")
                        val locId = hits.firstOrNull()?.getNumberProperty("locId")?.toInt() ?: 0
                        val stop = mapState.stops.firstOrNull { it.locId == locId }
                        if (stop != null) {
                            currentOnStopClick(stop)
                            true
                        } else {
                            false
                        }
                    }
                }
                // Block parent (Compose) gesture interception for single-finger touches so
                // MapView can pan normally; never consume the event itself. Multi-touch
                // zoom reaches MapView untouched.
                setOnTouchListener { v, event ->
                    if (event.pointerCount < 2) {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                    false
                }
                // MapLibre requires onStart() before it activates its file source (network).
                // post() guarantees the view is attached first.
                post { onStart() }
                mapState.mapView = this
            }
        },
        update = { view ->
            view.onStart()   // idempotent; also covers the factory's post() ordering
            view.onResume()
            mapState.stops = stops
            mapState.applyStops()
            mapState.vehicles = vehicles
            mapState.applyVehicles()
            val location = myLocation
            if (location != null) {
                // The me-dot always tracks the latest fix; the camera re-frames only when the fix
                // left the current radius (GPS jitter and panning without movement are no-ops).
                mapState.applyMe(location.latitude, location.longitude)
                val centered = mapState.lastCentered
                if ((centered == null || movedBeyondRadius(centered, location)) && mapState.map != null) {
                    mapState.map?.moveCamera(cameraFramingRadius(mapState.map!!, location))
                    mapState.lastCentered = location
                }
            } else if (!mapState.vehiclesFit && mapState.map != null && vehicles.isNotEmpty()) {
                // No location available: fit the camera to all vehicles once the viewport is stable.
                scheduleVehicleCameraFit(view, mapState, vehicles, fitSize)
            }
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            // MapLibre teardown order: onStart()/onStop() gate the native map, so
            // stop first, then pause, then destroy (AGENTS.md MapLibre section).
            mapState.mapView?.onStop()
            mapState.mapView?.onPause()
            mapState.mapView?.onDestroy()
            // Drop the map references so any still-scheduled fit callbacks no-op
            // instead of touching a destroyed map.
            mapState.map = null
            mapState.mapView = null
        }
    }
}

private class VehicleMapState {
    var mapView: MapView? = null
    var map: MapLibreMap? = null
    var stopsSource: GeoJsonSource? = null
    var vehiclesSource: GeoJsonSource? = null
    var meSource: GeoJsonSource? = null
    var radiusSource: GeoJsonSource? = null
    var stops: List<Stop> = emptyList()
    var vehicles: List<VehiclePosition> = emptyList()
    // The point the camera was last framed on; null = never centered yet.
    var lastCentered: LatLng? = null
    // One-shot by design: the camera fits to the vehicles exactly once per map lifetime
    // (only when no location fix exists); later vehicle polls update markers but must
    // not yank the camera away from wherever the user has panned.
    var vehiclesFit = false

    /** Pushes the latest nearby stops into the stop-source (no-op until the style is ready). */
    fun applyStops() {
        val source = stopsSource ?: return
        val features = stops.map { stop ->
            val feature = Feature.fromGeometry(Point.fromLngLat(stop.longitude, stop.latitude))
            feature.addNumberProperty("locId", stop.locId)
            feature.addStringProperty("name", stop.desc)
            feature
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    /** Pushes the latest vehicle positions into the GeoJsonSource (no-op until style is ready). */
    fun applyVehicles() {
        val source = vehiclesSource ?: return
        val features = vehicles.map { v ->
            val letter = transitBadgeLetter(v.routeNumber).ifBlank { "B" }
            val feature = Feature.fromGeometry(Point.fromLngLat(v.longitude, v.latitude))
            feature.addStringProperty("icon", "badge-$letter")
            feature.addNumberProperty("heading", v.bearing)
            feature
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    /** Drops the me-dot and the 800-ft radius ring into their sources. */
    fun applyMe(lat: Double, lng: Double) {
        val center = LatLng(lat, lng)
        meSource?.setGeoJson(
            FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Point.fromLngLat(lng, lat))))
        )
        radiusSource?.setGeoJson(
            FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(radiusRing(center))))
        )
    }
}

private fun drawableBitmap(context: Context, resId: Int, sizePx: Int): Bitmap {
    val d = ContextCompat.getDrawable(context, resId)
    return d?.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        ?: createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}

/** Colored circle badge with a white transit glyph, used as the vehicle marker image. */
private fun badgeBitmap(context: Context, fillColor: Int, glyphRes: Int, density: Float): Bitmap {
    val size = (34 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor })
    val glyph = drawableBitmap(context, glyphRes, (20 * density).toInt())
    c.drawBitmap(glyph, (size - glyph.width) / 2f, (size - glyph.height) / 2f, null)
    return out
}

/** Secondary-colored dot with a dark outline and white center, used as the nearby stop marker image. */
private fun stopDotBitmap(context: Context, fillColor: Int, density: Float): Bitmap {
    val size = (44 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    val center = size / 2f
    // Dark outline ensures the dot is visible on any map background.
    val outline = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.BLACK }
    c.drawCircle(center, center, size / 2f, outline)
    // Fill circle slightly smaller than outline for a ring effect.
    val fillRadius = size / 2f - (2 * density)
    c.drawCircle(center, center, fillRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor })
    // White center dot.
    val dotRadius = (6 * density).toInt().toFloat()
    c.drawCircle(center, center, dotRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    return out
}

/**
 * "You are here" marker: a primary-colored dot with a white ring inside a translucent
 * primary halo — larger and visually distinct from the flat secondary-colored stop dots
 * (and the vehicle badges) so the user's location stands out on the map.
 */
private fun meDotBitmap(context: Context, fillColor: Int, density: Float): Bitmap {
    val size = (56 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    val center = size / 2f
    // Soft halo (~25% alpha) behind the dot; gives the marker presence over map detail.
    val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = (fillColor and 0x00FFFFFF) or (0x40 shl 24)
    }
    c.drawCircle(center, center, size / 2f, halo)
    // White ring separates the solid dot from the halo and from any nearby stop dots.
    val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE }
    c.drawCircle(center, center, (13 * density).toInt().toFloat(), ring)
    val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor }
    c.drawCircle(center, center, (10 * density).toInt().toFloat(), dot)
    return out
}

/** Bounds of the 800-ft radius around a center (lat/lng degree offsets at that latitude). */
private fun radiusBounds(center: LatLng): LatLngBounds {
    val latDeg = NEARBY_RADIUS_METERS / 111_320.0
    val lngDeg = NEARBY_RADIUS_METERS / (111_320.0 * cos(Math.toRadians(center.latitude)))
    return LatLngBounds.from(
        center.latitude + latDeg, center.longitude + lngDeg,
        center.latitude - latDeg, center.longitude - lngDeg
    )
}

/** Camera centered on the user showing the 800-ft radius with 48px padding; falls back to a
 *  fixed zoom when bounds fitting is unavailable (degenerate viewport). */
private fun cameraFramingRadius(map: MapLibreMap, location: LatLng): CameraUpdate {
    val cam = map.getCameraForLatLngBounds(radiusBounds(location), intArrayOf(48, 48, 48, 48))
    return cam?.let { CameraUpdateFactory.newCameraPosition(it) }
        ?: CameraUpdateFactory.newLatLngZoom(location, ME_CAMERA_ZOOM)
}

/** Re-center threshold: reframe only when the fix moved more than the radius from the last
 *  centered point, so GPS jitter or panning without movement never yanks the camera. */
private fun movedBeyondRadius(from: LatLng, to: LatLng): Boolean {
    val results = FloatArray(1)
    Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results)
    return results[0] > NEARBY_RADIUS_METERS
}

/** 96-point closed ring approximating the 800-ft radius circle (GeoJSON rings must be closed). */
private fun radiusRing(center: LatLng): Polygon {
    val latDeg = NEARBY_RADIUS_METERS / 111_320.0
    val lngDeg = NEARBY_RADIUS_METERS / (111_320.0 * cos(Math.toRadians(center.latitude)))
    val ring = (0 until 96).map { i ->
        val angle = 2.0 * Math.PI * i / 96
        Point.fromLngLat(
            center.longitude + lngDeg * cos(angle),
            center.latitude + latDeg * sin(angle)
        )
    }
    return Polygon.fromLngLats(listOf(ring + ring.first()))
}

/** Fits the camera to all vehicles (no-op with a degenerate size is guarded by the caller). */
private fun fitVehicles(map: MapLibreMap, vehicles: List<VehiclePosition>) {
    if (vehicles.isEmpty()) return
    val all = vehicles.map { LatLng(it.latitude, it.longitude) }
    val bounds = LatLngBounds.from(
        all.maxOf { it.latitude }, all.maxOf { it.longitude },
        all.minOf { it.latitude }, all.minOf { it.longitude }
    )
    val cam = map.getCameraForLatLngBounds(bounds, intArrayOf(48, 48, 48, 48)) ?: return
    val target = cam.target ?: return
    if (cam.zoom > 18.0) {
        map.easeCamera(CameraUpdateFactory.newLatLngZoom(target, 18.0), 400)
    } else {
        map.easeCamera(CameraUpdateFactory.newCameraPosition(cam), 400)
    }
}

/**
 * Fits the camera to all vehicles once the viewport size is stable (non-zero and unchanged
 * since the last sample). The view can be resized or detached while a retry is pending, and
 * the map can be destroyed entirely, so every callback re-checks attachment and reads the
 * live map reference instead of a captured `!!`. Retries are capped — a later update() pass
 * (e.g. the next vehicle poll) re-triggers the fit if the size still hasn't settled.
 */
private fun scheduleVehicleCameraFit(
    view: MapView,
    state: VehicleMapState,
    vehicles: List<VehiclePosition>,
    fitSize: IntArray,
    attempts: Int = 0
) {
    val map = state.map ?: return
    if (state.vehiclesFit || vehicles.isEmpty()) return
    val settled = view.width > 0 && view.height > 0 &&
        view.width == fitSize[0] && view.height == fitSize[1]
    if (settled) {
        fitVehicles(map, vehicles)
        state.vehiclesFit = true
        return
    }
    fitSize[0] = view.width
    fitSize[1] = view.height
    if (attempts >= MAX_VEHICLE_FIT_ATTEMPTS) return
    view.postDelayed({
        if (state.vehiclesFit || !view.isAttachedToWindow) return@postDelayed
        val liveMap = state.map ?: return@postDelayed
        if (view.width > 0 && view.height > 0 && view.width == fitSize[0] && view.height == fitSize[1]) {
            fitVehicles(liveMap, vehicles)
            state.vehiclesFit = true
        } else {
            scheduleVehicleCameraFit(view, state, vehicles, fitSize, attempts + 1)
        }
    }, 400)
}

/** Cap on settle-check retries before giving up on the vehicle camera fit. */
private const val MAX_VEHICLE_FIT_ATTEMPTS = 3