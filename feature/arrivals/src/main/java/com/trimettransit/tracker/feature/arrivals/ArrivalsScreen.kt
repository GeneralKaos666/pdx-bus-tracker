package com.trimettransit.tracker.feature.arrivals

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import timber.log.Timber
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.BlockPosition
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.domain.arrivalKey
import com.trimettransit.tracker.model.domain.dedupeArrivals
import com.trimettransit.tracker.model.domain.detoursForLine
import com.trimettransit.tracker.model.domain.filterArrivalsByRoute
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.NavState
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.badgeBitmap
import com.trimettransit.tracker.ui.components.EmptyState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.ListLoadingSkeleton
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberIsInPipMode
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.components.transitBadgeLetter
import com.trimettransit.tracker.ui.components.transitBadgeLetters
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.components.transitOnColor
import com.trimettransit.tracker.ui.components.transitTypeLabel
import com.trimettransit.tracker.util.formatDateTime
import com.trimettransit.tracker.util.minutesUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
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

private const val POSITION_REFRESH_MS = 15_000L
private const val PIP_REFRESH_MS = 20_000L
private const val ARRIVALS_REFRESH_MS = 30_000L
private const val STOP_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val STOP_MAP_STYLE_URL_DARK = "https://tiles.openfreemap.org/styles/dark"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArrivalsScreen(
    transitRepository: TransitRepository,
    favoritesRepository: FavoritesRepository,
    stopId: String,
    stopName: String,
    routeId: Int,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    isDark: Boolean = false,
) {
    val context = LocalContext.current
    var arrivals by remember { mutableStateOf<List<Arrival>>(emptyList()) }
    var blockPositions by remember { mutableStateOf<List<BlockPosition>>(emptyList()) }
    var detours by remember { mutableStateOf<List<Detour>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var selectedDetours by remember { mutableStateOf<List<Detour>?>(null) }
    var showAllArrivals by remember { mutableStateOf(false) }
    var trackingKey by remember { mutableStateOf<String?>(null) }
    var trackingRouteId by remember { mutableIntStateOf(-1) }
    var trackingVehicleId by remember { mutableIntStateOf(0) }
    var unfilteredArrivals by remember { mutableStateOf<List<Arrival>>(emptyList()) }
    var onlySelectedRoute by remember { mutableStateOf(true) }
    // 30s tick forcing the arrival rows' countdowns to recompute in the foreground,
    // so "8 min" doesn't sit frozen until the next manual refresh.
    // The loop itself lives below the lifecycle observer so it can pause in background.
    var countdownTick by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val locId = stopId.toIntOrNull() ?: 0
    val stopNumberLabel = stringResource(R.string.stop_number, stopId)
    var stopLat by remember { mutableDoubleStateOf(latitude) }
    var stopLng by remember { mutableDoubleStateOf(longitude) }
    var isLoadingStop by remember { mutableStateOf(false) }
    val hasValidCoords = !isLoadingStop && stopLat != 0.0 && stopLng != 0.0

    // Read initial favorite state from DB
    LaunchedEffect(stopId) {
        if (locId > 0) {
            NavState.arrivalsIsFavorite = withContext(Dispatchers.IO) {
                favoritesRepository.isFavorite(locId)
            }
        }
    }

    LaunchedEffect(locId) {
        if ((stopLat == 0.0 || stopLng == 0.0) && locId > 0) {
            isLoadingStop = true
            transitRepository.getStopById(locId)?.let { stop ->
                stopLat = stop.latitude
                stopLng = stop.longitude
            }
            isLoadingStop = false
            if (stopLat == 0.0 || stopLng == 0.0) {
                Timber.w("Stop #$locId has zero coordinates after fallback — map hidden")
            }
        }
    }

    var arrivalsJob by remember { mutableStateOf<Job?>(null) }

    /**
     * Re-fetches arrivals. [showLoading] toggles the loading UI; silent refreshes
     * (background cadence) keep the last good data if a fetch fails so the screen
     * the user is looking at never blinks into an error state.
     */
    fun refreshArrivals(showLoading: Boolean) {
        arrivalsJob?.cancel()
        arrivalsJob = coroutineScope.launch {
            if (showLoading) isLoading = true
            val result = transitRepository.getArrivals(
                locIds = listOf(locId),
                showPosition = true,
                minutes = 30,
                maxArrivals = 15
            )
            if (result != null) {
                val allArrivals = dedupeArrivals(result.arrivals)
                unfilteredArrivals = allArrivals
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                onlySelectedRoute = prefs.getBoolean("pref_key_only_show_route_selected", true)
                arrivals = filterArrivalsByRoute(
                    allArrivals,
                    if (onlySelectedRoute && routeId > 0) routeId else 0
                )
                detours = result.detours
                blockPositions = result.blockPositions
                // Resolve stop coordinates from arrivals response if not yet known
                if (stopLat == 0.0 || stopLng == 0.0) {
                    if (result.stopLat != 0.0 && result.stopLng != 0.0) {
                        stopLat = result.stopLat
                        stopLng = result.stopLng
                    }
                }
                isError = false
            } else if (showLoading) {
                arrivals = emptyList()
                isError = true
            }
            if (showLoading) isLoading = false
        }
    }

    fun loadArrivals() = refreshArrivals(showLoading = true)

    // Re-fetch arrivals on app re-entry (and initial composition via lifecycle observer)
    RememberOnResume { loadArrivals() }
    val visibleCount = minOf(arrivals.size, 5)
    val showExpandButton = arrivals.size > 5 || unfilteredArrivals.size > arrivals.size
    // Tracked positions: the tapped row's own vehicle when it reports a position,
    // otherwise that line's other live vehicles so the map is never empty.
    fun arrivalFor(bp: BlockPosition): Arrival? =
        unfilteredArrivals.firstOrNull { it.vehicleID == bp.vehicleID }
    val trackedPositions = if (trackingVehicleId > 0) {
        blockPositions.filter { it.vehicleID == trackingVehicleId }
            .ifEmpty { blockPositions.filter { arrivalFor(it)?.routeId == trackingRouteId } }
    } else {
        blockPositions.filter { arrivalFor(it)?.routeId == trackingRouteId }
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

    // Countdown tick: only advance while the app is resumed, so a backgrounded
    // screen doesn't keep waking the coroutine every 30s for invisible rows.
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            if (isAppResumed) countdownTick++
        }
    }

    var positionRefreshInFlight by remember { mutableStateOf(false) }
    fun refreshPositions() {
        if (positionRefreshInFlight) return
        positionRefreshInFlight = true
        coroutineScope.launch {
            try {
                val result = transitRepository.getArrivals(
                    locIds = listOf(locId),
                    showPosition = true,
                    minutes = 30,
                    maxArrivals = 15
                )
                if (result != null) {
                    blockPositions = result.blockPositions
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
        NavState.arrivalsStopName = stopName.ifBlank { stopNumberLabel }
    }

    val smoothFling = rememberSmoothFlingBehavior()
    val listState = rememberLazyListState()

    DisposableEffect(Unit) {
        // Must use a stable lambda — loadArrivals is a local fun, always the same behavior
        NavState.arrivalsOnRefresh = { loadArrivals() }
        // Collapsed bottom-bar pill: scroll back to the top and refresh.
        NavState.onScrollToTop = {
            coroutineScope.launch { listState.animateScrollToItem(0) }
            loadArrivals()
        }
        onDispose {
            NavState.clearArrivals()
        }
    }

    val inPip = rememberIsInPipMode()

    // PiP: keep the countdown live; drop the map card and alerts dialog
    // (they cannot render usefully in the small window).
    LaunchedEffect(inPip) {
        if (inPip) {
            trackingKey = null
            selectedDetours = null
            while (true) {
                delay(PIP_REFRESH_MS)
                try {
                    loadArrivals()
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.e(e, "PiP arrivals refresh failed")
                    isError = true
                }
            }
        }
    }

    // Foreground silent refresh: re-fetch while the user watches so a bus flipping
    // to drop-off-only (or a canceled/delayed status) shows up without a manual
    // pull-to-refresh. PiP skips this — it already refreshes on its own loop.
    LaunchedEffect(inPip) {
        if (inPip) return@LaunchedEffect
        while (true) {
            delay(ARRIVALS_REFRESH_MS)
            if (isAppResumed) refreshArrivals(showLoading = false)
        }
    }

    Crossfade(
        targetState = inPip,
        animationSpec = tween(durationMillis = 200),
        label = "pipMode"
    ) { pip ->
        if (pip) {
            PipCountdownContent(arrivals = arrivals, stopName = stopName, tick = countdownTick)
            return@Crossfade
        }

        // Bridge resolved coordinates to outer scaffold for favorite persistence
        LaunchedEffect(stopLat, stopLng) {
            NavState.arrivalsLat = stopLat
            NavState.arrivalsLng = stopLng
        }

    val pullToRefreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = { loadArrivals() },
        state = pullToRefreshState,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            PullToRefreshDefaults.LoadingIndicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isLoading,
                state = pullToRefreshState,
                color = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        }
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
                    ListLoadingSkeleton()
                }

                1 -> {
                    ErrorState(message = stringResource(R.string.arrivals_load_error))
                }

                2 -> {
                    EmptyState(
                        message = if (unfilteredArrivals.isNotEmpty())
                            stringResource(R.string.no_upcoming_for_route)
                        else stringResource(R.string.empty_no_arrivals)
                    )
                }

                else -> {
                    ContentEntrance(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            flingBehavior = smoothFling,
                            contentPadding = PaddingValues(
                                start = 12.dp,
                                end = 12.dp,
                                top = 8.dp,
                                bottom = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val visibleArrivals =
                                if (showAllArrivals) unfilteredArrivals else arrivals.take(5)
                            items(
                                visibleArrivals,
                                key = { "${if (showAllArrivals) "all_" else "top_"}${arrivalKey(it)}" },
                                contentType = { "arrival" }) { arrival ->
                                val lineDetours = detoursForLine(detours, arrival.routeId)
                                val rowKey =
                                    "${arrival.tripID}_${arrival.routeId}_${arrival.scheduledMillis}_${arrival.blockID}_${arrival.vehicleID}"
                                Column {
                                    ArrivalItem(
                                        arrival = arrival,
                                        context = context,
                                        refreshKey = countdownTick,
                                        lineDetours = lineDetours,
                                        onShowAlerts = { selectedDetours = lineDetours },
                                        onClick = {
                                            if (hasValidCoords) {
                                                if (trackingKey == rowKey) {
                                                    trackingKey =
                                                        null              // tap the tracked row again to close
                                                } else {
                                                    trackingKey =
                                                        rowKey            // opens under this row; switches if another row is tracked
                                                    trackingRouteId = arrival.routeId
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
                                            blockPositions = trackedPositions,
                                            arrivals = unfilteredArrivals,
                                            trackedVehicleId = trackingVehicleId,
                                            isDark = isDark
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
                                                text = if (showAllArrivals) stringResource(R.string.show_fewer)
                                                else pluralStringResource(R.plurals.show_all_arrivals, unfilteredArrivals.size - visibleCount, unfilteredArrivals.size - visibleCount),
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
                                                contentDescription = if (showAllArrivals) stringResource(R.string.collapse_arrivals)
                                                else stringResource(R.string.expand_arrivals),
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
                    }
                }
            }
        }
    }
    }

    selectedDetours?.let { detoursForDialog ->
        AlertsDialog(
            detours = detoursForDialog,
            onDismiss = { selectedDetours = null }
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
                text = stringResource(R.string.alerts_count, detours.size),
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
                            text = stringResource(R.string.bullet),
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
                Text(stringResource(R.string.close))
            }
        }
    )
}

private fun formatDelay(arrival: Arrival, context: Context): String? {
    if (arrival.status != "estimated" || arrival.estimatedMillis == 0L || arrival.scheduledMillis == 0L) return null
    val delayMin = (arrival.estimatedMillis - arrival.scheduledMillis) / 60000.0
    return when {
        delayMin >= 1.0 -> context.getString(R.string.arrival_delay_late, delayMin.roundToInt())
        delayMin <= -1.0 -> context.getString(R.string.arrival_delay_early, -delayMin.roundToInt())
        else -> context.getString(R.string.arrival_on_time)
    }
}

@Composable
private fun StopMapCard(
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
    val badgeColors = remember(scheme) {
        transitBadgeLetters().associateWith { transitColor(it, scheme) }
    }
    val badgeGlyphColors = remember(scheme) {
        transitBadgeLetters().associateWith { transitOnColor(it, scheme) }
    }
    val context = LocalContext.current
    val mapStyleUrl = if (isDark) STOP_MAP_STYLE_URL_DARK else STOP_MAP_STYLE_URL
    // MapLibre halo/text colors are chosen for legibility against the basemap: light basemap
    // wants a light halo over dark glyphs, the dark basemap wants a dark halo over light glyphs.
    val countdownTextColor = scheme.onSurface.toArgb()
    val countdownHaloColor = if (isDark) scheme.surface.toArgb() else android.graphics.Color.WHITE
    // Track which basemap is currently loaded so a theme change re-applies the style in place.
    var appliedStyleUrl by remember { mutableStateOf<String?>(null) }

    fun applyStopMapStyle(style: Style) {
        style.addImage(
            "stop-dot",
            stopDotBitmap(context, scheme.primary.toArgb(), scheme.onPrimary.toArgb(), density)
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
        AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        getMapAsync { map ->
                            mapState.map = map
                            map.uiSettings.isCompassEnabled = false
                            map.uiSettings.isAttributionEnabled = true
                            map.setMaxZoomPreference(18.0)
                            map.setStyle(mapStyleUrl) { style ->
                                applyStopMapStyle(style)
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngZoom(
                                        LatLng(lat, lng),
                                        16.0
                                    )
                                )
                                appliedStyleUrl = mapStyleUrl
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
                    // If the resolved basemap (light/dark) changed since it was last applied,
                    // reload the style and re-add our images/sources/layers before pushing data.
                    val map = mapState.map
                    if (map != null && appliedStyleUrl != mapStyleUrl) {
                        appliedStyleUrl = mapStyleUrl
                        map.setStyle(mapStyleUrl) { style ->
                            applyStopMapStyle(style)
                            mapState.applyPositions()
                        }
                    }
                    mapState.positions = blockPositions
                    mapState.arrivals = arrivals
                    mapState.applyPositions()
                    // Follow the tracked bus instead of framing the stop together with it,
                    // so the camera stays centered on the vehicle and its "N min" label never
                    // clips at the map's top edge. The stop marker still renders but simply
                    // scrolls out of frame once a bus position is available.
                    if (map != null && view.width > 0 && view.height > 0) {
                        trackedTarget(blockPositions, trackedVehicleId)?.let { target ->
                            keepBusCentered(map, target, view.width, view.height, density)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
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

    /** Resolved "Dropoff Only" label, set when the map is configured. */
    var dropoffLabel: String = ""

    /** Resolved countdown labels, set when the map is configured. */
    var countdownDue: String = ""
    var countdownMinFormat: String = ""

    /** Pushes the latest bus positions into the GeoJsonSource (no-op until style is ready). */
    fun applyPositions() {
        val source = busSource ?: return
        val features = positions
            .filter { it.lat != 0.0 || it.lng != 0.0 }
            .map { bp ->
            val letter = transitBadgeLetter(bp.routeNumber).ifBlank { "B" }
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
                    if (mins <= 0) countdownDue else countdownMinFormat.format(mins)
                } else ""
            }
            feature.addStringProperty("countdown", label)
            feature
        }
        source.setGeoJson(FeatureCollection.fromFeatures(features))
    }
}

/** Primary-colored dot with a contrasting center, used as the stop marker image. */
private fun stopDotBitmap(
    context: Context,
    fillColor: Int,
    centerColor: Int = android.graphics.Color.WHITE,
    density: Float
): Bitmap {
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
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = centerColor })
    return out
}

/** The bus to follow: the tracked vehicle, else the first live position on that route. */
private fun trackedTarget(
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
    val p = map.projection.toScreenLocation(target)
    val outside = p.x < marginPx || p.x > viewWidth - marginPx ||
            p.y < topMarginPx || p.y > viewHeight - marginPx
    if (outside) {
        map.easeCamera(CameraUpdateFactory.newLatLng(target), 400)
    }
}

@Composable
private fun ArrivalItem(
    arrival: Arrival,
    context: Context,
    modifier: Modifier = Modifier,
    refreshKey: Int = 0,
    lineDetours: List<Detour> = emptyList(),
    onShowAlerts: (List<Detour>) -> Unit = {},
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
                        contentDescription = stringResource(transitTypeLabel(type)),
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

            AnimatedVisibility(
                visible = lineDetours.isNotEmpty(),
                enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.6f, animationSpec = tween(200)),
                exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.6f, animationSpec = tween(150))
            ) {
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    val alertInteractionSource = remember { MutableInteractionSource() }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier
                            .pressScale(alertInteractionSource)
                            .clickable(
                                interactionSource = alertInteractionSource,
                                indication = LocalIndication.current
                            ) { onShowAlerts(lineDetours) }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_alert_warning),
                                contentDescription = stringResource(R.string.show_alerts_for_route, arrival.routeId),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.onSurface
            ) {
                if (arrival.status == "canceled") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = context.getString(R.string.arrival_cancelled),
                            color = MaterialTheme.colorScheme.surface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (arrival.reason.isNotEmpty()) {
                            Text(
                                text = arrival.reason,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
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
                            color = MaterialTheme.colorScheme.surface,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        if (arrival.reason.isNotEmpty()) {
                            Text(
                                text = arrival.reason,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        AnimatedCountdownText(
                            minutesAway = minutesAway,
                            isEstimated = isEstimated,
                            color = MaterialTheme.colorScheme.surface,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val delayText = formatDelay(arrival, context)
                        if (delayText != null) {
                            Text(
                                text = delayText,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelSmall
                            )
                        } else if (!isEstimated) {
                            Text(
                                text = stringResource(R.string.scheduled),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
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
    favoritesRepository: FavoritesRepository,
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
            if (currentlyFavorite) {
                if (favoritesRepository.removeFavorite(locId)) {
                    true to context.getString(R.string.favorite_deleted_text)
                } else {
                    // Stop was already absent — the DB already matches the unfavorited UI state.
                    true to context.getString(R.string.favorite_does_not_exist_text)
                }
            } else {
                val stop = Stop(
                    desc = stopName,
                    latitude = lat,
                    longitude = lng,
                    routeNum = if (routeId > 0) routeId else 0,
                    locId = locId
                )
                if (favoritesRepository.addFavorite(stop)) {
                    true to context.getString(R.string.favorite_added_text)
                } else {
                    // Already a favorite — the DB already matches the favorited UI state.
                    true to context.getString(R.string.favorite_exists_text)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to toggle favorite")
            false to context.getString(R.string.failed_to_update_favorite)
        }
    }
}

@Composable
private fun PipCountdownContent(
    arrivals: List<Arrival>,
    stopName: String,
    modifier: Modifier = Modifier,
    tick: Int = 0
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = stopName.ifBlank { stringResource(R.string.stop) },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        if (arrivals.isEmpty()) {
            Text(
                text = stringResource(R.string.no_upcoming_arrivals),
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
                            text = stringResource(R.string.canceled),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.error
                        )
                    } else if (arrival.dropOffOnly) {
                        Text(
                            text = stringResource(R.string.dropoff_only),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSurfaceVariant
                        )
                    } else {
                        AnimatedCountdownText(
                            minutesAway = minutesAway,
                            isEstimated = arrival.status == "estimated",
                            color = color,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                }
            }
        }
    }
}

/**
 * Countdown "N min" / "Due" label that rolls with minute changes instead of snapping.
 * The tick updates the minute bucket every ~30s; AnimatedContent slides the change in the
 * direction of the countdown (decreasing rolls in from the right) and fades the swap.
 */
@Composable
private fun AnimatedCountdownText(
    minutesAway: Long,
    isEstimated: Boolean,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    AnimatedContent(
        targetState = minutesAway.coerceAtLeast(0L),
        modifier = modifier,
        transitionSpec = {
            val decreasing = targetState < initialState
            val enter = (if (decreasing) {
                slideInHorizontally(tween(250)) { it / 3 }
            } else {
                slideInHorizontally(tween(250)) { -it / 3 }
            }) + fadeIn(tween(250))
            val exit = (if (decreasing) {
                slideOutHorizontally(tween(180)) { -it / 3 }
            } else {
                slideOutHorizontally(tween(180)) { it / 3 }
            }) + fadeOut(tween(180))
            enter togetherWith exit
        },
        label = "countdownRoll"
    ) { minutes ->
        Text(
            text = if (minutes <= 0) stringResource(R.string.due) else stringResource(R.string.minutes, minutes),
            color = color,
            style = style,
            fontWeight = if (isEstimated) FontWeight.Bold else FontWeight.Normal
        )
    }
}
