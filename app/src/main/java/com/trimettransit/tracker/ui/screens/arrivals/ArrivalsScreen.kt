package com.trimettransit.tracker.ui.screens.arrivals

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.R
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.model.Arrival
import com.trimettransit.tracker.data.model.Detour
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.ui.NavState
import com.trimettransit.tracker.ui.TransitApi
import com.trimettransit.tracker.ui.screens.components.EmptyState
import com.trimettransit.tracker.ui.screens.components.ErrorState
import com.trimettransit.tracker.ui.screens.components.LoadingState
import com.trimettransit.tracker.util.formatDateTime
import com.trimettransit.tracker.util.minutesUntil
import com.trimettransit.tracker.ui.screens.components.transitColor
import com.trimettransit.tracker.ui.screens.components.transitInitial
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.viewinterop.AndroidView
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

private const val TAG = "ArrivalsScreen"

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
    var detours by remember { mutableStateOf<List<Detour>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var alertsExpanded by remember { mutableStateOf(false) }
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
        }
    }

    fun loadArrivals() {
        coroutineScope.launch {
            isLoading = true
            val result = TransitApi.fetchArrivals(
                context = context,
                locIds = listOf(locId),
                showPosition = false,
                minutes = 30,
                maxArrivals = 4
            )
            if (result != null && !result.isQueryError) {
                val allArrivals = result.arrivals?.toList() ?: emptyList()
                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                val onlySelectedRoute = prefs.getBoolean("pref_key_only_show_route_selected", true)
                arrivals = if (onlySelectedRoute && routeId > 0) {
                    allArrivals.filter { it.routeId == routeId }
                } else {
                    allArrivals
                }
                detours = result.detours?.toList() ?: emptyList()
                isError = false
            } else {
                arrivals = emptyList()
                isError = true
            }
            isLoading = false
        }
    }

    LaunchedEffect(stopId) {
        loadArrivals()
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    if (hasValidCoords) {
                        item(key = "map") {
                            StopMapCard(
                                lat = stopLat,
                                lng = stopLng,
                                stopName = stopName
                            )
                        }
                    }
                    if (detours.isNotEmpty()) {
                        item {
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Column {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { alertsExpanded = !alertsExpanded }
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Alerts (${detours.size})",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            imageVector = if (alertsExpanded) Icons.Default.KeyboardArrowUp
                                                else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (alertsExpanded) "Collapse alerts"
                                                else "Expand alerts",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    AnimatedVisibility(visible = alertsExpanded) {
                                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                            detours.forEach { detour ->
                                                Text(
                                                    text = detour.desc ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(arrivals, key = { "${it.tripID}_${it.routeId}_${it.scheduledMillis}" }) { arrival ->
                        ArrivalItem(arrival = arrival, context = context)
                        HorizontalDivider()
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

@Composable
private fun StopMapCard(
    lat: Double,
    lng: Double,
    stopName: String,
    modifier: Modifier = Modifier
) {
    var mapView by remember { mutableStateOf<MapView?>(null) }

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
                    text = stopName.ifBlank { "Stop Location" },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            AndroidView(
                factory = { ctx ->
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        isTilesScaledToDpi = true
                        controller.setZoom(16.0)
                        controller.setCenter(GeoPoint(lat, lng))
                        val marker = Marker(this).apply {
                            position = GeoPoint(lat, lng)
                            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            title = stopName
                        }
                        overlays.add(marker)
                        mapView = this
                    }
                },
                update = { view ->
                    view.onResume()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mapView?.onPause()
            mapView?.onDetach()
        }
    }
}

@Composable
private fun ArrivalItem(arrival: Arrival, context: Context) {
    val type = when {
        arrival.routeId == 200 -> "M"
        arrival.routeId == 100 || arrival.routeId == 90 -> "R"
        arrival.routeId in 1..99 -> "B"
        else -> ""
    }
    val color = transitColor(type, MaterialTheme.colorScheme)
    val initial = transitInitial(type)

    val displayTime = if (arrival.status == "estimated" && arrival.estimated != null) {
        arrival.estimated
    } else {
        arrival.scheduled
    }

    val formattedTime = if (displayTime != null) formatDateTime(displayTime, context) else ""
    val minutesAway = if (displayTime != null) minutesUntil(displayTime) else 0L
    val relativeText = if (minutesAway <= 0) "Due" else "${minutesAway} min"
    val isEstimated = arrival.status == "estimated"

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
                Text(
                    text = initial,
                    color = MaterialTheme.colorScheme.surface,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
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

internal fun toggleFavorite(context: Context, locId: Int, stopName: String, currentlyFavorite: Boolean): String {
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
            db.addFavorite(stop, null)
            context.getString(R.string.favorite_added_text)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to toggle favorite", e)
        "Failed to update favorite"
    }
}
