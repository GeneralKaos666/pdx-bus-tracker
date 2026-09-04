package com.trimettransit.tracker.feature.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.components.searchStops
import com.trimettransit.tracker.ui.components.StopSearchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Search field pinned to the top of the Favorites screen. Typing a query opens a
 * floating dropdown of matching stops over the favorites list; clearing the
 * query (or tapping a result) collapses it. All stops are lazy-loaded on the
 * first non-blank query so opening Favorites never costs a network call.
 */
@Composable
fun HomeSearchBar(
    transitRepository: TransitRepository,
    onStopSelected: (Stop) -> Unit,
    content: @Composable () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var allStops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Stop>>(emptyList()) }

    // Lazy-load the full stop list once, on the first non-blank query. Keyed on
    // (allStops == null, query.isNotBlank()) so typing never re-launches the
    // network call while the list is still loading.
    LaunchedEffect(allStops == null, query.isNotBlank()) {
        if (allStops == null && query.isNotBlank()) {
            isLoading = true
            hasError = false
            allStops = transitRepository.searchStops()
            isLoading = false
            if (allStops == null) hasError = true
        }
    }

    LaunchedEffect(query, allStops) {
        // Drop the previous query's matches immediately so the dropdown never shows
        // stale results under the new text while the search is being recomputed.
        results = emptyList()
        if (query.isNotBlank() && allStops != null) {
            results = withContext(Dispatchers.Default) { searchStops(allStops!!, query) }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            shape = RoundedCornerShape(28.dp),
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
                            onClick = { query = "" },
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

        Box(modifier = Modifier.fillMaxSize()) {
            content()

            SearchResultsDropdown(
                query = query,
                isLoading = isLoading,
                hasError = hasError,
                allStops = allStops,
                results = results,
                onStopClick = { stop ->
                    query = ""
                    onStopSelected(stop)
                },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
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
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = query.isNotBlank(),
        modifier = modifier,
        enter = expandVertically(
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)),
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
            shrinkTowards = Alignment.Top
        ) + fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing))
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxHeight(0.65f)
        ) {
            when {
                isLoading && allStops == null -> SearchPanelLoading()
                hasError && allStops == null -> SearchPanelMessage(
                    stringResource(R.string.no_connection)
                )
                results.isEmpty() -> SearchPanelMessage(stringResource(R.string.no_stops_found))
                else -> {
                    val listState = rememberLazyListState()
                    val smoothFling = rememberSmoothFlingBehavior()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        flingBehavior = smoothFling
                    ) {
                        items(results, key = { it.locId }, contentType = { "stopSearch" }) { stop ->
                            StopSearchItem(
                                stop = stop,
                                onClick = { onStopClick(stop) },
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
private fun SearchPanelLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.loading_stops),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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