package com.trimettransit.tracker.feature.stops

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.transit.ApiKeys
import com.trimettransit.tracker.transit.TransitApi
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitTypeLabel

@Composable
fun StopsRouteList(
    selectedRoute: Route?,
    onRouteToggle: (Route) -> Unit,
    routeTrailingContent: @Composable LazyItemScope.(Route) -> Unit
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
            val url = context.getString(com.trimettransit.tracker.transit.R.string.base_route_url) +
                    "/appID/$key"
            routes = TransitApi.fetchRoutes(context, url)
        }
        isLoading = false
    }

    val safeRoutes = routes
    StopListContent(
        isLoading = isLoading,
        items = safeRoutes,
        errorMessage = if (isMissingApiKey) "API key not configured.\nPlease check app settings."
                       else "Unable to load routes.\nCheck your connection.",
        emptyMessage = "No routes available.",
        stateLabel = "routesState",
        key = { it.routeId },
        contentType = { "route" },
        itemContent = { route ->
            RouteListItem(
                route = route,
                isExpanded = selectedRoute?.routeId == route.routeId,
                onClick = { onRouteToggle(route) },
                modifier = Modifier.animateItem()
            )
        },
        itemTrailingContent = { route ->
            routeTrailingContent(route)
        }
    )
}

@Composable
private fun RouteListItem(
    route: Route,
    isExpanded: Boolean,
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
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
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
                    text = route.desc,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = transitTypeLabel(route.typeLetter),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            val chevronRotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .rotate(chevronRotation)
            )
        }
    }
}