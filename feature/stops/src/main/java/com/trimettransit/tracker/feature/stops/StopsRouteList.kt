package com.trimettransit.tracker.feature.stops

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberDenseGridEnabled
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.appearance.rowContentPadding
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitTypeLabel
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.appCardBorder
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.m3SpatialDefault

@Composable
fun StopsRouteList(
    transitRepository: TransitRepository,
    selectedRoute: Route?,
    pageVisible: Boolean,
    onRouteToggle: (Route) -> Unit,
    routeTrailingContent: @Composable (Route) -> Unit
) {
    var routes by remember { mutableStateOf<List<Route>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMissingApiKey by remember { mutableStateOf(false) }
    var isOffline by remember { mutableStateOf(false) }
    // Bumped by the retry button to re-run the fetch (LaunchedEffect key).
    var retryKey by remember { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Loads on first composition, on an explicit retry, and when this tab becomes visible *with
    // nothing loaded* — which is how a user who reconnected recovers, since there is no Try Again
    // button while offline. A healthy list is never refetched.
    LaunchedEffect(retryKey, pageVisible, routes == null) {
        if (!pageVisible) return@LaunchedEffect
        if (routes != null) return@LaunchedEffect
        isLoading = true
        isMissingApiKey = false
        if (!transitRepository.isConfigured()) {
            isMissingApiKey = true
            isOffline = false
            routes = null
        } else {
            val fetched = transitRepository.getRoutes()
            isOffline = isOfflineFailure(fetched, context)
            routes = fetched
        }
        isLoading = false
    }

    // There is no retry button while offline, so recover as soon as the connection comes back.
    OnNetworkRegained { if (routes == null) retryKey++ }

    val gridMode = rememberDenseGridEnabled()
    val safeRoutes = routes
    val listState = rememberLazyGridState()

    // Bring a newly expanded route into view. Without this, tapping a route near the bottom of a
    // long list opens its directions below the fold with nothing on screen to show they appeared.
    // One grid item per route, so the route's list index is its item index.
    val expandedIndex = selectedRoute?.let { selected ->
        safeRoutes?.indexOfFirst { it.routeId == selected.routeId }?.takeIf { it >= 0 }
    }
    LaunchedEffect(expandedIndex) {
        if (expandedIndex != null) listState.animateScrollToItem(expandedIndex)
    }

    StopListContent(
        isLoading = isLoading,
        items = safeRoutes,
        errorMessage = when {
            isMissingApiKey -> stringResource(R.string.api_key_not_configured)
            isOffline -> stringResource(R.string.offline_no_data)
            else -> stringResource(R.string.unable_to_load_routes)
        },
        emptyMessage = stringResource(R.string.no_routes_available),
        stateLabel = "routesState",
        gridMode = gridMode,
        key = { it.routeId },
        contentType = { "route" },
        // Neither an offline device nor a missing API key can be fixed by tapping again.
        onRetry = if (isOffline || isMissingApiKey) null else ({ retryKey++ }),
        listState = listState,
        itemContent = { route ->
            RouteListItem(
                route = route,
                isExpanded = selectedRoute?.routeId == route.routeId,
                onClick = { onRouteToggle(route) },
                modifier = if (AppMotion.reduceMotion) Modifier else Modifier.animateItem(),
                gridMode = gridMode
            )
        },
        itemTrailingContent = routeTrailingContent
    )
}

@Composable
private fun RouteListItem(
    route: Route,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gridMode: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    // This row toggles a section, so it reports both its role and the section's state. Card's
    // clickable does NOT supply a button role, so it is set here explicitly. The chevron below is
    // decorative for the same reason: otherwise TalkBack announces it a second time.
    val expandedLabel = stringResource(R.string.expanded)
    val collapsedLabel = stringResource(R.string.collapsed)
    Card(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .then(if (gridMode) Modifier else Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            .semantics {
                role = Role.Button
                stateDescription = if (isExpanded) expandedLabel else collapsedLabel
            }
            .pressScale(interactionSource),
        shape = appCardShape(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        border = appCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = rowContentPadding(comfortable = 12.dp, compact = 6.dp)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val colorScheme = MaterialTheme.colorScheme
            val typeColor = transitColor(
                route.typeLetter,
                colorScheme,
                LocalAppearanceStyle.current.transitTypeColors
            )
            Surface(
                modifier = Modifier.sizeIn(minWidth = 44.dp, minHeight = 44.dp),
                shape = appCardShape(),
                color = typeColor
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
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
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(transitTypeLabel(route.typeLetter)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            val chevronRotation by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = m3SpatialDefault()
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .rotate(chevronRotation)
            )
        }
    }
}
