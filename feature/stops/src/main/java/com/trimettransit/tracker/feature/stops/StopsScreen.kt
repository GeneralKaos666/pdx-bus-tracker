package com.trimettransit.tracker.feature.stops

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.transit.ApiKeys
import com.trimettransit.tracker.ui.components.InlineSkeleton
import com.trimettransit.tracker.ui.components.StopListItem
import com.trimettransit.tracker.ui.components.pressScale

/**
 * Routes list with an accordion drill-down, mirroring the arrivals map card:
 * tapping a route expands its directions inline under the row (expandVertically +
 * fadeIn); tapping a direction expands its stops under it. Tap again to collapse.
 */
@Composable
fun StopsScreen(
    transitRepository: TransitRepository,
    selectedRoute: Route?,
    selectedDirection: Direction?,
    onRouteToggle: (Route) -> Unit,
    onDirectionToggle: (Direction) -> Unit,
    onNavigateToArrivals: (Stop, routeId: Int) -> Unit
) {
    StopsRouteList(
        transitRepository = transitRepository,
        selectedRoute = selectedRoute,
        onRouteToggle = onRouteToggle,
        routeTrailingContent = { route ->
            AnimatedVisibility(
                visible = selectedRoute?.routeId == route.routeId,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                DirectionsSubCard(
                    route = route,
                    transitRepository = transitRepository,
                    selectedDirection = selectedDirection,
                    onDirectionToggle = onDirectionToggle,
                    directionTrailingContent = { direction ->
                        AnimatedVisibility(
                            visible = selectedDirection?.dir == direction.dir,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            StopsSubCard(
                                routeId = route.routeId,
                                directionId = direction.dir,
                                transitRepository = transitRepository,
                                onStopSelected = { stop -> onNavigateToArrivals(stop, route.routeId) }
                            )
                        }
                    }
                )
            }
        }
    )
}

@Composable
private fun DirectionsSubCard(
    route: Route,
    transitRepository: TransitRepository,
    selectedDirection: Direction?,
    onDirectionToggle: (Direction) -> Unit,
    directionTrailingContent: @Composable (Direction) -> Unit
) {
    var directions by remember { mutableStateOf<List<Direction>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMissingApiKey by remember { mutableStateOf(false) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(route.routeId, retryKey) {
        isLoading = true
        isMissingApiKey = false
        if (ApiKeys.getTrimetApiKey().isBlank()) {
            isMissingApiKey = true
            directions = null
        } else {
            directions = transitRepository.getDirections(route.routeId)
        }
        isLoading = false
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)) {
            val safeDirections = directions
            Crossfade(
                targetState = when {
                    isLoading -> 0
                    isMissingApiKey -> 1
                    safeDirections == null -> 2
                    safeDirections.isEmpty() -> 3
                    else -> 4
                },
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "directionsSubCardState"
            ) { state ->
                when (state) {
                    0 -> InlineSkeleton(rows = 2)
                    1 -> InlineMessage("API key not configured.\nPlease check app settings.")
                    2 -> InlineRetry(
                        message = "Unable to load directions.\nCheck your connection.",
                        onRetry = { retryKey++ }
                    )
                    3 -> InlineMessage("No directions available.")
                    else -> Column {
                        safeDirections.orEmpty().forEach { direction ->
                            DirectionItem(
                                direction = direction,
                                isExpanded = selectedDirection?.dir == direction.dir,
                                onClick = { onDirectionToggle(direction) }
                            )
                            directionTrailingContent(direction)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DirectionItem(
    direction: Direction,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .pressScale(interactionSource),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = direction.desc,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            val chevronRotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.rotate(chevronRotation)
            )
        }
    }
}

@Composable
private fun StopsSubCard(
    routeId: Int,
    directionId: Int,
    transitRepository: TransitRepository,
    onStopSelected: (Stop) -> Unit
) {
    var stops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMissingApiKey by remember { mutableStateOf(false) }
    var retryKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(routeId, directionId, retryKey) {
        isLoading = true
        isMissingApiKey = false
        if (ApiKeys.getTrimetApiKey().isBlank()) {
            isMissingApiKey = true
            stops = null
        } else {
            stops = transitRepository.getStops(routeId, directionId)
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        val safeStops = stops
        Crossfade(
            targetState = when {
                isLoading -> 0
                isMissingApiKey -> 1
                safeStops == null -> 2
                safeStops.isEmpty() -> 3
                else -> 4
            },
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "stopsSubCardState"
        ) { state ->
            when (state) {
                0 -> InlineSkeleton(rows = 2)
                1 -> InlineMessage("API key not configured.\nPlease check app settings.")
                2 -> InlineRetry(
                    message = "Unable to load stops.\nCheck your connection.",
                    onRetry = { retryKey++ }
                )
                3 -> InlineMessage("No stops available.")
                else -> Column {
                    safeStops.orEmpty().forEach { stop ->
                        StopListItem(
                            stop = stop,
                            onClick = { onStopSelected(stop) },
                            zoomOnTap = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    )
}

@Composable
private fun InlineRetry(message: String, onRetry: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        val retrySource = remember { MutableInteractionSource() }
        OutlinedButton(
            onClick = onRetry,
            interactionSource = retrySource,
            modifier = Modifier
                .padding(top = 8.dp)
                .pressScale(retrySource)
        ) {
            Text("Try Again")
        }
    }
}