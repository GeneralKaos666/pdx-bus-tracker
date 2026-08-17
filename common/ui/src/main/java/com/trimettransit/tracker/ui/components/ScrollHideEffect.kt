package com.trimettransit.tracker.ui.components

import androidx.compose.foundation.ScrollState
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
 * scrolls up, via [NavState.bottomBarVisible]. One per scrollable screen;
 * harmless to call with a scroll state that is not yet composed (it only
 * reacts to offset changes).
 */
@Composable
fun AutoHideBottomBarEffect(state: LazyListState) {
    val threshold = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(state) {
        var previous = state.layoutInfo.visibleItemsInfo.firstOrNull()?.offset ?: 0
        snapshotFlow {
            state.layoutInfo.visibleItemsInfo.firstOrNull()?.offset ?: 0
        }.drop(1).collect { offset ->
            val delta = offset - previous
            previous = offset
            if (delta > threshold && NavState.bottomBarVisible) {
                NavState.bottomBarVisible = false
            } else if (delta < -threshold && !NavState.bottomBarVisible) {
                NavState.bottomBarVisible = true
            }
        }
    }
}

/**
 * [ScrollState] variant for non-lazy scrollables (Settings' verticalScroll).
 */
@Composable
fun AutoHideBottomBarEffect(state: ScrollState) {
    val threshold = with(LocalDensity.current) { 24.dp.toPx() }
    LaunchedEffect(state) {
        var previous = state.value
        snapshotFlow { state.value }.drop(1).collect { value ->
            val delta = value - previous
            previous = value
            if (delta > threshold && NavState.bottomBarVisible) {
                NavState.bottomBarVisible = false
            } else if (delta < -threshold && !NavState.bottomBarVisible) {
                NavState.bottomBarVisible = true
            }
        }
    }
}