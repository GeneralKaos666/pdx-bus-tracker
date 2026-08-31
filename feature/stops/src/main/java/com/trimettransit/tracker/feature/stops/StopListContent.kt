package com.trimettransit.tracker.feature.stops

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.EmptyState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.ListLoadingSkeleton
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior

/**
 * Shared list shell for the Routes list: Crossfade between loading, error,
 * empty and the smooth-fling LazyColumn. [itemTrailingContent] renders under
 * each item (used by the routes accordion's expanded sub-cards). [onRetry],
 * when provided, adds a Try Again button to the error state.
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
    itemTrailingContent: @Composable LazyItemScope.(T) -> Unit = {},
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
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = stateLabel
    ) { state ->
        when (state) {
            0 -> ListLoadingSkeleton()
            1 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        ErrorState(message = errorMessage)
                        if (onRetry != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            val retrySource = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = onRetry,
                                interactionSource = retrySource,
                                modifier = Modifier.pressScale(retrySource)
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
            }
            2 -> EmptyState(message = emptyMessage)
            else -> {
                ContentEntrance(modifier = Modifier.fillMaxSize()) {
                    val listState = rememberLazyListState()
                    val smoothFling = rememberSmoothFlingBehavior()
                    LazyColumn(
                        state = listState,
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