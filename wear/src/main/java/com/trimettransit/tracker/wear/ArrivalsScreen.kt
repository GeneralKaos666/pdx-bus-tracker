package com.trimettransit.tracker.wear

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.AmbientMode
import androidx.wear.compose.foundation.AmbientTickEffect
import androidx.wear.compose.foundation.LocalAmbientModeManager
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CardDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.tiles.TileService
import com.trimettransit.tracker.R
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.FavoritesRepositoryImpl
import com.trimettransit.tracker.data.local.RecentStopsRepositoryImpl
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.BlockPosition
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.domain.arrivalKey
import com.trimettransit.tracker.model.domain.filterArrivalsByRoute
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.transit.TransitRepositoryImpl
import com.trimettransit.tracker.util.ConnectionUtils
import com.trimettransit.tracker.util.minutesUntil
import com.trimettransit.tracker.wear.tile.FavoriteArrivalsTileService
import com.trimettransit.tracker.wear.tile.TileCache
import com.trimettransit.tracker.wear.tile.TileScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

/** Live arrivals for one stop, refreshed while the screen is visible. */
@Composable
fun ArrivalsScreen(stop: Stop) {
    val context = LocalContext.current
    val displayName = stop.desc.ifBlank { stringResource(R.string.stop_format, stop.locId) }
    val appContext = context.applicationContext
    val transitRepository = remember { TransitRepositoryImpl(appContext) }
    val favoritesRepository = remember { FavoritesRepositoryImpl(DatabaseHelper(appContext)) }
    val recentStopsRepository = remember { RecentStopsRepositoryImpl(DatabaseHelper(appContext)) }
    var result by remember { mutableStateOf<ArrivalsResult?>(null) }
    var isFavorite by remember { mutableStateOf(false) }
    var favoriteBusy by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    // Ambience drives battery-friendly behaviour: while the wear display is dimmed we
    // stop polling the API and only re-render the countdowns once per minute (tick).
    val ambientManager = LocalAmbientModeManager.current
    val isAmbient = ambientManager?.currentAmbientMode is AmbientMode.Ambient
    var ambientTick by remember { mutableIntStateOf(0) }
    if (isAmbient) {
        ambientManager.AmbientTickEffect { ambientTick++ }
    }
    // Enriched stop fetched once from the API (coords/desc), reused for the recent-stop
    // and favorite DB writes so toggling a heart never triggers a network round-trip.
    var enriched by remember { mutableStateOf(stop) }
    val scope = rememberCoroutineScope()

    // Standalone setup: load favorite state and record this stop as a recent stop
    // directly into the watch's own database (no phone involved).
    LaunchedEffect(stop.locId) {
        favoriteBusy = true
        withContext(Dispatchers.IO) {
            isFavorite = favoritesRepository.isFavorite(stop.locId)
            enriched = enrichedOrStop(stop, transitRepository)
            recentStopsRepository.addRecentStop(enriched)
        }
        favoriteBusy = false
    }

    LaunchedEffect(stop.locId, isAmbient) {
        if (isAmbient) {
            // Low-power: hold the last data, no network calls. Countdowns move with
            // the per-minute ambient tick driven above.
            while (true) {
                delay(60_000)
            }
        }
        var consecutiveFailures = 0
        while (true) {
            val fresh = withContext(Dispatchers.IO) {
                if (!ConnectionUtils.isOnline(context)) null
                else transitRepository.getArrivals(
                    listOf(stop.locId),
                    minutes = 30,
                    maxArrivals = 4
                )
            }
            if (fresh != null) {
                consecutiveFailures = 0
                failed = false
                result = fresh
                // Keep the stand-alone "next departure" Tile fresh when this stop is
                // the one it features, then nudge the system to swap in the new countdown.
                if (TileCache.updateIfFeatured(context, enriched, fresh.arrivals.orEmpty())) {
                    runCatching {
                        TileService.getUpdater(context)
                            .requestUpdate(FavoriteArrivalsTileService::class.java)
                    }
                }
                delay(30_000)
            } else {
                // Back off on failure/offline so we don't hammer the API every 30s.
                consecutiveFailures++
                if (result == null) failed = true
                delay(minOf(30_000L * consecutiveFailures, 120_000L))
            }
        }
    }

    // Boardable arrivals only — drop-off-only (non-boarding) buses are skipped so the
    // watch never counts down to a ride the user can't catch. The tile cache below still
    // gets the raw list; the Tile does its own skip when picking the next departure.
    val boardable = result?.arrivals.orEmpty().filterNot { it.dropOffOnly }
    // Optional per-route narrowing (Settings): when the user only wants to see departures
    // on the route this stop was opened from, keep just those rows. The map still gets the
    // full results so it isn't blank when the narrowed list is empty.
    val onlyShowRoute = WearPrefs.onlyShowSelectedRoute(context)
    val arrivals = if (onlyShowRoute && stop.routeNum != 0) {
        filterArrivalsByRoute(boardable, stop.routeNum)
    } else {
        boardable
    }
    val hasMap = result?.stopLat != 0.0 && result?.stopLng != 0.0

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            result == null && failed -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WearFadeInOnce {
                        Text(stringResource(R.string.unable_to_load_arrivals), textAlign = TextAlign.Center)
                    }
                }
            }
            result == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WearFadeInOnce { CircularProgressIndicator() }
                }
            }
            arrivals.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    WearFadeInOnce {
                        Text(stringResource(R.string.no_arrivals), textAlign = TextAlign.Center)
                    }
                }
            }
            else -> ArrivalList(
                displayName = displayName,
                arrivals = arrivals,
                mapArrivals = result?.arrivals.orEmpty(),
                blockPositions = result?.blockPositions.orEmpty(),
                stopLat = result?.stopLat ?: 0.0,
                stopLng = result?.stopLng ?: 0.0,
                hasMap = hasMap,
                isAmbient = isAmbient,
                ambientTick = ambientTick,
                isFavorite = isFavorite,
                favoriteEnabled = !favoriteBusy,
                onFavoriteToggle = { checked ->
                    favoriteBusy = true
                    val previous = isFavorite
                    isFavorite = checked
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching {
                                if (checked) {
                                    favoritesRepository.addFavorite(enriched)
                                } else {
                                    favoritesRepository.removeFavorite(stop.locId)
                                }
                                // Repoint the tile at the new first favorite (i.e. this
                                // stop when favoriting) and refresh its timeline.
                                if (checked) TileScheduler.refreshNow(context)
                                true
                            }.getOrDefault(false)
                        }
                        if (!ok) isFavorite = previous
                        favoriteBusy = false
                    }
                }
            )
        }
    }
}

