package com.trimettransit.tracker.wear

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.IconToggleButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.tiles.TileService
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.transit.TransitApi
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
    val displayName = stop.desc.ifBlank { "Stop ${stop.locId}" }
    var result by remember { mutableStateOf<ArrivalsResult?>(null) }
    var isFavorite by remember { mutableStateOf(false) }
    var favoriteBusy by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // Standalone setup: load favorite state and record this stop as a recent stop
    // directly into the watch's own database (no phone involved).
    LaunchedEffect(stop.locId) {
        favoriteBusy = true
        withContext(Dispatchers.IO) {
            val db = DatabaseHelper(context.applicationContext)
            isFavorite = db.isFavorite(stop.locId)
            db.addRecentStop(enrichedOrStop(stop, context))
        }
        favoriteBusy = false
    }

    LaunchedEffect(stop.locId) {
        while (true) {
            val fresh = withContext(Dispatchers.IO) {
                TransitApi.fetchArrivals(
                    context,
                    listOf(stop.locId),
                    minutes = 30,
                    maxArrivals = 4
                )
            }
            if (fresh != null) {
                result = fresh
                // Keep the stand-alone "next departure" Tile fresh when this stop is
                // the one it features, then nudge the system to swap in the new countdown.
                if (TileCache.updateIfFeatured(context, stop, fresh.arrivals.orEmpty())) {
                    runCatching {
                        TileService.getUpdater(context)
                            .requestUpdate(FavoriteArrivalsTileService::class.java)
                    }
                }
            }
            delay(30_000)
        }
    }

    val arrivals = result?.arrivals.orEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
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
                        Text("No arrivals right now", textAlign = TextAlign.Center)
                    }
                }
            }
            else -> ArrivalList(
                displayName = displayName,
                arrivals = arrivals,
                isFavorite = isFavorite,
                favoriteEnabled = !favoriteBusy,
                onFavoriteToggle = { checked ->
                    favoriteBusy = true
                    val previous = isFavorite
                    isFavorite = checked
                    scope.launch {
                        val ok = withContext(Dispatchers.IO) {
                            runCatching {
                                val db = DatabaseHelper(context.applicationContext)
                                val embedded = enrichedOrStop(stop, context)
                                if (checked) db.addFavorite(embedded) else db.removeFavorite(stop.locId)
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
private suspend fun enrichedOrStop(stop: Stop, context: android.content.Context): Stop =
    if (stop.latitude == 0.0 && stop.longitude == 0.0 && stop.desc.isNotBlank()) {
        TransitApi.fetchStopById(context, stop.locId) ?: stop
    } else {
        stop
    }

@Composable
private fun ArrivalList(
    displayName: String,
    arrivals: List<Arrival>,
    isFavorite: Boolean,
    favoriteEnabled: Boolean,
    onFavoriteToggle: (Boolean) -> Unit
) {
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        WearContentEntrance(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
            TransformingLazyColumn(
                state = listState
            ) {
                item {
                    ListHeader {
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
                            Text(
                                text = "♥",
                                color = if (isFavorite) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
                itemsIndexed(arrivals) { _, arrival ->
                    ArrivalRow(arrival)
                }
            }
        }
    }
}

@Composable
private fun ArrivalRow(arrival: Arrival) {
    val displayTime: DateTime? =
        if (arrival.status == "estimated" && arrival.estimated != null) arrival.estimated
        else arrival.scheduled
    val minutes = displayTime?.let { minutesUntil(it) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Text(
            text = arrival.shortSign.ifBlank { arrival.fullSign },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = when {
                minutes == null -> "No time info"
                minutes <= 0L -> "Due"
                else -> "$minutes min"
            },
            style = MaterialTheme.typography.labelSmall
        )
    }
}