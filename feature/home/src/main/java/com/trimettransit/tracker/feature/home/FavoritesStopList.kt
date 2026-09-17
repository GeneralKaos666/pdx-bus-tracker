package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.Stop
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
    onRemove: (Stop) -> Unit,
    onRename: (Stop) -> Unit
) {
    ListStateShell(
        isLoading = isLoading,
        isError = isError,
        isEmpty = stops.isEmpty(),
        emptyMessage = emptyText,
        errorMessage = stringResource(R.string.unable_to_load),
        label = "favoritesStopList"
    ) {
        FavoritesList(
            stops = stops,
            onNavigateToArrivals = onNavigateToArrivals,
            onMove = onMove,
            onRemove = onRemove,
            onRename = onRename
        )
    }
}

@Composable
private fun FavoritesList(
    stops: List<Stop>,
    onNavigateToArrivals: (Stop) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Stop) -> Unit,
    onRename: (Stop) -> Unit
) {
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
            top = 8.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = navPillBottomPadding() + 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stops.size, key = { stops[it].locId }, contentType = { "stop" }) { index ->
            val stop = stops[index]
            val dismissState = rememberSwipeToDismissBoxState()
            LaunchedEffect(dismissState.currentValue) {
                if (dismissState.currentValue == SwipeToDismissBoxValue.StartToEnd ||
                    dismissState.currentValue == SwipeToDismissBoxValue.EndToStart
                ) {
                    onRemove(stop)
                }
            }
            SwipeToDismissBox(
                state = dismissState,
                backgroundContent = {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                modifier = Modifier.animateItem()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    StopListItem(
                        stop = stop,
                        onClick = { onNavigateToArrivals(stop) },
                        modifier = if (dense) {
                            Modifier.staggeredFadeIn(index, enabled = !entranceDone)
                        } else {
                            Modifier
                                .favoriteDragToReorder(index = index, onMove = onMove)
                                .staggeredFadeIn(index, enabled = !entranceDone)
                        },
                        gridMode = dense
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (dense) {
                            IconButton(
                                onClick = { onMove(index, index - 1) },
                                enabled = index > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.move_up)
                                )
                            }
                            IconButton(
                                onClick = { onMove(index, index + 1) },
                                enabled = index < stops.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.move_down)
                                )
                            }
                        }
                        IconButton(onClick = { onRename(stop) }) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = stringResource(R.string.rename_favorite)
                            )
                        }
                        IconButton(onClick = { onRemove(stop) }) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.remove_favorite)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Vertical long-press-drag reorder for the 1-column list. The dragged item
 * follows the finger via [graphicsLayer] (draw-phase only); on release the
 * target index is derived from the accumulated offset over the measured item
 * height. A press without movement is a no-op.
 */
@Composable
private fun Modifier.favoriteDragToReorder(
    index: Int,
    onMove: (from: Int, to: Int) -> Unit
): Modifier {
    return this.then(FavoriteDragModifier(index, onMove))
}

@Composable
private fun FavoriteDragModifier(
    index: Int,
    onMove: (from: Int, to: Int) -> Unit
): Modifier {
    val itemHeightPx = remember { mutableFloatStateOf(0f) }
    val dragOffset = remember { mutableFloatStateOf(0f) }
    val dragged = remember { mutableStateOf(false) }
    val currentIndex = rememberUpdatedState(index)
    val currentOnMove = rememberUpdatedState(onMove)
    return Modifier
        .onSizeChanged { itemHeightPx.floatValue = it.height.toFloat() }
        .zIndex(if (dragged.value) 1f else 0f)
        .pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = { dragged.value = true },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffset.floatValue += dragAmount.y
                },
                onDragEnd = {
                    val h = itemHeightPx.floatValue
                    val offset = dragOffset.floatValue
                    dragged.value = false
                    dragOffset.floatValue = 0f
                    if (h > 0f && offset != 0f) {
                        val delta = (offset / h).roundToInt()
                        val target = (currentIndex.value + delta).coerceIn(0, Int.MAX_VALUE)
                        if (target != currentIndex.value) {
                            currentOnMove.value(currentIndex.value, target)
                        }
                    }
                },
                onDragCancel = {
                    dragged.value = false
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