/** Returns a full [Stop] (enriched via the API when only nav-arg fields exist). */
private suspend fun enrichedOrStop(stop: Stop, transitRepository: TransitRepository): Stop =
    if (stop.latitude == 0.0 && stop.longitude == 0.0 && stop.desc.isNotBlank()) {
        transitRepository.getStopById(stop.locId) ?: stop
    } else {
        stop
    }

@Composable
private fun ArrivalList(
    displayName: String,
    arrivals: List<Arrival>,
    mapArrivals: List<Arrival>,
    blockPositions: List<BlockPosition>,
    stopLat: Double,
    stopLng: Double,
    hasMap: Boolean,
    isAmbient: Boolean,
    ambientTick: Int,
    isFavorite: Boolean,
    favoriteEnabled: Boolean,
    onFavoriteToggle: (Boolean) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    // The departure whose bus is currently shown on the map; tapping its row again closes it.
    var trackedVehicleId by remember { mutableStateOf<Int?>(null) }

    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = { ScrollIndicator(listState) }
    ) { contentPadding ->
        WearContentEntrance(modifier = Modifier.fillMaxSize()) {
            TransformingLazyColumn(
                state = listState,
                contentPadding = contentPadding
            ) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) {
                        Text(
                            text = displayName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconToggleButton(
                            checked = isFavorite,
                            onCheckedChange = onFavoriteToggle,
                            enabled = favoriteEnabled,
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) {
                                    Icons.Default.Favorite
                                } else {
                                    Icons.Default.FavoriteBorder
                                },
                                contentDescription = stringResource(R.string.favorites),
                                tint = if (isFavorite) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                arrivals.forEach { arrival ->
                    val rowKey = arrivalKey(arrival)
                    item(key = rowKey) {
                        Column(
                            modifier = Modifier
                                .transformedHeight(this, transformationSpec)
                                .minimumVerticalContentPadding(CardDefaults.minimumVerticalListContentPadding)
                                .graphicsLayer {
                                    alpha = if (isAmbient) 0.6f else 1f
                                }
                        ) {
                            ArrivalRow(
                                arrival = arrival,
                                countdownTick = ambientTick,
                                onClick = {
                                    if (hasMap) {
                                        trackedVehicleId = if (trackedVehicleId == arrival.vehicleID) {
                                            null
                                        } else {
                                            arrival.vehicleID
                                        }
                                    }
                                }
                            )
                            AnimatedVisibility(
                                visible = hasMap && trackedVehicleId == arrival.vehicleID,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                WearStopMapCard(
                                    lat = stopLat,
                                    lng = stopLng,
                                    blockPositions = blockPositions,
                                    arrivals = mapArrivals,
                                    trackedVehicleId = arrival.vehicleID
                                )
                            }
                        }
                    }
                }
                if (hasMap) {
                    item {
                        Text(
                            text = stringResource(R.string.tap_to_track_hint),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrivalRow(
    arrival: Arrival,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    countdownTick: Int = 0
) {
    val displayTime: DateTime? =
        if (arrival.status == "estimated" && arrival.estimated != null) arrival.estimated
        else arrival.scheduled
    // Re-derived on each ambient tick so the countdown stays true while the display is dimmed.
    val minutes = remember(displayTime, countdownTick) {
        displayTime?.let { minutesUntil(it) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = arrival.shortSign.ifBlank { arrival.fullSign },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        WearCountdownText(
            minutes = minutes,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

/**
 * Countdown "N min" / "Due" label that rolls with minute changes instead of snapping.
 * The arrivals loop refreshes every ~30s; this slides in the direction of the countdown
 * (a decreasing value rolls in toward the earlier time) and fades the swap, matching the
 * phone's [AnimatedCountdownText].
 */
@Composable
private fun WearCountdownText(
    minutes: Long?,
    style: androidx.compose.ui.text.TextStyle
) {
    val target = minutes
    AnimatedContent(
        targetState = target,
        modifier = Modifier,
        transitionSpec = {
            val t = targetState
            val i = initialState
            val decreasing = t != null && i != null && t < i
            val enter = (if (decreasing) {
                slideInHorizontally(m3SpatialDefault()) { it / 3 }
            } else {
                slideInHorizontally(m3SpatialDefault()) { -it / 3 }
            }) + fadeIn(m3EffectsDefault())
            val exit = (if (decreasing) {
                slideOutHorizontally(m3SpatialFast()) { -it / 3 }
            } else {
                slideOutHorizontally(m3SpatialFast()) { it / 3 }
            }) + fadeOut(m3EffectsFast())
            enter togetherWith exit
        },
        label = "wearCountdownRoll"
    ) { mins ->
        Text(
            text = when {
                mins == null -> stringResource(R.string.no_time_info)
                mins <= 0L -> stringResource(R.string.due)
                else -> stringResource(R.string.countdown_min, mins)
            },
            style = style
        )
    }
}