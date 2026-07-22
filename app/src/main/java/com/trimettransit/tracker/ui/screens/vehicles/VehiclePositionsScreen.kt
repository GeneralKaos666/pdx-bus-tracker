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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.data.model.VehiclePosition
import com.trimettransit.tracker.ui.TransitApi
import com.trimettransit.tracker.ui.screens.components.transitColor
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Loading vehicles...")
                    }
                }
            }
            errorMessage != null && vehicles == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = errorMessage ?: "Unknown error",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            vehicles != null -> {
                if (vehicles!!.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (hasLoaded) "No vehicles found for this route" else "Enter a route and tap Load",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vehicles!!) { vehicle ->
                            VehicleListItem(vehicle = vehicle)
                        }
                    }
                }
            }
            !hasLoaded -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Enter a route number and tap Load",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun VehicleListItem(vehicle: VehiclePosition) {
    val delaySeconds = vehicle.delay
    val delayColor = when {
        delaySeconds > 300 -> Color(0xFFD32F2F)  // Red: > 5 min
        delaySeconds > 60 -> Color(0xFFFBC02D)   // Yellow: > 1 min
        delaySeconds < -300 -> Color(0xFFD32F2F)  // Red: > 5 min late
        delaySeconds < -60 -> Color(0xFFFBC02D)   // Yellow: > 1 min late
        else -> Color(0xFF388E3C)                 // Green: on time
    }

    val delayText = when {
        delaySeconds > 0 -> "${delaySeconds / 60} min early"
        delaySeconds < 0 -> "${(-delaySeconds) / 60} min late"
        else -> "On time"
    }

    val bearingText = when {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = CircleShape,
                        color = transitColor(vehicle.type)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = vehicle.type,
                                color = Color.White,
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
