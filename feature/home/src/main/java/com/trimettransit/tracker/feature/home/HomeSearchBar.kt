package com.trimettransit.tracker.feature.home

import com.trimettransit.tracker.ui.components.rememberPressSource
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.trimettransit.tracker.ui.components.SearchPanelState
import com.trimettransit.tracker.ui.components.StopSearchItem
import com.trimettransit.tracker.ui.components.rememberStopSearchResults
import com.trimettransit.tracker.ui.components.staggeredFadeIn
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.m3ContentExpand
import com.trimettransit.tracker.ui.theme.m3ContentShrink
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
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
    var query by rememberSaveable { mutableStateOf("") }
    val search = rememberStopSearchResults(
        loadStops = { transitRepository.searchStops() },
        query = query
    )

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
                        val clearSource = rememberPressSource()
                        IconButton(
                            onClick = {
                                query = ""
                                focusManager.clearFocus()
                            },
                            interactionSource = clearSource,
                            modifier = Modifier.size(48.dp).pressScale(clearSource)
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
                    isLoading = search.isLoading,
                    hasError = search.hasError,
                    allStops = search.stops,
                    results = search.results,
                    onStopClick = { stop ->
                        query = ""
                        focusManager.clearFocus()
                        onStopSelected(stop)
                    },
                    onRetry = search.retry,
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
            val panelState = when {
                // The rest of the app shows shimmer placeholders while a list loads; a lone
                // spinner here was the odd one out.
                isLoading && allStops == null -> SearchPanelState.Loading
                hasError && allStops == null -> SearchPanelState.Error
                results.isEmpty() -> SearchPanelState.NoResults
                else -> SearchPanelState.Results
            }
            Crossfade(
                targetState = panelState,
                animationSpec = m3EffectsDefault(),
                label = "searchPanelState"
            ) { state ->
                when (state) {
                    SearchPanelState.Loading -> ListLoadingSkeleton(rows = 4)
                    SearchPanelState.Error -> ErrorState(
                        message = stringResource(R.string.no_connection),
                        onRetry = onRetry
                    )
                    SearchPanelState.NoResults -> SearchPanelMessage(stringResource(R.string.no_stops_found))
                    else -> {
                        val dense = rememberDenseGridEnabled()
                        val listState = rememberLazyGridState()
                        val smoothFling = rememberSmoothFlingBehavior()
                        var entranceDone by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) {
                            delay(700)
                            entranceDone = true
                        }
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
                            itemsIndexed(results, key = { _, stop -> stop.locId }, contentType = { _, _ -> "stopSearch" }) { index, stop ->
                                StopSearchItem(
                                    stop = stop,
                                    onClick = { onStopClick(stop) },
                                    modifier = (if (AppMotion.reduceMotion) Modifier else Modifier.animateItem())
                                        .staggeredFadeIn(index, enabled = !entranceDone),
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