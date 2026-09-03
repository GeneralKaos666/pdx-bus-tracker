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
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.components.transitTypeLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private const val MAX_RESULTS = 250

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
                placeholder = { Text("Search stops by name or ID") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        val clearSource = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = { query = "" },
                            interactionSource = clearSource,
                            modifier = Modifier.pressScale(clearSource)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
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
                    "No connection.\nPlease check your internet."
                )
                results.isEmpty() -> SearchPanelMessage("No stops found.")
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
                text = "Loading stops…",
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

@Composable
private fun StopSearchItem(
    stop: Stop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val typeColor = remember(stop.transitType, colorScheme) {
            transitColor(stop.transitType, colorScheme)
        }
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = typeColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = transitIconResource(stop.transitType)),
                    contentDescription = transitTypeLabel(stop.transitType),
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.desc,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stop.dirDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun searchStops(allStops: List<Stop>, query: String): List<Stop> {
    val trimmed = query.trim().lowercase(Locale.US)
    if (trimmed.isEmpty()) return emptyList()

    val queryAsInt = trimmed.toIntOrNull()

    return allStops.filter { stop ->
        val matchDesc = stop.desc.lowercase(Locale.US).contains(trimmed)
        val matchDir = stop.dirDesc.lowercase(Locale.US).contains(trimmed)
        val matchId = queryAsInt != null && stop.locId.toString().contains(queryAsInt.toString())
        matchDesc || matchDir || matchId
    }.take(MAX_RESULTS)
}