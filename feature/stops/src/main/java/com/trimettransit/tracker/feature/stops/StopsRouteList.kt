package com.trimettransit.tracker.feature.stops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.transit.TransitApi
import com.trimettransit.tracker.ui.components.LoadingState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.EmptyState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.transit.ApiKeys
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior

@Composable
fun StopsRouteList(
    onRouteSelected: (Route) -> Unit
) {
    val context = LocalContext.current
    var routes by remember { mutableStateOf<List<Route>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMissingApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val key = ApiKeys.getTrimetApiKey()
        if (key.isBlank()) {
            isMissingApiKey = true
            routes = null
        } else {
            val url = context.getString(R.string.base_route_url) +
                    "/appID/$key"
            routes = TransitApi.fetchRoutes(context, url)
        }
        isLoading = false
    }

    val safeRoutes = routes
    Crossfade(
        targetState = when {
            isLoading -> 0
            safeRoutes == null -> 1
            safeRoutes.isEmpty() -> 2
            else -> 3
        },
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "routesState"
    ) { state ->
        when (state) {
            0 -> {
                LoadingState()
            }
            1 -> {
                ErrorState(
                    message = if (isMissingApiKey) "API key not configured.\nPlease check app settings."
                              else "Unable to load routes.\nCheck your connection."
                )
            }
            2 -> {
                EmptyState(message = "No routes available.")
            }
            else -> {
                ContentEntrance(modifier = Modifier.fillMaxSize()) {
                val smoothFling = rememberSmoothFlingBehavior()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    flingBehavior = smoothFling,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(safeRoutes ?: emptyList(), key = { it.routeId }, contentType = { "route" }) { route ->
                        RouteListItem(
                            route = route,
                            onClick = { onRouteSelected(route) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                }
            }
        }
    }
}

@Composable
private fun RouteListItem(
    route: Route,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .pressScale(interactionSource),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colorScheme = MaterialTheme.colorScheme
            val typeColor = remember(route.typeLetter, colorScheme) {
                transitColor(route.typeLetter, colorScheme)
            }
            Surface(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                color = typeColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = route.routeId.toString(),
                        color = MaterialTheme.colorScheme.surface,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = route.desc ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = routeTypeLabel(route.typeLetter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

private fun routeTypeLabel(type: String?): String = when (type) {
    "B" -> "Bus"
    "R", "M" -> "MAX Light Rail"
    "T" -> "Streetcar"
    "W" -> "WES Commuter Rail"
    else -> "Transit"
}
