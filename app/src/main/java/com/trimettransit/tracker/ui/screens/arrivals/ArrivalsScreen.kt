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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.R
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.model.Arrival
import com.trimettransit.tracker.data.model.Detour
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.ui.TransitApi
import com.trimettransit.tracker.ui.screens.components.EmptyState
import com.trimettransit.tracker.ui.screens.components.ErrorState
import com.trimettransit.tracker.ui.screens.components.LoadingState
import com.trimettransit.tracker.ui.screens.components.transitColor
import com.trimettransit.tracker.ui.screens.components.transitInitial
import com.trimettransit.tracker.util.DateUtils
import kotlinx.coroutines.launch

private const val TAG = "ArrivalsScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArrivalsScreen(
    stopId: String,
    stopName: String,
    routeId: Int,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var arrivals by remember { mutableStateOf<List<Arrival>>(emptyList()) }
    var detours by remember { mutableStateOf<List<Detour>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var alertsExpanded by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val locId = stopId.toIntOrNull() ?: 0

    LaunchedEffect(stopId) {
        if (locId > 0) {
            isFavorite = DatabaseHelper(context.applicationContext).isFavorite(locId)
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

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stopName.ifBlank { "Stop #$stopId" }) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val msg = toggleFavorite(context, locId, stopName, isFavorite)
                            isFavorite = !isFavorite
                            snackbarHostState.showSnackbar(msg)
                        }
                    }) {
                        Icon(
                            if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFavorite) "Remove favorite" else "Add favorite",
                            tint = if (isFavorite) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    IconButton(onClick = { loadArrivals() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isLoading,
            onRefresh = { loadArrivals() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        )
        {
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
}

@Composable
private fun ArrivalItem(arrival: Arrival, context: Context) {
    val type = when {
        arrival.routeId == 200 -> "M"
        arrival.routeId == 100 || arrival.routeId == 90 -> "R"
        arrival.routeId in 1..99 -> "B"
        else -> ""
    }
    val color = transitColor(type)
    val initial = transitInitial(type)

    val displayTime = if (arrival.status == "estimated" && arrival.estimated != null) {
        arrival.estimated
    } else {
        arrival.scheduled
    }

    val formattedTime = if (displayTime != null) DateUtils.a(displayTime, context) else ""
    val minutesAway = if (displayTime != null) DateUtils.b(displayTime) else 0L
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

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = relativeText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isEstimated) FontWeight.Bold else FontWeight.Normal,
                color = if (isEstimated) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface
            )
            if (!isEstimated) {
                Text(
                    text = "scheduled",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun toggleFavorite(context: Context, locId: Int, stopName: String, currentlyFavorite: Boolean): String {
    return try {
        val db = DatabaseHelper(context.applicationContext)
        if (currentlyFavorite) {
            val writableDb = db.writableDatabase
            writableDb.delete("favorites", "loc_id = ?", arrayOf(locId.toString()))
            writableDb.close()
            context.getString(R.string.favorite_deleted_text)
        } else {
            val stop = Stop()
            stop.setDesc(stopName)
            stop.setLocId(locId)
            db.addFavorite(stop, null)
            context.getString(R.string.favorite_added_text)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to toggle favorite", e)
        "Failed to update favorite"
    }
}
