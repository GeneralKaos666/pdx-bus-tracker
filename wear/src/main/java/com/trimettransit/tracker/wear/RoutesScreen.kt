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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.R
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
        RoutesLevel.ROUTES -> stringResource(R.string.routes)
        RoutesLevel.DIRECTIONS -> routeName
        RoutesLevel.STOPS -> dirName
    }

    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        val isEmpty = when (level) {
            RoutesLevel.ROUTES -> state.routes.isEmpty()
            RoutesLevel.DIRECTIONS -> state.directions.isEmpty()
            RoutesLevel.STOPS -> state.stops.isEmpty()
        }
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    WearFadeInOnce { CircularProgressIndicator() }
                }
            }
            state.isMissingApiKey -> {
                CenteredMessageWithRetry(
                    message = stringResource(R.string.api_key_not_configured),
                    contentPadding = contentPadding,
                    onRetry = { retryKey++ }
                )
            }
            state.failed -> {
                CenteredMessageWithRetry(
                    message = when (level) {
                        RoutesLevel.ROUTES -> stringResource(R.string.unable_to_load_routes)
                        RoutesLevel.DIRECTIONS -> stringResource(R.string.unable_to_load_directions)
                        RoutesLevel.STOPS -> stringResource(R.string.unable_to_load_stops)
                    },
                    contentPadding = contentPadding,
                    onRetry = { retryKey++ }
                )
            }
            isEmpty -> {
                CenteredMessageWithRetry(
                    message = when (level) {
                        RoutesLevel.ROUTES -> stringResource(R.string.no_routes)
                        RoutesLevel.DIRECTIONS -> stringResource(R.string.no_directions)
                        RoutesLevel.STOPS -> stringResource(R.string.no_stops)
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
                                items(state.routes, key = { it.routeId }) { route ->
                                    RouteRow(route, onClick = { onRouteClick(route) })
                                }
                            RoutesLevel.DIRECTIONS ->
                                items(state.directions, key = { it.dir }) { dir ->
                                    DirectionRow(dir, onClick = { onDirectionClick(dir) })
                                }
                            RoutesLevel.STOPS ->
                                items(state.stops, key = { it.locId }) { stop ->
                                    StopRow(stop, onClick = { onStopClick(stop) })
                                }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun rememberRoutesState(
    transitRepository: TransitRepository,
    apiKey: String,
    level: RoutesLevel,
    routeId: Int,
    dirId: Int,
    retryKey: Int
): RoutesListState {
    var routes by remember { mutableStateOf<List<Route>?>(null) }
    var directions by remember { mutableStateOf<List<Direction>?>(null) }
    var stops by remember { mutableStateOf<List<Stop>?>(null) }
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
            val ok = withContext(Dispatchers.IO) {
                when (level) {
                    RoutesLevel.ROUTES -> {
                        transitRepository.getRoutes()?.also { routes = it } != null
                    }
                    RoutesLevel.DIRECTIONS -> {
                        transitRepository.getDirections(routeId)?.also { directions = it } != null
                    }
                    RoutesLevel.STOPS -> {
                        transitRepository.getStops(routeId, dirId)?.also { stops = it } != null
                    }
                }
            }
            if (!ok) failed = true
        }
        isLoading = false
    }

    return RoutesListState(
        routes = routes ?: emptyList(),
        directions = directions ?: emptyList(),
        stops = stops ?: emptyList(),
        isLoading = isLoading,
        isMissingApiKey = isMissingApiKey,
        failed = failed
    )
}

private class RoutesListState(
    val routes: List<Route>,
    val directions: List<Direction>,
    val stops: List<Stop>,
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
                    label = { Text(stringResource(R.string.retry)) }
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
