package com.trimettransit.tracker.feature.arrivals

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import timber.log.Timber
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.BlockPosition
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.transit.TransitApi
import com.trimettransit.tracker.ui.NavState
import com.trimettransit.tracker.ui.components.AutoHideBottomBarEffect
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.EmptyState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.LoadingState
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberIsInPipMode
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.components.transitBadgeLetter
import com.trimettransit.tracker.ui.components.transitBadgeLetters
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.components.transitTypeLabel
import com.trimettransit.tracker.util.formatDateTime
import com.trimettransit.tracker.util.minutesUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
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
) {
    val context = LocalContext.current
    var arrivals by remember { mutableStateOf<List<Arrival>>(emptyList()) }
    var blockPositions by remember { mutableStateOf<List<BlockPosition>>(emptyList()) }
    var detours by remember { mutableStateOf<List<Detour>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var showAlertsDialog by remember { mutableStateOf(false) }
    var showAllArrivals by remember { mutableStateOf(false) }
    var trackingKey by remember { mutableStateOf<String?>(null) }
    var trackingRouteId by remember { mutableIntStateOf(-1) }
    var trackingSign by remember { mutableStateOf("") }
    var trackingVehicleId by remember { mutableIntStateOf(0) }
    var unfilteredArrivals by remember { mutableStateOf<List<Arrival>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val locId = stopId.toIntOrNull() ?: 0
    var stopLat by remember { mutableDoubleStateOf(latitude) }
    var stopLng by remember { mutableDoubleStateOf(longitude) }
    var isLoadingStop by remember { mutableStateOf(false) }
    val hasValidCoords = !isLoadingStop && stopLat != 0.0 && stopLng != 0.0

    // Read initial favorite state from DB
    LaunchedEffect(stopId) {
        if (locId > 0) {
            NavState.arrivalsIsFavorite = withContext(Dispatchers.IO) {
                DatabaseHelper(context.applicationContext).isFavorite(locId)
            }
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
                Timber.w("Stop #$locId has zero coordinates after fallback — map hidden")
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
            if (result != null) {
                val allArrivals = result.arrivals ?: emptyList()
                unfilteredArrivals = allArrivals
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val onlySelectedRoute = prefs.getBoolean("pref_key_only_show_route_selected", true)
                arrivals = if (onlySelectedRoute && routeId > 0) {
                    allArrivals.filter { it.routeId == routeId }
                } else {
                    allArrivals
                }
                detours = result.detours ?: emptyList()
                blockPositions = result.blockPositions ?: emptyList()
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
    RememberOnResume { loadArrivals() }
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
                if (result != null) {
                    blockPositions = result.blockPositions ?: emptyList()
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
    val listState = rememberLazyListState()
    AutoHideBottomBarEffect(listState)

    // Progress of the alerts button collapsing into the pinned strip as it
    // slides above the viewport top: 0 = at rest, 1 = fully scrolled off.
    // Once the button leaves the viewport entirely, the strip stays collapsed
    // (progress 1) until the button scrolls back into view.
    var alertsProgress by remember { mutableFloatStateOf(0f) }
    var alertsItemSeen by remember { mutableStateOf(false) }
    LaunchedEffect(listState) {
        snapshotFlow {
            val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == "alerts" }
            (item?.offset ?: 0) to (item?.size ?: 0)
        }.collect { (offset, size) ->
            if (size > 0) {
                alertsItemSeen = true
                alertsProgress = if (offset < 0) {
                    (-offset.toFloat() / size).coerceIn(0f, 1f)
                } else {
                    0f
                }
            } else if (alertsItemSeen) {
                alertsProgress = 1f
            }
        }
    }

    val inPip = rememberIsInPipMode()

    // PiP: keep the countdown live; drop the map card and alerts dialog
    // (they cannot render usefully in the small window).
    LaunchedEffect(inPip) {
        if (inPip) {
            trackingKey = null
            showAlertsDialog = false
            while (true) {
                delay(POSITION_REFRESH_MS)
                loadArrivals()
            }
        }
    }

    if (inPip) {
        PipCountdownContent(arrivals = arrivals, stopName = stopName)
        return
    }

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
        Crossfade(
            targetState = when {
                isLoading && arrivals.isEmpty() -> 0
                isError && arrivals.isEmpty() -> 1
                arrivals.isEmpty() -> 2
                else -> 3
            },
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "arrivalsState"
        ) { state ->
            when (state) {
                0 -> {
                    LoadingState()
                }

                1 -> {
                    ErrorState(message = "Unable to load arrivals.\nPull to retry.")
                }

                2 -> {
                    EmptyState(
                        message = if (unfilteredArrivals.isNotEmpty())
                            "No upcoming arrivals for this route.\nArrivals from other routes are hidden — disable the filter in Settings."
                        else "No upcoming arrivals."
                    )
                }

                else -> {
                    ContentEntrance(modifier = Modifier.fillMaxSize()) {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            flingBehavior = smoothFling,
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (detours.isNotEmpty()) {
                                item(key = "alerts", contentType = "alerts") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                    )
                                }
                            }

                            val visibleArrivals =
                                if (showAllArrivals) unfilteredArrivals else arrivals.take(5)
                            items(
                                visibleArrivals,
                                key = { "${it.tripID}_${it.routeId}_${it.scheduledMillis}" },
                                contentType = { "arrival" }) { arrival ->
                                        val rowKey =
                                            "${arrival.tripID}_${arrival.routeId}_${arrival.scheduledMillis}"
                                        Column {
                                            ArrivalItem(
                                                arrival = arrival,
                                                context = context,
                                                onClick = {
                                                    if (hasValidCoords) {
                                                        if (trackingKey == rowKey) {
                                                            trackingKey =
                                                                null              // tap the tracked row again to close
                                                        } else {
                                                            trackingKey =
                                                                rowKey            // opens under this row; switches if another row is tracked
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
                                                    arrivals = arrivals,
                                                    headerText = trackingSign.ifBlank { stopName },
                                                    onClose = { trackingKey = null }
                                                )
                                            }
                                        }
                                    }

                                    if (showExpandButton) {
                                        item(key = "showAll", contentType = "showAll") {
                                            val interactionSource =
                                                remember { MutableInteractionSource() }
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = MaterialTheme.shapes.large,
                                                color = MaterialTheme.colorScheme.surfaceContainerLow
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .pressScale(interactionSource)
                                                        .clickable(
                                                            interactionSource = interactionSource,
                                                            indication = LocalIndication.current
                                                        ) { showAllArrivals = !showAllArrivals }
                                                        .padding(
                                                            horizontal = 16.dp,
                                                            vertical = 12.dp
                                                        ),
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
                                                        animationSpec = tween(
                                                            durationMillis = 350,
                                                            easing = FastOutSlowInEasing
                                                        )
                                                    )
                                                    Icon(
                                                        imageVector = Icons.Default.KeyboardArrowDown,
                                                        contentDescription = if (showAllArrivals) "Collapse arrivals"
                                                        else "Expand arrivals",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.rotate(
                                                            showAllArrowRotation
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                            if (detours.isNotEmpty()) {
                                val stripWidth = 40.dp * (1 - alertsProgress) + maxWidth * alertsProgress
                                val stripHeight = 40.dp * (1 - alertsProgress) + 4.dp * alertsProgress
                                val stripOffsetX = -12.dp * (1 - alertsProgress)
                                val stripOffsetY = 12.dp * (1 - alertsProgress)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = stripOffsetX, y = stripOffsetY)
                                        .width(stripWidth)
                                        .height(stripHeight)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            lerp(
                                                MaterialTheme.colorScheme.errorContainer,
                                                MaterialTheme.colorScheme.error,
                                                alertsProgress
                                            )
                                        )
                                        .clickable(
                                            enabled = alertsProgress < 0.5f
                                        ) { showAlertsDialog = true },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_alert_warning),
                                        contentDescription = "Show alerts",
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier
                                            .size(22.dp)
                                            .graphicsLayer {
                                                alpha = 1 - alertsProgress
                                                scaleX = (1 - alertsProgress).coerceAtLeast(0.01f)
                                                scaleY = (1 - alertsProgress).coerceAtLeast(0.01f)
                                            }
                                    )
                                }
                                Text(
                                    text = detours.size.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset {
                                            val p = 1 - alertsProgress
                                            IntOffset(
                                                x = (-4.dp * p).roundToPx(),
                                                y = (4.dp * p).roundToPx()
                                            )
                                        }
                                        .graphicsLayer {
                                            alpha = 1 - alertsProgress
                                            scaleX = (1 - alertsProgress).coerceAtLeast(0.01f)
                                            scaleY = (1 - alertsProgress).coerceAtLeast(0.01f)
                                        }
                                        .background(
                                            MaterialTheme.colorScheme.error,
                                            CircleShape
                                        )
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAlertsDialog && detours.isNotEmpty()) {
        AlertsDialog(
            detours = detours,
            onDismiss = { showAlertsDialog = false }
        )
    }
}

@Composable
private fun AlertsDialog(
    detours: List<Detour>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Alerts (${detours.size})",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
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
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = detour.desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
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

@Composable
private fun StopMapCard(
    lat: Double,
    lng: Double,
    stopName: String,
    modifier: Modifier = Modifier,
    blockPositions: List<BlockPosition> = emptyList(),
    arrivals: List<Arrival> = emptyList(),
    headerText: String? = null,
    onClose: (() -> Unit)? = null,
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
        transitBadgeLetters().associateWith { transitColor(it, scheme) }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.large,
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
                        val interactionSource = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = onClose,
                            interactionSource = interactionSource,
                            modifier = Modifier.pressScale(interactionSource)
                        ) {
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
                        getMapAsync { map ->
                            mapState.map = map
                            map.uiSettings.isCompassEnabled = false
                            map.uiSettings.isAttributionEnabled = true
                            map.setMaxZoomPreference(18.0)
                            map.setStyle(STOP_MAP_STYLE_URL) { style ->
                                style.addImage(
                                    "stop-dot",
                                    stopDotBitmap(ctx, scheme.primary.toArgb(), density)
                                )
                                badgeColors.forEach { (letter, color) ->
                                    style.addImage(
                                        "badge-$letter",
                                        badgeBitmap(
                                            ctx,
                                            color.toArgb(),
                                            transitIconResource(letter),
                                            density
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
                                        textColor(scheme.onSurface.toArgb()),
                                        textHaloColor(android.graphics.Color.WHITE),
                                        textHaloWidth(2f),
                                        textAllowOverlap(true),
                                        textIgnorePlacement(true),
                                        textFont(arrayOf("Noto Sans Bold"))
                                    )
                                )
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(lat, lng),
                                        16.0
                                    )
                                )
                                mapState.applyPositions()   // in case update ran before style load
                            }
                        }
                        // Consume single-finger touches at View level to prevent
                        // propagation to Compose parent gesture handlers
                        // (pull-to-refresh, nav drawer). Multi-touch zoom unaffected.
                        setOnTouchListener { _, event ->
                            if (event.actionMasked == MotionEvent.ACTION_UP) performClick()
                            event.pointerCount < 2
                        }
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
                    mapState.arrivals = arrivals
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
    var arrivals: List<Arrival> = emptyList()

    /** Pushes the latest bus positions into the GeoJsonSource (no-op until style is ready). */
    fun applyPositions() {
        val source = busSource ?: return
        val features = positions.map { bp ->
            val letter = transitBadgeLetter(bp.routeNumber).ifBlank { "B" }
            val feature = Feature.fromGeometry(Point.fromLngLat(bp.lng, bp.lat))
            feature.addStringProperty("icon", "badge-$letter")
            feature.addNumberProperty("bearing", bp.bearing)
            // Time-left label shown above the icon: the tracked arrival for this vehicle,
            // phrased exactly like the list rows. Empty string renders nothing on the map.
            val match = arrivals.firstOrNull { it.vehicleID == bp.vehicleID }
            val displayTime = match?.let { a ->
                if (a.status == "estimated" && a.estimated != null) a.estimated else a.scheduled
            }
            val label = if (displayTime != null) {
                val mins = minutesUntil(displayTime)
                if (mins <= 0) "Due" else "$mins min"
            } else ""
            feature.addStringProperty("countdown", label)
            feature
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}

private fun drawableBitmap(context: Context, resId: Int, sizePx: Int): Bitmap {
    val d = ContextCompat.getDrawable(context, resId)
    return d?.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        ?: createBitmap(1, 1, Bitmap.Config.ARGB_8888)
}

/** Colored circle badge with a white transit glyph, used as the bus marker image. */
private fun badgeBitmap(context: Context, fillColor: Int, glyphRes: Int, density: Float): Bitmap {
    val size = (34 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawCircle(
        size / 2f,
        size / 2f,
        size / 2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor })
    val glyph = drawableBitmap(context, glyphRes, (20 * density).toInt())
    c.drawBitmap(glyph, (size - glyph.width) / 2f, (size - glyph.height) / 2f, null)
    return out
}

/** Primary-colored dot with a white center, used as the stop marker image. */
private fun stopDotBitmap(context: Context, fillColor: Int, density: Float): Bitmap {
    val size = (34 * density).toInt()
    val out = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val c = Canvas(out)
    c.drawCircle(
        size / 2f,
        size / 2f,
        size / 2f,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = fillColor })
    val dotRadius = (6 * density).toInt().toFloat()
    c.drawCircle(
        size / 2f,
        size / 2f,
        dotRadius,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = android.graphics.Color.WHITE })
    return out
}

private fun fitIfNeeded(
    map: MapLibreMap,
    stopLat: Double,
    stopLng: Double,
    points: List<Pair<Double, Double>>
) {
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val type = transitBadgeLetter(arrival.routeId)
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

    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = lerp(
                MaterialTheme.colorScheme.surfaceContainerLow,
                color,
                0.10f
            )
        ),
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
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
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
                        Crossfade(
                            targetState = relativeText,
                            animationSpec = tween(
                                durationMillis = 300,
                                easing = FastOutSlowInEasing
                            ),
                            label = "countdownText"
                        ) { text ->
                            Text(
                                text = text,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (isEstimated) FontWeight.Bold else FontWeight.Normal
                            )
                        }
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

suspend fun toggleFavorite(
    context: Context,
    locId: Int,
    stopName: String,
    currentlyFavorite: Boolean,
    routeId: Int = -1,
    lat: Double = 0.0,
    lng: Double = 0.0
): Pair<Boolean, String> {
    return withContext(Dispatchers.IO) {
        try {
            val db = DatabaseHelper(context.applicationContext)
            if (currentlyFavorite) {
                if (db.removeFavorite(locId)) {
                    true to context.getString(R.string.favorite_deleted_text)
                } else {
                    // Stop was already absent — the DB already matches the unfavorited UI state.
                    true to context.getString(R.string.favorite_does_not_exist_text)
                }
            } else {
                val stop = Stop()
                stop.desc = stopName
                stop.locId = locId
                stop.latitude = lat
                stop.longitude = lng
                if (routeId > 0) stop.routeNum = routeId
                if (db.addFavorite(stop)) {
                    true to context.getString(R.string.favorite_added_text)
                } else {
                    // Already a favorite — the DB already matches the favorited UI state.
                    true to context.getString(R.string.favorite_exists_text)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle favorite")
            false to "Failed to update favorite"
        }
    }
}

@Composable
private fun PipCountdownContent(
    arrivals: List<Arrival>,
    stopName: String,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = stopName.ifBlank { "Stop" },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        if (arrivals.isEmpty()) {
            Text(
                text = "No upcoming arrivals",
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        } else {
            arrivals.take(5).forEach { arrival ->
                val type = transitBadgeLetter(arrival.routeId)
                val color = transitColor(type, scheme)
                val displayTime = if (arrival.status == "estimated" && arrival.estimated != null) {
                    arrival.estimated
                } else {
                    arrival.scheduled
                }
                val minutesAway = if (displayTime != null) minutesUntil(displayTime) else 0L
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(26.dp),
                        shape = CircleShape,
                        color = color
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(id = transitIconResource(type)),
                                contentDescription = null,
                                tint = scheme.surface,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = arrival.shortSign,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (arrival.status == "canceled") {
                        Text(
                            text = "Canceled",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.error
                        )
                    } else {
                        Text(
                            text = if (minutesAway <= 0) "Due" else "${minutesAway} min",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }
        }
    }
}
