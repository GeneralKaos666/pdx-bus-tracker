package com.trimettransit.tracker.ui.screens.arrivals

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.R
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.model.Arrival
import com.trimettransit.tracker.data.model.BlockPosition
import com.trimettransit.tracker.data.model.Detour
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.ui.NavState
import com.trimettransit.tracker.ui.TransitApi
import com.trimettransit.tracker.ui.screens.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.screens.components.EmptyState
import com.trimettransit.tracker.ui.screens.components.ErrorState
import com.trimettransit.tracker.ui.screens.components.LoadingState
import com.trimettransit.tracker.ui.screens.components.rememberOnResume
import com.trimettransit.tracker.util.formatDateTime
import com.trimettransit.tracker.util.minutesUntil
import com.trimettransit.tracker.ui.screens.components.transitColor
import com.trimettransit.tracker.ui.screens.components.transitIconResource
import com.trimettransit.tracker.ui.screens.components.transitTypeLabel
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalDensity
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.Property
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
import androidx.compose.ui.graphics.toArgb
import java.io.File

private const val TAG = "ArrivalsScreen"
private const val POSITION_REFRESH_MS = 30_000L
private const val STOP_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrivalsScreen(
    stopId: String,
    stopName: String,
    routeId: Int,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
)
{
    val context = LocalContext.current
    var arrivals by remember { mutableStateOf<List<Arrival>>(emptyList()) }
    var blockPositions by remember { mutableStateOf<List<BlockPosition>>(emptyList()) }
    var detours by remember { mutableStateOf<List<Detour>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var showAlertsSheet by remember { mutableStateOf(false) }
    var showAllArrivals by remember { mutableStateOf(false) }
    var trackingKey by remember { mutableStateOf<String?>(null) }
    var trackingRouteId by remember { mutableStateOf(-1) }
    var trackingSign by remember { mutableStateOf("") }
    var trackingVehicleId by remember { mutableStateOf(0) }
    var unfilteredArrivals by remember { mutableStateOf<List<Arrival>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val locId = stopId.toIntOrNull() ?: 0
    var stopLat by remember { mutableStateOf(latitude) }
    var stopLng by remember { mutableStateOf(longitude) }
    var isLoadingStop by remember { mutableStateOf(false) }
    val hasValidCoords = !isLoadingStop && stopLat != 0.0 && stopLng != 0.0

    // Read initial favorite state from DB
    LaunchedEffect(stopId) {
        if (locId > 0) {
            NavState.arrivalsIsFavorite = DatabaseHelper(context.applicationContext).isFavorite(locId)
        }
    }

    LaunchedEffect(locId) {
        if ((stopLat == 0.0 || stopLng == 0.0) && locId > 0) {
            isLoadingStop = true
            TransitApi.fetchStopById(context, locId)?.let { stop ->
                stopLat = stop.latitude
                stopLng = stop.longitude
            }
            isLoadingStop = false
            if (stopLat == 0.0 || stopLng == 0.0) {
                Log.w(TAG, "Stop #$locId has zero coordinates after fallback — map hidden")
            }
        }
    }

    fun loadArrivals() {
        coroutineScope.launch {
            isLoading = true
            val result = TransitApi.fetchArrivals(
                context = context,
                locIds = listOf(locId),
                showPosition = true,
                minutes = 30,
                maxArrivals = 15
            )
            if (result != null && !result.isQueryError) {
                val allArrivals = result.arrivals?.toList() ?: emptyList()
                unfilteredArrivals = allArrivals
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val onlySelectedRoute = prefs.getBoolean("pref_key_only_show_route_selected", true)
                arrivals = if (onlySelectedRoute && routeId > 0) {
                    allArrivals.filter { it.routeId == routeId }
                } else {
                    allArrivals
                }
                detours = result.detours?.toList() ?: emptyList()
                blockPositions = result.blockPositions?.toList() ?: emptyList()
                // Resolve stop coordinates from arrivals response if not yet known
                if (stopLat == 0.0 || stopLng == 0.0) {
                    if (result.stopLat != 0.0 && result.stopLng != 0.0) {
                        stopLat = result.stopLat
                        stopLng = result.stopLng
                    }
                }
                isError = false
            } else {
                arrivals = emptyList()
                isError = true
            }
            isLoading = false
        }
    }

    // Re-fetch arrivals on app re-entry (and initial composition via lifecycle observer)
    rememberOnResume { loadArrivals() }
    val totalRouteCount = unfilteredArrivals.map { it.routeId }.distinct().size
    val visibleCount = minOf(arrivals.size, 5)
    val showExpandButton = totalRouteCount > 1 && unfilteredArrivals.size > visibleCount
    // Tracked positions: the tapped row's own vehicle when it reports a position,
    // otherwise that line's other live vehicles so the map is never empty.
    val trackedPositions = if (trackingVehicleId > 0) {
        blockPositions.filter { it.vehicleID == trackingVehicleId }
            .ifEmpty { blockPositions.filter { it.routeNumber == trackingRouteId } }
    } else {
        blockPositions.filter { it.routeNumber == trackingRouteId }
    }

    // Live position polling: only while the tracking dropdown is open
    var isAppResumed by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            isAppResumed = event == Lifecycle.Event.ON_RESUME
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var positionRefreshInFlight by remember { mutableStateOf(false) }
    fun refreshPositions() {
        if (positionRefreshInFlight) return
        positionRefreshInFlight = true
        coroutineScope.launch {
            try {
                val result = TransitApi.fetchArrivals(
                    context = context,
                    locIds = listOf(locId),
                    showPosition = true,
                    minutes = 30,
                    maxArrivals = 15
                )
                if (result != null && !result.isQueryError) {
                    blockPositions = result.blockPositions?.toList() ?: emptyList()
                }
            } finally {
                positionRefreshInFlight = false
            }
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(POSITION_REFRESH_MS)
            if (isAppResumed && trackingKey != null) refreshPositions()
        }
    }
    LaunchedEffect(trackingKey) {
        if (trackingKey != null) refreshPositions()
    }

    // Populate NavState for outer scaffold's top bar
    LaunchedEffect(Unit) {
        NavState.arrivalsStopName = stopName.ifBlank { "Stop #$stopId" }
    }

    DisposableEffect(Unit) {
        // Must use a stable lambda — loadArrivals is a local fun, always the same behavior
        NavState.arrivalsOnRefresh = { loadArrivals() }
        onDispose {
            NavState.clearArrivals()
        }
    }
    val smoothFling = rememberSmoothFlingBehavior()

    // Bridge resolved coordinates to outer scaffold for favorite persistence
    LaunchedEffect(stopLat, stopLng) {
        NavState.arrivalsLat = stopLat
        NavState.arrivalsLng = stopLng
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { loadArrivals() },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            isLoading && arrivals.isEmpty() -> {
                LoadingState()
            }

            isError && arrivals.isEmpty() -> {
                ErrorState(message = "Unable to load arrivals.\nPull to retry.")
            }

            arrivals.isEmpty() && !isLoading -> {
                EmptyState(message = "No upcoming arrivals.")
            }

            else -> {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxSize(),
                        flingBehavior = smoothFling,
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {


                    val visibleArrivals = if (showAllArrivals) unfilteredArrivals else arrivals.take(5)
                    items(visibleArrivals, key = { "${it.tripID}_${it.routeId}_${it.scheduledMillis}" }, contentType = { "arrival" }) { arrival ->
                        val rowKey = "${arrival.tripID}_${arrival.routeId}_${arrival.scheduledMillis}"
                        Column {
                            ArrivalItem(
                                arrival = arrival,
                                context = context,
                                onClick = {
                                    if (hasValidCoords) {
                                        if (trackingKey == rowKey) {
                                            trackingKey = null              // tap the tracked row again to close
                                        } else {
                                            trackingKey = rowKey            // opens under this row; switches if another row is tracked
                                            trackingRouteId = arrival.routeId
                                            trackingSign = arrival.shortSign
                                            trackingVehicleId = arrival.vehicleID
                                        }
                                    }
                                },
                                modifier = Modifier.animateItem()
                            )
                            AnimatedVisibility(
                                visible = trackingKey == rowKey,
                                enter = expandVertically() + fadeIn(),
                                exit = shrinkVertically() + fadeOut()
                            ) {
                                StopMapCard(
                                    lat = stopLat,
                                    lng = stopLng,
                                    stopName = stopName,
                                    blockPositions = trackedPositions,
                                    headerText = trackingSign.ifBlank { stopName },
                                    onClose = { trackingKey = null }
                                )
                            }
                        }
                    }

                    if (showExpandButton) {
                        item(key = "showAll", contentType = "showAll") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceContainerLow
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showAllArrivals = !showAllArrivals }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showAllArrivals) "Show fewer"
                                            else "Show all arrivals (${unfilteredArrivals.size - visibleCount} more)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val showAllArrowRotation by animateFloatAsState(
                                        targetValue = if (showAllArrivals) 180f else 0f,
                                        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = if (showAllArrivals) "Collapse arrivals"
                                            else "Expand arrivals",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.rotate(showAllArrowRotation)
                                    )
                                }
                            }
                        }
                    }
                }

                if (detours.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAlertsSheet = true }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alerts (${detours.size})",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Show alerts",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                }
            }
        }
    }
}

    if (showAlertsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAlertsSheet = false },
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ) {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)
            ) {
                Text(
                    text = "Alerts (${detours.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                detours.forEach { detour ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "\u2022",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = detour.desc ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}

private fun formatDelay(arrival: Arrival): String? {
    if (arrival.status != "estimated" || arrival.estimatedMillis == 0L || arrival.scheduledMillis == 0L) return null
    val delayMin = ((arrival.estimatedMillis - arrival.scheduledMillis) / 60000).toInt()
    return when {
        delayMin > 1 -> "${delayMin} min late"
        delayMin < -1 -> "${-delayMin} min early"
        else -> "On time"
    }
}

private fun routeTypeLetter(routeNumber: Int): String = when {
    routeNumber == 200 -> "M"
    routeNumber == 100 || routeNumber == 90 -> "R"
    routeNumber in 1..99 -> "B"
    else -> ""
}

@Composable
private fun StopMapCard(
    lat: Double,
    lng: Double,
    stopName: String,
    blockPositions: List<BlockPosition> = emptyList(),
    headerText: String? = null,
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showHeader: Boolean = true
) {
    val mapState = remember { MapState() }
    // Size of the map view at the time of the last camera fit. Fitting the camera while
    // the card is still expanding (0 x 0 or growing) yields a degenerate/never-converging
    // fit, so only fit once the viewport size is stable.
    val fitSize = remember { intArrayOf(-1, -1) }
    val density = LocalDensity.current.density
    val scheme = MaterialTheme.colorScheme
    val badgeColors = remember(scheme) {
        listOf("B", "M", "R", "W", "T").associateWith { transitColor(it, scheme) }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            if (showHeader) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = headerText ?: stopName.ifBlank { "Stop Location" },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    if (onClose != null) {
                        IconButton(onClick = onClose) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close map",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        // TEMP-DEBUG: file marker to verify style load / feature push on-device.
                        mapState.debugOut = File(ctx.filesDir, "map_debug.txt").apply { delete() }
                        getMapAsync { map ->
                            mapState.map = map
                            map.uiSettings.isCompassEnabled = false
                            map.uiSettings.isAttributionEnabled = true
                            map.setMaxZoomPreference(18.0)
                            map.setStyle(STOP_MAP_STYLE_URL) { style ->
                                style.addImage("stop-dot", stopDotBitmap(ctx, scheme.primary.toArgb(), density))
                                badgeColors.forEach { (letter, color) ->
                                    style.addImage(
                                        "badge-$letter",
                                        badgeBitmap(ctx, color.toArgb(), transitIconResource(letter), density)
                                    )
                                }
                                style.addSource(
                                    GeoJsonSource("stop-source", Feature.fromGeometry(Point.fromLngLat(lng, lat)))
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
                                        iconRotate(Expression.get("heading")),
                                        iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                                        iconAnchor(Property.ICON_ANCHOR_CENTER),
                                        iconAllowOverlap(true),
                                        iconIgnorePlacement(true)
                                    )
                                )
                                mapState.busSource = busSource
                                mapState.log("onStyleLoaded ok")
                                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 16.0))
                                mapState.applyPositions()   // in case update ran before style load
                            }
                        }
                        // Consume single-finger touches at View level to prevent
                        // propagation to Compose parent gesture handlers
                        // (pull-to-refresh, nav drawer). Multi-touch zoom unaffected.
                        setOnTouchListener { _, event -> event.pointerCount < 2 }
                        // MapLibre requires onStart() before it activates its file source
                        // (network). post() guarantees the view is attached first.
                        post { onStart() }
                        mapState.mapView = this
                    }
                },
                update = { view ->
                    view.onStart()   // idempotent; also covers the factory's post() ordering
                    view.onResume()
                    mapState.positions = blockPositions
                    mapState.applyPositions()
                    // Keep stop + tracked buses in view; re-fit only once the map has a
                    // stable, non-degenerate size and something is actually off-screen.
                    val map = mapState.map
                    val points = blockPositions.map { it.lat to it.lng }
                    if (map != null && points.isNotEmpty()) {
                        val settled = view.width > 0 && view.height > 0 &&
                            view.width == fitSize[0] && view.height == fitSize[1]
                        if (settled) {
                            fitIfNeeded(map, lat, lng, points)
                        } else {
                            fitSize[0] = view.width
                            fitSize[1] = view.height
                            // The card is mid-expansion; wait for layout to settle, then fit
                            // (retry once if it is still resizing).
                            view.postDelayed({
                                if (view.width == fitSize[0] && view.height == fitSize[1]) {
                                    fitIfNeeded(map, lat, lng, points)
                                } else {
                                    fitSize[0] = view.width
                                    fitSize[1] = view.height
                                    view.postDelayed({ fitIfNeeded(map, lat, lng, points) }, 400)
                                }
                            }, 400)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapState.mapView?.onStop()
            mapState.mapView?.onPause()
            mapState.mapView?.onDestroy()
        }
    }
}

