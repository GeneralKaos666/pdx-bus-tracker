package com.trimettransit.tracker.wear

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.transit.ApiKeys
import com.trimettransit.tracker.model.repository.TransitRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Which level of the Routes drill-down a [RoutesScreen] instance renders. */
enum class RoutesLevel {
    ROUTES, DIRECTIONS, STOPS
}

/**
 * Watch-flavored Routes browser, mirroring the phone's route → direction → stop
 * drill-down. Each level fetches its data live from the TriMet API (the same
 * [TransitApi] calls the phone uses) rather than from the phone-synced local DB.
 */
@Composable
fun RoutesScreen(
    transitRepository: TransitRepository,
    level: RoutesLevel,
    routeId: Int = 0,
    routeName: String = "",
    dirId: Int = 0,
    dirName: String = "",
    onStopClick: (Stop) -> Unit = {},
    onRouteClick: (Route) -> Unit = {},
    onDirectionClick: (Direction) -> Unit = {}
) {
    val apiKey = remember { ApiKeys.getTrimetApiKey() }

    var retryKey by remember { mutableIntStateOf(0) }

    val state = rememberRoutesState(transitRepository, apiKey, level, routeId, dirId, retryKey)

    val header = when (level) {
        RoutesLevel.ROUTES -> "Routes"
        RoutesLevel.DIRECTIONS -> routeName
        RoutesLevel.STOPS -> dirName
    }

    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        val t = state
        when {
            t.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    WearFadeInOnce { CircularProgressIndicator() }
                }
            }
            t.isMissingApiKey -> {
                CenteredMessageWithRetry(
                    message = "API key not configured.\nPlease check app settings.",
                    contentPadding = contentPadding,
                    onRetry = { retryKey++ }
                )
            }
            t.failed -> {
                CenteredMessageWithRetry(
                    message = when (level) {
                        RoutesLevel.ROUTES -> "Unable to load routes.\nCheck your connection."
                        RoutesLevel.DIRECTIONS -> "Unable to load directions.\nCheck your connection."
                        RoutesLevel.STOPS -> "Unable to load stops.\nCheck your connection."
                    },
                    contentPadding = contentPadding,
                    onRetry = { retryKey++ }
                )
            }
            t.items.isEmpty() -> {
                CenteredMessageWithRetry(
                    message = when (level) {
                        RoutesLevel.ROUTES -> "No routes available."
                        RoutesLevel.DIRECTIONS -> "No directions available."
                        RoutesLevel.STOPS -> "No stops available."
                    },
                    contentPadding = contentPadding,
                    onRetry = { retryKey++ }
                )
            }
            else -> {
                WearContentEntrance(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                    TransformingLazyColumn(state = listState) {
                        item {
                            ListHeader {
                                Text(header, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        when (level) {
                            RoutesLevel.ROUTES ->
                                items(t.items as List<Route>, key = { it.routeId }) { route ->
                                    RouteRow(route, onClick = { onRouteClick(route) })
                                }
                            RoutesLevel.DIRECTIONS ->
                                items(t.items as List<Direction>, key = { it.dir }) { dir ->
                                    DirectionRow(dir, onClick = { onDirectionClick(dir) })
                                }
                            RoutesLevel.STOPS ->
                                items(t.items as List<Stop>, key = { it.locId }) { stop ->
                                    StopRow(stop, onClick = { onStopClick(stop) })
                                }
                        }
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun rememberRoutesState(
    transitRepository: TransitRepository,
    apiKey: String,
    level: RoutesLevel,
    routeId: Int,
    dirId: Int,
    retryKey: Int
): RoutesListState {
    var items by remember { mutableStateOf<List<Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMissingApiKey by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }

    LaunchedEffect(level, routeId, dirId, retryKey) {
        isLoading = true
        isMissingApiKey = false
        failed = false
        if (apiKey.isBlank()) {
            isMissingApiKey = true
        } else {
            val result = withContext(Dispatchers.IO) {
                when (level) {
                    RoutesLevel.ROUTES -> transitRepository.getRoutes() as List<Any>?
                    RoutesLevel.DIRECTIONS -> transitRepository.getDirections(routeId) as List<Any>?
                    RoutesLevel.STOPS -> transitRepository.getStops(routeId, dirId) as List<Any>?
                }
            }
            if (result != null) items = result else failed = true
        }
        isLoading = false
    }

    return RoutesListState(
        items = items ?: emptyList(),
        isLoading = isLoading,
        isMissingApiKey = isMissingApiKey,
        failed = failed
    )
}

private class RoutesListState(
    val items: List<Any>,
    val isLoading: Boolean,
    val isMissingApiKey: Boolean,
    val failed: Boolean
)

@Composable
private fun CenteredMessageWithRetry(
    message: String,
    contentPadding: PaddingValues,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        WearFadeInOnce {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = message,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                val retrySource = remember { MutableInteractionSource() }
                Button(
                    onClick = onRetry,
                    interactionSource = retrySource,
                    modifier = Modifier.wearPressScale(retrySource),
                    label = { Text("Retry") }
                )
            }
        }
    }
}

@Composable
private fun RouteRow(route: Route, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().wearPressScale(interactionSource),
        label = {
            Text(
                text = "${route.typeLetter} ${route.routeId} · ${route.desc}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        }
    )
}

@Composable
private fun DirectionRow(direction: Direction, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().wearPressScale(interactionSource),
        label = {
            Text(
                text = direction.desc,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        }
    )
}

@Composable
private fun StopRow(stop: Stop, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().wearPressScale(interactionSource),
        label = {
            Text(
                text = stop.desc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        },
        secondaryLabel = {
            if (stop.dirDesc.isNotBlank()) {
                Text(
                    text = stop.dirDesc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
        }
    )
}
