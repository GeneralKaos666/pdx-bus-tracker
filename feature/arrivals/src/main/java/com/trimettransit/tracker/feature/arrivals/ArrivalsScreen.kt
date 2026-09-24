package com.trimettransit.tracker.feature.arrivals

import timber.log.Timber
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.BlockPosition
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.TransitAlert
import com.trimettransit.tracker.model.domain.arrivalKey
import com.trimettransit.tracker.model.domain.dedupeArrivals
import com.trimettransit.tracker.model.domain.detoursForLine
import com.trimettransit.tracker.model.domain.alertsForLine
import com.trimettransit.tracker.model.domain.filterArrivalsByRoute
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.appearance.AppearancePrefs
import com.trimettransit.tracker.ui.appearance.refreshDelayMillis
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.ListStateShell
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberIsInPipMode
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.m3ContentExpand
import com.trimettransit.tracker.ui.theme.m3ContentShrink
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.util.SingleJobRunner
import com.trimettransit.tracker.util.nextMinuteBoundaryDelayMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val POSITION_REFRESH_MS = 15_000L
private const val PIP_REFRESH_MS = 20_000L
private const val ARRIVALS_FETCH_MINUTES = 30
private const val ARRIVALS_FETCH_MAX = 15
private const val PREF_TAP_TO_TRACK_HINT_SHOWN = "pref_key_tap_to_track_hint_shown"

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ArrivalsScreen(
    transitRepository: TransitRepository,
    favoritesRepository: FavoritesRepository,
    stopId: Int,
    stopName: String,
    routeId: Int,
    latitude: Double = 0.0,
    longitude: Double = 0.0,
    isDark: Boolean = false,
    onArrivalsStateChange: (stopName: String, isFavorite: Boolean, lat: Double, lng: Double) -> Unit,
    onRegisterRefresh: ((() -> Unit)?) -> Unit,
    onRegisterScrollToTop: ((() -> Unit)?) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var arrivals by remember { mutableStateOf<List<Arrival>>(emptyList()) }
    var blockPositions by remember { mutableStateOf<List<BlockPosition>>(emptyList()) }
    var detours by remember { mutableStateOf<List<Detour>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var selectedDetours by remember { mutableStateOf<List<Detour>?>(null) }
    var alerts by remember { mutableStateOf<List<TransitAlert>>(emptyList()) }
    var selectedAlerts by remember { mutableStateOf<List<TransitAlert>?>(null) }
    var selectedTripId by remember { mutableStateOf<String?>(null) }
    var selectedTripStatus by remember { mutableStateOf<com.trimettransit.tracker.model.TripStatus?>(null) }
    var isTripStatusLoading by remember { mutableStateOf(false) }
    var showAllArrivals by remember { mutableStateOf(false) }
    var trackingKey by remember { mutableStateOf<String?>(null) }
    var trackingRouteId by remember { mutableIntStateOf(-1) }
    var trackingVehicleId by remember { mutableIntStateOf(0) }
    var unfilteredArrivals by remember { mutableStateOf<List<Arrival>>(emptyList()) }
    var onlySelectedRoute by remember { mutableStateOf(true) }
    // Minute-aligned tick forcing the arrival rows' countdowns to recompute in the
    // foreground, so "8 min" doesn't sit frozen until the next manual refresh.
    // The loop itself lives below the lifecycle observer so it can pause in background.
    var countdownTick by remember { mutableIntStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val stopNumberLabel = stringResource(R.string.stop_number, stopId)
    var stopLat by remember { mutableDoubleStateOf(latitude) }
    var stopLng by remember { mutableDoubleStateOf(longitude) }
    var isLoadingStop by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    val hasValidCoords = !isLoadingStop && stopLat != 0.0 && stopLng != 0.0

    // One-time "tap to track" hint: shown once ever, on the first visit where the
    // tracking map can actually open (arrivals loaded + valid coordinates).
    var showTapHintDialog by remember {
        mutableStateOf(
            !PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_TAP_TO_TRACK_HINT_SHOWN, false)
        )
    }
    fun dismissTapToTrackHint() {
        showTapHintDialog = false
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit { putBoolean(PREF_TAP_TO_TRACK_HINT_SHOWN, true) }
    }

    // Read initial favorite state from DB
    LaunchedEffect(stopId) {
        if (stopId > 0) {
            isFavorite = withContext(Dispatchers.IO) {
                favoritesRepository.isFavorite(stopId)
            }
            onArrivalsStateChange(stopName.ifBlank { stopNumberLabel }, isFavorite, stopLat, stopLng)
        }
    }

    LaunchedEffect(stopId) {
        if ((stopLat == 0.0 || stopLng == 0.0) && stopId > 0) {
            isLoadingStop = true
            transitRepository.getStopById(stopId)?.let { stop ->
                stopLat = stop.latitude
                stopLng = stop.longitude
            }
            isLoadingStop = false
            if (stopLat == 0.0 || stopLng == 0.0) {
                Timber.w("Stop #$stopId has zero coordinates after fallback — map hidden")
            }
        }
    }

    // Single-flight runner: cancels any in-flight arrivals fetch when a newer
    // one is launched, so a slower superseded read can't overwrite newer data.
    val arrivalsRunner = remember { SingleJobRunner(coroutineScope) }

    /**
     * Re-fetches arrivals. [showLoading] toggles the loading UI; silent refreshes
     * (background cadence) keep the last good data if a fetch fails so the screen
     * the user is looking at never blinks into an error state.
     */
    fun refreshArrivals(showLoading: Boolean) {
        arrivalsRunner.launch {
            if (showLoading) isLoading = true
            val result = transitRepository.getArrivals(
                locIds = listOf(stopId),
                showPosition = true,
                minutes = ARRIVALS_FETCH_MINUTES,
                maxArrivals = ARRIVALS_FETCH_MAX
            )
            if (result != null) {
                val allArrivals = dedupeArrivals(result.arrivals)
                unfilteredArrivals = allArrivals
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                onlySelectedRoute = prefs.getBoolean(
                    AppearancePrefs.ARRIVALS_ONLY_SELECTED_ROUTE,
                    AppearancePrefs.DEFAULT_ONLY_SHOW_SELECTED_ROUTE
                )
                arrivals = filterArrivalsByRoute(
                    allArrivals,
                    if (onlySelectedRoute && routeId > 0) routeId else 0
                )
                detours = result.detours
                blockPositions = result.blockPositions
                // Scoped Alerts V2 fetch: routes seen in this stop's arrivals plus the
                // stop itself, mirroring detoursForLine/alertsForLine's per-line intent —
                // never an unscoped/system-wide fetch (TransitApi refuses those outright).
                val alertRouteIds = allArrivals.map { it.routeId }.filter { it > 0 }.distinct()
                alerts = if (alertRouteIds.isNotEmpty() || stopId > 0) {
                    transitRepository.getAlerts(
                        routes = alertRouteIds.ifEmpty { null },
                        locIds = if (stopId > 0) listOf(stopId) else null
                    ) ?: alerts
                } else {
                    emptyList()
                }
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
    val visibleCount = minOf(arrivals.size, TOP_ARRIVAL_ROWS)
    val showExpandButton = arrivals.size > TOP_ARRIVAL_ROWS || unfilteredArrivals.size > arrivals.size
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

    // Countdown tick: minute-aligned to the wall clock so a row's "8 min -> 7 min"
    // flip rolls exactly on the minute. The loop sleeps while backgrounded instead of
    // waking every minute for invisible rows, and fires immediately on resume so the
    // countdown never sits up to a minute stale after returning to the app.
    LaunchedEffect(isAppResumed) {
        if (!isAppResumed) return@LaunchedEffect
        countdownTick++
        while (true) {
            delay(nextMinuteBoundaryDelayMillis())
            countdownTick++
        }
    }

    var positionRefreshInFlight by remember { mutableStateOf(false) }
    fun refreshPositions() {
        if (positionRefreshInFlight) return
        positionRefreshInFlight = true
        coroutineScope.launch {
            try {
                val result = transitRepository.getArrivals(
                    locIds = listOf(stopId),
                    showPosition = true,
                    minutes = ARRIVALS_FETCH_MINUTES,
                    maxArrivals = ARRIVALS_FETCH_MAX
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

    // Report the resolved stop name to the outer scaffold's top bar
    LaunchedEffect(Unit) {
        onArrivalsStateChange(stopName.ifBlank { stopNumberLabel }, isFavorite, stopLat, stopLng)
    }

    val smoothFling = rememberSmoothFlingBehavior()
    val listState = rememberLazyListState()

    // Hoisted above the LazyColumn so per-row recompositions (every minute tick)
    // don't re-hit SharedPreferences for every visible arrival.
    val showClock = remember { prefs.getBoolean(AppearancePrefs.ARRIVALS_SHOW_CLOCK, true) }
    val showRouteBadge = remember { prefs.getBoolean(AppearancePrefs.ARRIVALS_SHOW_ROUTE_BADGES, true) }
    val showVehicleInfo = remember { prefs.getBoolean(AppearancePrefs.ARRIVALS_SHOW_VEHICLE_INFO, true) }

    DisposableEffect(Unit) {
        // Must use a stable lambda — loadArrivals is a local fun, always the same behavior
        onRegisterRefresh { loadArrivals() }
        // Collapsed bottom-bar pill: scroll back to the top and refresh.
        onRegisterScrollToTop {
            coroutineScope.launch { listState.animateScrollToItem(0) }
            loadArrivals()
        }
        onDispose {
            onRegisterRefresh(null)
            onRegisterScrollToTop(null)
            onArrivalsStateChange("", false, 0.0, 0.0)
        }
    }

    val inPip = rememberIsInPipMode()

    // PiP: keep the countdown live; drop the map card and alerts dialog
    // (they cannot render usefully in the small window).
    LaunchedEffect(inPip) {
        if (inPip) {
            trackingKey = null
            selectedDetours = null
            selectedAlerts = null
            while (true) {
                delay(PIP_REFRESH_MS)
                loadArrivals()
            }
        }
    }

    // Foreground silent refresh: re-fetch while the user watches so a bus flipping
    // to drop-off-only (or a canceled/delayed status) shows up without a manual
    // pull-to-refresh. PiP skips this — it already refreshes on its own loop.
    // The cadence is user-tunable (15s–5min); re-read each cycle so changes apply live.
    LaunchedEffect(inPip) {
        if (inPip) return@LaunchedEffect
        while (true) {
            val delayMs = prefs.refreshDelayMillis()
            delay(delayMs)
            if (isAppResumed) refreshArrivals(showLoading = false)
        }
    }

    Crossfade(
        targetState = inPip,
        animationSpec = m3EffectsDefault(),
        label = "pipMode"
    ) { pip ->
        if (pip) {
            PipCountdownContent(arrivals = arrivals, stopName = stopName, tick = countdownTick)
            return@Crossfade
        }

        // Bridge resolved coordinates to the outer scaffold for favorite persistence.
        // Re-read the favorite from the DB: the top bar may have toggled it since the
        // initial read (e.g. PiP exit re-fires this effect), so the local mirror is stale.
        LaunchedEffect(stopLat, stopLng) {
            if (stopId > 0) {
                isFavorite = withContext(Dispatchers.IO) { favoritesRepository.isFavorite(stopId) }
            }
            onArrivalsStateChange(stopName.ifBlank { stopNumberLabel }, isFavorite, stopLat, stopLng)
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
        ListStateShell(
            isLoading = isLoading,
            isError = isError,
            isEmpty = arrivals.isEmpty(),
            emptyMessage = if (unfilteredArrivals.isNotEmpty())
                stringResource(R.string.no_upcoming_for_route)
            else stringResource(R.string.empty_no_arrivals),
            errorMessage = stringResource(R.string.arrivals_load_error),
            label = "arrivalsState",
            onRetry = { loadArrivals() }
        ) {
            ContentEntrance(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    flingBehavior = smoothFling,
                    contentPadding = PaddingValues(
                        start = 12.dp,
                                end = 12.dp,
                                top = 8.dp,
                                bottom = navPillBottomPadding() + 8.dp
                            ),
                            // Centres a short list instead of leaving a void under it. `spacedBy`
                            // with an alignment only uses the alignment when the content is
                            // smaller than the viewport, so a full list lays out as before.
                            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
                        ) {
                            val visibleArrivals =
                                if (showAllArrivals) unfilteredArrivals else arrivals.take(TOP_ARRIVAL_ROWS)
                            items(
                                visibleArrivals,
                                key = { arrivalKey(it) },
                                contentType = { "arrival" }) { arrival ->
                                val lineDetours = detoursForLine(detours, arrival.routeId)
                                val lineAlerts = alertsForLine(alerts, arrival.routeId)
                                val rowKey = arrivalKey(arrival)
                                Column {
                                    ArrivalItem(
                                        arrival = arrival,
                                        context = context,
                                        refreshKey = countdownTick,
                                        lineDetours = lineDetours,
                                        lineAlerts = lineAlerts,
                                        showClock = showClock,
                                        showRouteBadge = showRouteBadge,
                                        showVehicleInfo = showVehicleInfo,
                                        onShowAlerts = { d, a -> selectedDetours = d; selectedAlerts = a },
                                        onShowTripStatus = {
                                            selectedTripId = arrival.tripID
                                            selectedTripStatus = null
                                            isTripStatusLoading = true
                                            coroutineScope.launch {
                                                selectedTripStatus = transitRepository
                                                    .getTripStatus(tripIds = listOf(arrival.tripID))
                                                    ?.trips
                                                    ?.firstOrNull { it.tripId == arrival.tripID }
                                                isTripStatusLoading = false
                                            }
                                        },
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
                                                    dismissTapToTrackHint()
                                                }
                                            }
                                        },
                                        modifier = Modifier.animateItem()
                                    )
                                    AnimatedVisibility(
                                        visible = trackingKey == rowKey,
                                        enter = m3ContentExpand(),
                                        exit = m3ContentShrink()
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
                                        shape = appCardShape(),
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
                                                animationSpec = m3SpatialDefault()
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

    if (selectedDetours != null || selectedAlerts != null) {
        AlertsDialog(
            detours = selectedDetours.orEmpty(),
            alerts = selectedAlerts.orEmpty(),
            onDismiss = {
                selectedDetours = null
                selectedAlerts = null
            }
        )
    }

    if (selectedTripId != null) {
        TripStatusSheet(
            trip = selectedTripStatus,
            isLoading = isTripStatusLoading,
            onDismiss = {
                selectedTripId = null
                selectedTripStatus = null
            }
        )
    }

    if (showTapHintDialog && !inPip && !isLoading && arrivals.isNotEmpty() && hasValidCoords) {
        AlertDialog(
            onDismissRequest = { dismissTapToTrackHint() },
            title = { Text(stringResource(R.string.tap_to_track_title)) },
            text = { Text(stringResource(R.string.tap_to_track_message)) },
            confirmButton = {
                TextButton(onClick = { dismissTapToTrackHint() }) {
                    Text(stringResource(R.string.got_it))
                }
            }
        )
    }
}