private class MapState {
    var mapView: MapView? = null
    var map: MapLibreMap? = null
    var busSource: GeoJsonSource? = null
    var positions: List<BlockPosition> = emptyList()

    /** TEMP-DEBUG: file marker (logd is dead on the test device). */
    var debugOut: File? = null
    fun log(msg: String) {
        debugOut?.appendText("${System.currentTimeMillis()} $msg\n")
    }

    /** Pushes the latest bus positions into the GeoJsonSource (no-op until style is ready). */
    fun applyPositions() {
        val source = busSource ?: return
        val features = positions.map { bp ->
            val letter = routeTypeLetter(bp.routeNumber).ifBlank { "B" }
            val feature = Feature.fromGeometry(Point.fromLngLat(bp.lng, bp.lat))
            feature.addStringProperty("icon", "badge-$letter")
            feature.addNumberProperty("heading", bp.heading)
            feature
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
        log("applyPositions n=${features.size}")
    }
}

private fun drawableBitmap(context: Context, resId: Int, sizePx: Int): Bitmap {
    val d = ContextCompat.getDrawable(context, resId)
    return d?.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        ?: Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}

/** Colored circle badge with a white transit glyph, used as the bus marker image. */
private fun badgeBitmap(context: Context, fillColor: Int, glyphRes: Int, density: Float): Bitmap {
    val size = (34 * density).toInt()
    val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor })
    val glyph = drawableBitmap(context, glyphRes, (20 * density).toInt())
    c.drawBitmap(glyph, (size - glyph.width) / 2f, (size - glyph.height) / 2f, null)
    return out
}

