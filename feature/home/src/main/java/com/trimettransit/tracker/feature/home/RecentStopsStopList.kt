package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.ListStateShell
import com.trimettransit.tracker.ui.components.StopListItem
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.rememberDenseGridEnabled
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior

@Composable
fun RecentStopsStopList(
    stops: List<Stop>,
    isLoading: Boolean,
    isError: Boolean,
    emptyText: String,
    onNavigateToArrivals: (Stop) -> Unit,
    onPromote: (Stop) -> Unit,
    onDismiss: (Stop) -> Unit,
    emptyActions: @Composable (() -> Unit)? = null
) {
    ListStateShell(
        isLoading = isLoading,
        isError = isError,
        isEmpty = stops.isEmpty(),
        emptyMessage = emptyText,
        errorMessage = stringResource(R.string.unable_to_load),
        label = "recentStopsStopList",
        emptyActions = emptyActions
    ) {
        RecentStopsList(
            stops = stops,
            onNavigateToArrivals = onNavigateToArrivals,
            onPromote = onPromote,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun RecentStopsList(
    stops: List<Stop>,
    onNavigateToArrivals: (Stop) -> Unit,
    onPromote: (Stop) -> Unit,
    onDismiss: (Stop) -> Unit
) {
    ContentEntrance(modifier = Modifier.fillMaxSize()) {
        val dense = rememberDenseGridEnabled()
        val listState = rememberLazyGridState()
        val smoothFling = rememberSmoothFlingBehavior()
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (dense) 2 else 1),
            state = listState,
            modifier = Modifier.fillMaxSize(),
            flingBehavior = smoothFling,
            contentPadding = PaddingValues(
                top = 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = navPillBottomPadding() + 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(stops, key = { it.locId }, contentType = { "stop" }) { stop ->
                StopListItem(
                    stop = stop,
                    onClick = { onNavigateToArrivals(stop) },
                    modifier = Modifier.animateItem(),
                    gridMode = dense,
                    trailingContent = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onPromote(stop) }) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = stringResource(R.string.add_to_favorites)
                                )
                            }
                            IconButton(onClick = { onDismiss(stop) }) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.remove_recent)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
