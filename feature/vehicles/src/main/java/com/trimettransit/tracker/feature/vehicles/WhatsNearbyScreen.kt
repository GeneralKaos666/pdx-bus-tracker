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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.trimettransit.tracker.ui.components.rememberOnResume
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.PropertyFactory.iconRotate
import org.maplibre.android.style.layers.PropertyFactory.iconRotationAlignment
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val WHATS_NEARBY_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val NEARBY_STOP_SEARCH_METERS = 500
private const val ME_CAMERA_ZOOM = 16.0
private const val LOCATION_FIX_TIMEOUT_MS = 10_000L

@Composable
fun WhatsNearbyScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var vehicles by remember { mutableStateOf<List<VehiclePosition>?>(null) }
    var stops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasLoaded by remember { mutableStateOf(false) }
    var myLocation by remember { mutableStateOf<LatLng?>(null) }
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
        isLoading = true
        errorMessage = null
        coroutineScope.launch {
            val result = TransitApi.fetchVehicles(
                context = context,
                routes = null,
                onRouteOnly = true,
                showStale = false
            )
            vehicles = result
            isLoading = false
            hasLoaded = true
            if (result == null) {
                errorMessage = "Unable to load vehicle positions"
            }
        }
    }

    fun loadStopsNearby() {
        val location = myLocation ?: return
        coroutineScope.launch {
            stops = TransitApi.fetchStopsByLocation(
                context = context,
                ll = "${location.latitude},${location.longitude}",
                meters = NEARBY_STOP_SEARCH_METERS
            )
        }
    }

    fun refreshLocation() {
        if (myLocation != null) return
        coroutineScope.launch {
            myLocation = requestCurrentLocation(context)
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
            // Fast path: last known fix, if any. Otherwise wait for a fresh fix so the
            // "you are here" dot and centered camera appear even with no prior location.
            myLocation = readLastKnownLocation(context) ?: requestCurrentLocation(context)
        }
    }

    LaunchedEffect(myLocation) {
        loadStopsNearby()
    }

    LaunchedEffect(Unit) {
        loadVehicles()
    }

    // Re-fetch on app re-entry
    rememberOnResume {
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
                modifier = Modifier.fillMaxSize()
            )
            when {
                isLoading && vehicles == null -> {
                    LoadingState()
                }
                errorMessage != null && vehicles == null -> {
                    ErrorState(message = errorMessage ?: "Unknown error")
                }
            }
        }
    }
}

/** Last-known fix from any provider; null when the device has no stored fix yet. */
private fun readLastKnownLocation(context: Context): LatLng? {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFineLocation) return null
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        ?: return null
    return LatLng(location.latitude, location.longitude)
}