/** Primary-colored dot with a white center, used as the stop marker image. */
private fun stopDotBitmap(context: Context, fillColor: Int, density: Float): Bitmap {
    val size = (34 * density).toInt()
    val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawCircle(size / 2f, size / 2f, size / 2f, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor })
    val dotRadius = (6 * density).toInt().toFloat()
    c.drawCircle(size / 2f, size / 2f, dotRadius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    return out
}

private fun fitIfNeeded(map: MapLibreMap, stopLat: Double, stopLng: Double, points: List<Pair<Double, Double>>) {
    val all = listOf(LatLng(stopLat, stopLng)) + points.map { LatLng(it.first, it.second) }
    val visible = map.projection.visibleRegion.latLngBounds
    if (all.any { !visible.contains(it) }) {
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
}

@Composable
private fun ArrivalItem(
    arrival: Arrival,
    context: Context,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val type = routeTypeLetter(arrival.routeId)
    val scheme = MaterialTheme.colorScheme
    val color = remember(type, scheme) {
        transitColor(type, scheme)
    }
    val displayTime = if (arrival.status == "estimated" && arrival.estimated != null) {
        arrival.estimated
    } else {
        arrival.scheduled
    }

    val formattedTime = if (displayTime != null) formatDateTime(displayTime, context) else ""
    val minutesAway = if (displayTime != null) minutesUntil(displayTime) else 0L
    val relativeText = if (minutesAway <= 0) "Due" else "${minutesAway} min"
    val isEstimated = arrival.status == "estimated"

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = color
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = transitIconResource(type)),
                        contentDescription = transitTypeLabel(type),
                        tint = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = arrival.shortSign,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = color
            ) {
                if (arrival.status == "canceled") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.arrival_cancelled),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (arrival.reason.isNotEmpty()) {
                            Text(
                                text = arrival.reason,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else if (arrival.dropOffOnly) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.arrival_dropoff_only),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (arrival.reason.isNotEmpty()) {
                            Text(
                                text = arrival.reason,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = relativeText,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = if (isEstimated) FontWeight.Bold else FontWeight.Normal
                        )
                        val delayText = formatDelay(arrival)
                        if (delayText != null) {
                            Text(
                                text = delayText,
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else if (!isEstimated) {
                            Text(
                                text = "scheduled",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun toggleFavorite(context: Context, locId: Int, stopName: String, currentlyFavorite: Boolean, routeId: Int = -1, lat: Double = 0.0, lng: Double = 0.0): String {
    return try {
        val db = DatabaseHelper(context.applicationContext)
        if (currentlyFavorite) {
            val writableDb = db.writableDatabase
            writableDb.delete("favorites", "loc_id = ?", arrayOf(locId.toString()))
            writableDb.close()
            context.getString(R.string.favorite_deleted_text)
        } else {
            val stop = Stop()
            stop.desc = stopName
            stop.locId = locId
            stop.latitude = lat
            stop.longitude = lng
            if (routeId > 0) stop.routeNum = routeId
            db.addFavorite(stop, null)
            context.getString(R.string.favorite_added_text)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to toggle favorite", e)
        "Failed to update favorite"
    }
}
