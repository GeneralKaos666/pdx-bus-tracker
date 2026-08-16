package com.trimettransit.tracker.feature.stops

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.EmptyState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.LoadingState
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior

/**
 * Shared list shell for the Routes list: Crossfade between loading, error,
 * empty and the smooth-fling LazyColumn. [itemTrailingContent] renders under
 * each item (used by the routes accordion's expanded sub-cards).
 */
@Composable
internal fun <T> StopListContent(
    isLoading: Boolean,
    items: List<T>?,
    errorMessage: String,
    emptyMessage: String,
    stateLabel: String,
    key: (T) -> Any,
    contentType: (T) -> Any?,
    itemContent: @Composable LazyItemScope.(T) -> Unit,
    itemTrailingContent: @Composable LazyItemScope.(T) -> Unit = {}
) {
    val safeItems = items
    Crossfade(
        targetState = when {
            isLoading -> 0
            safeItems == null -> 1
            safeItems.isEmpty() -> 2
            else -> 3
        },
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = stateLabel
    ) { state ->
        when (state) {
            0 -> LoadingState()
            1 -> ErrorState(message = errorMessage)
            2 -> EmptyState(message = emptyMessage)
            else -> {
                ContentEntrance(modifier = Modifier.fillMaxSize()) {
                    val smoothFling = rememberSmoothFlingBehavior()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        flingBehavior = smoothFling,
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
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