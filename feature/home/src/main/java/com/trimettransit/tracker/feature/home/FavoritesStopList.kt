package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.FavoriteEdits
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.domain.ErrorCopyKind
import com.trimettransit.tracker.model.domain.errorCopyKind
import com.trimettransit.tracker.ui.components.FavoriteToggleButton
import com.trimettransit.tracker.ui.components.ListStateShell
import com.trimettransit.tracker.ui.components.StopListItem
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.rememberDenseGridEnabled
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.components.staggeredFadeIn
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FavoritesStopList(
    stops: List<Stop>,
    isLoading: Boolean,
    isError: Boolean,
    emptyText: String,
    onNavigateToArrivals: (Stop) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onDeleteRequest: (Stop) -> Unit,
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
        label = "favoritesStopList",
        emptyActions = emptyActions,
        onRetry = onRetry
    ) {
        FavoritesList(
            stops = stops,
            onNavigateToArrivals = onNavigateToArrivals,
            onMove = onMove,
            onDeleteRequest = onDeleteRequest
        )
    }
}

@Composable
private fun FavoritesList(
    stops: List<Stop>,
    onNavigateToArrivals: (Stop) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onDeleteRequest: (Stop) -> Unit
) {
    val dense = rememberDenseGridEnabled()
    val listState = rememberLazyGridState()
    val smoothFling = rememberSmoothFlingBehavior()
    var entranceDone by remember { mutableStateOf(false) }
    var dragging by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(700)
        entranceDone = true
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (dense) 2 else 1),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        flingBehavior = smoothFling,
        userScrollEnabled = !dragging,
        contentPadding = PaddingValues(
            top = 8.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = navPillBottomPadding() + 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(stops, key = { _, stop -> stop.locId }, contentType = { _, _ -> "stop" }) { index, stop ->
            val moveUpLabel = stringResource(R.string.move_favorite_up)
            val moveDownLabel = stringResource(R.string.move_favorite_down)
            StopListItem(
                stop = stop,
                onClick = { onNavigateToArrivals(stop) },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateItem()
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(moveUpLabel) {
                                if (index > 0) {
                                    onMove(index, index - 1)
                                    true
                                } else {
                                    false
                                }
                            },
                            CustomAccessibilityAction(moveDownLabel) {
                                if (index < stops.size - 1) {
                                    onMove(index, index + 1)
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                    }
                    .favoriteDragToReorder(
                        index = index,
                        itemCount = stops.size,
                        spanCount = if (dense) 2 else 1,
                        onMove = onMove,
                        onDraggingChange = { dragging = it }
                    )
                    .staggeredFadeIn(index, enabled = !entranceDone),
                gridMode = dense,
                trailingContent = {
                    // This list only ever holds favourites, so the shared toggle always renders filled
                    // and always removes — keeping its icon and label identical to the hearts on
                    // search results, Recents and Lines.
                    FavoriteToggleButton(
                        isFavorite = true,
                        onClick = { onDeleteRequest(stop) }
                    )
                }
            )
        }
    }
}

/**
 * Vertical long-press-drag reorder. The dragged item follows the finger via
 * [graphicsLayer] (draw-phase only); on release the target index is derived
 * from the accumulated offset over the measured item height, scaled by
 * [spanCount] so dragging one row moves one grid row in two-column mode.
 * The target is clamped to list bounds so drags past either end land on the
 * first/last item instead of silently doing nothing. A press without movement
 * is a no-op. [onDraggingChange] lets the host freeze list scrolling while a
 * drag is in flight so the two never fight over one finger.
 */
@Composable
private fun Modifier.favoriteDragToReorder(
    index: Int,
    itemCount: Int,
    spanCount: Int,
    onMove: (from: Int, to: Int) -> Unit,
    onDraggingChange: (Boolean) -> Unit
): Modifier {
    val itemHeightPx = remember { mutableFloatStateOf(0f) }
    val dragOffset = remember { mutableFloatStateOf(0f) }
    val dragged = remember { mutableStateOf(false) }
    val currentIndex = rememberUpdatedState(index)
    val currentItemCount = rememberUpdatedState(itemCount)
    val currentSpanCount = rememberUpdatedState(spanCount)
    val currentOnMove = rememberUpdatedState(onMove)
    val currentOnDraggingChange = rememberUpdatedState(onDraggingChange)
    return this
        .onSizeChanged { itemHeightPx.floatValue = it.height.toFloat() }
        .zIndex(if (dragged.value) 1f else 0f)
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    dragged.value = true
                    currentOnDraggingChange.value(true)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffset.floatValue += dragAmount.y
                },
                onDragEnd = {
                    val h = itemHeightPx.floatValue
                    val offset = dragOffset.floatValue
                    dragged.value = false
                    currentOnDraggingChange.value(false)
                    dragOffset.floatValue = 0f
                    if (h > 0f && offset != 0f) {
                        val rows = (offset / h).roundToInt()
                        val from = currentIndex.value
                        val target = FavoriteEdits.reorderTarget(
                            from = from,
                            delta = rows * currentSpanCount.value.coerceAtLeast(1),
                            size = currentItemCount.value
                        )
                        if (target != from) {
                            currentOnMove.value(from, target)
                        }
                    }
                },
                onDragCancel = {
                    dragged.value = false
                    currentOnDraggingChange.value(false)
                    dragOffset.floatValue = 0f
                }
            )
        }
        .graphicsLayer {
            if (dragged.value) {
                translationY = dragOffset.floatValue
                scaleX = 1.02f
                scaleY = 1.02f
            }
        }
}
