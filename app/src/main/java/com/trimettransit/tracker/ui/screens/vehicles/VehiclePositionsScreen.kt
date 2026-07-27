package com.trimettransit.tracker.ui.screens.vehicles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.data.model.VehiclePosition
import com.trimettransit.tracker.ui.TransitApi
import com.trimettransit.tracker.ui.screens.components.EmptyState
import com.trimettransit.tracker.ui.screens.components.ErrorState
import com.trimettransit.tracker.ui.screens.components.LoadingState
import com.trimettransit.tracker.ui.screens.components.transitColor
import com.trimettransit.tracker.ui.screens.components.rememberOnResume

import kotlinx.coroutines.launch

@Composable
fun VehiclePositionsScreen(
    onNavigateToArrivals: (Stop, Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var routeInput by remember { mutableStateOf("") }
    var vehicles by remember { mutableStateOf<List<VehiclePosition>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasLoaded by remember { mutableStateOf(false) }

    fun loadVehicles() {
        isLoading = true
        errorMessage = null
        vehicles = null
        coroutineScope.launch {
            val routeIds = routeInput.split(",").mapNotNull { it.trim().toIntOrNull() }
            val result = TransitApi.fetchVehicles(
                context = context,
                routes = routeIds.ifEmpty { null },
                onRouteOnly = true,
                showStale = false
            )
            vehicles = result
            isLoading = false
            hasLoaded = true
            if (result != null && result.isEmpty()) {
                errorMessage = "No vehicles found for this route"
            }
        }
    }

    // Re-fetch on app re-entry
    rememberOnResume {
        if (hasLoaded) {
            loadVehicles()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Vehicle Positions",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Route input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = routeInput,
                onValueChange = { routeInput = it },
                label = { Text("Route number(s)") },
                placeholder = { Text("e.g. 90 or 20,90") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(
                onClick = { loadVehicles() },
                enabled = !isLoading
            ) {
                Text("Load")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Content
        when {
            isLoading -> {
                LoadingState(message = "Loading vehicles...")
            }
            errorMessage != null && vehicles == null -> {
                ErrorState(message = errorMessage ?: "Unknown error")
            }
            vehicles != null -> {
                val safeVehicles = vehicles!!
                if (safeVehicles.isEmpty()) {
                    EmptyState(
                        message = if (hasLoaded) "No vehicles found for this route"
                                  else "Enter a route and tap Load"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(safeVehicles, key = { it.vehicleID }) { vehicle ->
                            VehicleListItem(vehicle = vehicle)
                        }
                    }
                }
            }
            !hasLoaded -> {
                EmptyState(message = "Enter a route number and tap Load")
            }
        }
    }
}

@Composable
private fun VehicleListItem(vehicle: VehiclePosition) {
    val delaySeconds = vehicle.delay
    val colorScheme = MaterialTheme.colorScheme
    val delayColor = remember(delaySeconds, colorScheme) {
        when {
            delaySeconds > 300 -> colorScheme.error
            delaySeconds > 60 -> colorScheme.tertiary
            delaySeconds < -300 -> colorScheme.error
            delaySeconds < -60 -> colorScheme.tertiary
            else -> colorScheme.primary
        }
    }

    val delayText = remember(delaySeconds) {
        when {
            delaySeconds > 0 -> "${delaySeconds / 60} min early"
            delaySeconds < 0 -> "${(-delaySeconds) / 60} min late"
            else -> "On time"
        }
    }

    val bearingText = remember(vehicle.bearing) {
        when {
            vehicle.bearing in 337.5f..360f || vehicle.bearing in 0f..22.5f -> "N"
            vehicle.bearing in 22.5f..67.5f -> "NE"
            vehicle.bearing in 67.5f..112.5f -> "E"
            vehicle.bearing in 112.5f..157.5f -> "SE"
            vehicle.bearing in 157.5f..202.5f -> "S"
            vehicle.bearing in 202.5f..247.5f -> "SW"
            vehicle.bearing in 247.5f..292.5f -> "W"
            vehicle.bearing in 292.5f..337.5f -> "NW"
            else -> "--"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header: Route + Vehicle ID
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val vehicleColor = remember(vehicle.type, colorScheme) {
                    transitColor(vehicle.type, colorScheme)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = vehicleColor
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = vehicle.type,
                                color = MaterialTheme.colorScheme.surface,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Route ${vehicle.routeNumber}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Vehicle #${vehicle.vehicleID}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Delay indicator
                Surface(
                    shape = CircleShape,
                    color = delayColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = delayText,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = delayColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Direction + bearing
                Column {
                    Text(
                        text = "Direction",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Dir ${vehicle.direction} ($bearingText)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Next stop
                Column {
                    Text(
                        text = "Next Stop",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (vehicle.nextLocID > 0) "Stop #${vehicle.nextLocID}" else "Unknown",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Last stop
                Column {
                    Text(
                        text = "Last Stop",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (vehicle.lastLocID > 0) "Stop #${vehicle.lastLocID}" else "Unknown",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Sign message
            if (!vehicle.signMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = vehicle.signMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
