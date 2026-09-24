package com.trimettransit.tracker.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.components.ListLoadingSkeleton
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.FavoriteToggleButton
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberDenseGridEnabled
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.components.searchStops
import com.trimettransit.tracker.ui.components.StopSearchItem
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.m3ContentExpand
import com.trimettransit.tracker.ui.theme.m3ContentShrink
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Search field pinned to the top of the Favorites screen. Typing a query opens a
 * floating dropdown of matching stops over the favorites list; clearing the
 * query (or tapping a result) collapses it. All stops are lazy-loaded on the
 * first non-blank query so opening Favorites never costs a network call.
 */
@OptIn(FlowPreview::class)
@Composable
fun HomeSearchBar(
    transitRepository: TransitRepository,
    favoriteIds: Set<Int>,
    onToggleFavorite: (Stop) -> Unit,
    onStopSelected: (Stop) -> Unit,
    header: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var query by remember { mutableStateOf("") }
    var allStops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    // Bumped by the error panel's retry action so the fetch effect re-runs.
    var attempt by remember { mutableIntStateOf(0) }
    var results by remember { mutableStateOf<List<Stop>>(emptyList()) }

    // Lazy-load the full stop list once, on the first non-blank query. Keyed on
    // (allStops == null, query.isNotBlank()) so typing never re-launches the
    // network call while the list is still loading.
    LaunchedEffect(allStops == null, query.isNotBlank(), attempt) {
        if (allStops == null && query.isNotBlank()) {
            isLoading = true
            hasError = false
            allStops = transitRepository.searchStops()
            isLoading = false
            if (allStops == null) hasError = true
        }
    }

    // Debounced search: rapid keystrokes share one filter pass over the multi-MB
    // list instead of re-filtering per character. snapshotFlow tracks the query;
    // allStops is the effect key (loaded once, then stable). collectLatest cancels
    // a superseded filter so only the latest query publishes.
    LaunchedEffect(allStops) {
        snapshotFlow { query }
            .debounce(200.milliseconds)
            .collectLatest { q ->
                val stops = allStops
                results = if (q.isBlank() || stops == null) {
                    emptyList()
                } else {
                    withContext(Dispatchers.Default) { searchStops(stops, q) }
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = appCardShape(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_stops_hint)) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        val clearSource = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = {
                                query = ""
                                focusManager.clearFocus()
                            },
                            interactionSource = clearSource,
                            modifier = Modifier.pressScale(clearSource)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.clear_search))
                        }
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(query) {
                    detectTapGestures { focusManager.clearFocus() }
                }
        ) {
            HomeSearchHeader(visible = query.isBlank() && header != null, header = header)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                content()

                SearchResultsDropdown(
                    query = query,
                    isLoading = isLoading,
                    hasError = hasError,
                    allStops = allStops,
                    results = results,
                    onStopClick = { stop ->
                        query = ""
                        focusManager.clearFocus()
                        onStopSelected(stop)
                    },
                    onRetry = { attempt++ },
                    favoriteIds = favoriteIds,
                    onToggleFavorite = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun HomeSearchHeader(
    visible: Boolean,
    header: (@Composable () -> Unit)?
) {
    if (header == null) return
    AnimatedVisibility(
        visible = visible,
        enter = m3ContentExpand(),
        exit = m3ContentShrink(shrinkTowards = Alignment.Top)
    ) {
        header()
    }
}

@Composable
private fun SearchResultsDropdown(
    query: String,
    isLoading: Boolean,
    hasError: Boolean,
    allStops: List<Stop>?,
    results: List<Stop>,
    onStopClick: (Stop) -> Unit,
    onRetry: () -> Unit,
    favoriteIds: Set<Int>,
    onToggleFavorite: (Stop) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = query.isNotBlank(),
        modifier = modifier,
        enter = m3ContentExpand(),
        exit = m3ContentShrink(shrinkTowards = Alignment.Top)
    ) {
        Surface(
            shape = appCardShape(),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxHeight(0.65f)
        ) {
            when {
                // The rest of the app shows shimmer placeholders while a list loads; a lone
                // spinner here was the odd one out.
                isLoading && allStops == null -> ListLoadingSkeleton(rows = 4)
                hasError && allStops == null -> ErrorState(
                    message = stringResource(R.string.no_connection),
                    onRetry = onRetry
                )
                results.isEmpty() -> SearchPanelMessage(stringResource(R.string.no_stops_found))
                else -> {
                    val dense = rememberDenseGridEnabled()
                    val listState = rememberLazyGridState()
                    val smoothFling = rememberSmoothFlingBehavior()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (dense) 2 else 1),
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        flingBehavior = smoothFling,
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = navPillBottomPadding()
                        ),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(results, key = { it.locId }, contentType = { "stopSearch" }) { stop ->
                            StopSearchItem(
                                stop = stop,
                                onClick = { onStopClick(stop) },
                                modifier = Modifier.animateItem(),
                                gridMode = dense,
                                trailingContent = {
                                    FavoriteToggleButton(
                                        isFavorite = favoriteIds.contains(stop.locId),
                                        onClick = { onToggleFavorite(stop) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchPanelMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}