package com.trimettransit.tracker.feature.stops

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.EmptyState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.ListLoadingSkeleton
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.theme.m3EffectsDefault

/**
 * Shared list shell for the Routes list: Crossfade between loading, error,
 * empty and the smooth-fling grid (single column, or two columns on wide panes
 * when [gridMode]). [itemTrailingContent] renders under each item (used by the
 * routes accordion's expanded sub-cards). [onRetry], when provided, adds a
 * Try Again button to the error state.
 */
@Composable
internal fun <T> StopListContent(
    isLoading: Boolean,
    items: List<T>?,
    errorMessage: String,
    emptyMessage: String,
    stateLabel: String,
    gridMode: Boolean,
    key: (T) -> Any,
    contentType: (T) -> Any?,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit,
    itemTrailingContent: @Composable (T) -> Unit = {},
    onRetry: (() -> Unit)? = null
) {
    val safeItems = items
    Crossfade(
        targetState = when {
            isLoading -> 0
            safeItems == null -> 1
            safeItems.isEmpty() -> 2
            else -> 3
        },
        animationSpec = m3EffectsDefault(),
        label = stateLabel
    ) { state ->
        when (state) {
            0 -> ListLoadingSkeleton()
            1 -> {
                ErrorState(
                    message = errorMessage,
                    onRetry = onRetry
                )
            }
            2 -> EmptyState(message = emptyMessage)
            else -> {
                ContentEntrance(modifier = Modifier.fillMaxSize()) {
                    val listState = rememberLazyGridState()
                    val smoothFling = rememberSmoothFlingBehavior()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (gridMode) 2 else 1),
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
                        items(safeItems ?: emptyList(), key = key, contentType = contentType) { item ->
                            itemContent(item)
                            itemTrailingContent(item)
                        }
                    }
                }
            }
        }
    }
}