/**
 * Requests a fresh fix via LocationManager.getCurrentLocation, probing GPS then the network
 * provider, each with a bounded wait. getLastKnownLocation() is often null on devices with
 * no prior fix, so this is the path that actually produces the me-dot and camera center.
 */
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
            deferred.complete(null)
        } catch (e: IllegalArgumentException) {
            // Provider not present on this device — try the next one.
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
    modifier: Modifier = Modifier
) {
    val mapState = remember { VehicleMapState() }
    // Size of the map view at the time of the last camera fit; a degenerate (0x0 or still
    // resizing) viewport yields a never-converging fit, so defer until the size is stable.
    val fitSize = remember { intArrayOf(-1, -1) }
    val density = LocalDensity.current.density
    val scheme = MaterialTheme.colorScheme
    val badgeColors = remember(scheme) {
        listOf("B", "M", "R", "W", "T").associateWith { transitColor(it, scheme) }
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
                        val stopsSource = GeoJsonSource("stop-source")
                        style.addSource(stopsSource)
                        style.addLayer(
                            SymbolLayer("stop-layer", "stop-source").withProperties(
                                iconImage("stop-dot"),
                                iconAnchor(Property.ICON_ANCHOR_CENTER),
                                iconAllowOverlap(true),
                                iconIgnorePlacement(true)
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
                        mapState.stopsSource = stopsSource
                        mapState.vehiclesSource = vehiclesSource
                        mapState.meSource = meSource
                        val location = myLocation
                        if (location != null) {
                            mapState.applyMe(location.latitude, location.longitude)
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(location, ME_CAMERA_ZOOM))
                            mapState.mePlaced = true
                        } else {
                            // Downtown Portland default viewport until a fix arrives.
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(45.5189, -122.6795), 12.0))
                        }
                        mapState.applyStops()      // in case update ran before style load
                        mapState.applyVehicles()   // in case update ran before style load
                    }
                }
                // Consume single-finger touches at View level to prevent propagation
                // to Compose parent gesture handlers. Multi-touch zoom unaffected.
                setOnTouchListener { v, event ->
                    val consume = event.pointerCount < 2
                    if (consume && event.actionMasked == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                    consume
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
            if (location != null && !mapState.mePlaced) {
                // Location arrived after the style loaded (e.g. permission granted mid-session).
                mapState.mePlaced = true
                mapState.applyMe(location.latitude, location.longitude)
                mapState.map?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, ME_CAMERA_ZOOM))
            } else if (location == null && !mapState.vehiclesFit && mapState.map != null && vehicles.isNotEmpty()) {
                // No location available: fit the camera to all vehicles once the viewport is stable.
                val settled = view.width > 0 && view.height > 0 &&
                    view.width == fitSize[0] && view.height == fitSize[1]
                if (settled) {
                    fitVehicles(mapState.map!!, vehicles)
                    mapState.vehiclesFit = true
                } else {
                    fitSize[0] = view.width
                    fitSize[1] = view.height
                    view.postDelayed({
                        if (view.width == fitSize[0] && view.height == fitSize[1]) {
                            fitVehicles(mapState.map!!, vehicles)
                            mapState.vehiclesFit = true
                        } else {
                            fitSize[0] = view.width
                            fitSize[1] = view.height
                            view.postDelayed({
                                fitVehicles(mapState.map!!, vehicles)
                                mapState.vehiclesFit = true
                            }, 400)
                        }
                    }, 400)
                }
            }
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            mapState.mapView?.onStop()
            mapState.mapView?.onPause()
            mapState.mapView?.onDestroy()
        }
    }
}

private class VehicleMapState {
    var mapView: MapView? = null
    var map: MapLibreMap? = null
    var stopsSource: GeoJsonSource? = null
    var vehiclesSource: GeoJsonSource? = null
    var meSource: GeoJsonSource? = null
    var stops: List<Stop> = emptyList()
    var vehicles: List<VehiclePosition> = emptyList()
    var mePlaced = false
    var vehiclesFit = false

    /** Pushes the latest nearby stops into the stop-source (no-op until the style is ready). */
    fun applyStops() {
        val source = stopsSource ?: return
        val features = stops.map { stop ->
            Feature.fromGeometry(Point.fromLngLat(stop.longitude, stop.latitude))
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    /** Pushes the latest vehicle positions into the GeoJsonSource (no-op until style is ready). */
    fun applyVehicles() {
        val source = vehiclesSource ?: return
        val features = vehicles.map { v ->
            val letter = vehicleLetter(v.routeNumber).ifBlank { "B" }
            val feature = Feature.fromGeometry(Point.fromLngLat(v.longitude, v.latitude))
            feature.addStringProperty("icon", "badge-$letter")
            feature.addNumberProperty("heading", v.bearing)
            feature
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }

    /** Drops a single "you are here" feature into the me-source. */
    fun applyMe(lat: Double, lng: Double) {
        val source = meSource ?: return
        source.setGeoJson(FeatureCollection.fromFeatures(listOf(Feature.fromGeometry(Point.fromLngLat(lng, lat)))))
    }
}

/** Transit-type letter for a route number (same mapping as the arrivals screen's badge logic). */
private fun vehicleLetter(routeNumber: Int): String = when {
    routeNumber == 200 -> "M"
    routeNumber == 100 || routeNumber == 90 -> "R"
    routeNumber in 1..99 -> "B"
    else -> ""
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

/** Secondary-colored dot with a white center, used as the nearby stop marker image. */
private fun stopDotBitmap(context: Context, fillColor: Int, density: Float): Bitmap {
    val size = (34 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor })
    val dotRadius = (5 * density).toInt().toFloat()
    c.drawCircle(size / 2f, size / 2f, dotRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    return out
}

/** Primary-colored dot with a white center, used as the "you are here" marker image. */
private fun meDotBitmap(context: Context, fillColor: Int, density: Float): Bitmap {
    val size = (34 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor })
    val dotRadius = (6 * density).toInt().toFloat()
    c.drawCircle(size / 2f, size / 2f, dotRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    return out
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