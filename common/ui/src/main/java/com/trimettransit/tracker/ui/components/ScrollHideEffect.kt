package com.trimettransit.tracker.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.NavState
import kotlinx.coroutines.flow.drop

/**
 * Fades the bottom bar out while the list scrolls down and back in while it
 * scrolls up, via [NavState.bottomBarVisible]. Used by the Arrivals screen
 * only; harmless to call with a scroll state that is not yet composed (it only
 * reacts to offset changes).
 */
@Composable
fun AutoHideBottomBarEffect(state: LazyListState) {
    val threshold = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(state) {
        var previous = scrollPosition(state)
        snapshotFlow { scrollPosition(state) }.drop(1).collect { position ->
            val delta = position - previous
            previous = position
            if (delta < -threshold && NavState.bottomBarVisible) {
                NavState.bottomBarVisible = false
            } else if (delta > threshold && !NavState.bottomBarVisible) {
                NavState.bottomBarVisible = true
            }
        }
    }
}

/** Monotonic approximation of the list's scroll position, immune to the
 *  first-visible-item offset jumping by a whole item when one leaves the
 *  viewport. */
private fun scrollPosition(state: LazyListState): Float {
    val first = state.layoutInfo.visibleItemsInfo.firstOrNull() ?: return 0f
    return (first.index.toFloat() * first.size) + state.firstVisibleItemScrollOffset
}