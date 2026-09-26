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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.domain.ErrorCopyKind
import com.trimettransit.tracker.model.domain.errorCopyKind
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.FavoriteToggleButton
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
    favoriteIds: Set<Int>,
    onToggleFavorite: (Stop) -> Unit,
    onDismiss: (Stop) -> Unit,
    emptyActions: @Composable (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null
) {
    // Local SQLite-backed list: a load failure is never a network error, so route the
    // copy choice through errorCopyKind to keep connection copy off local failures.
    val errorMessage = when (errorCopyKind(isNetworkError = false, hasCachedData = stops.isNotEmpty())) {
        ErrorCopyKind.CONNECTION -> stringResource(R.string.no_connection)
        ErrorCopyKind.LOCAL, ErrorCopyKind.EMPTY -> stringResource(R.string.unable_to_load)
    }
    ListStateShell(
        isLoading = isLoading,
        isError = isError,
        isEmpty = stops.isEmpty(),
        emptyMessage = emptyText,
        errorMessage = errorMessage,
        label = "recentStopsStopList",
        emptyActions = emptyActions,
        onRetry = onRetry
    ) {
        RecentStopsList(
            stops = stops,
            onNavigateToArrivals = onNavigateToArrivals,
            favoriteIds = favoriteIds,
            onToggleFavorite = onToggleFavorite,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun RecentStopsList(
    stops: List<Stop>,
    onNavigateToArrivals: (Stop) -> Unit,
    favoriteIds: Set<Int>,
    onToggleFavorite: (Stop) -> Unit,
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
                            FavoriteToggleButton(
                                isFavorite = favoriteIds.contains(stop.locId),
                                onClick = { onToggleFavorite(stop) }
                            )
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
