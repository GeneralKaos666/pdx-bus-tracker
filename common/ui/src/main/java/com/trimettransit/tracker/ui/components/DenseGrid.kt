package com.trimettransit.tracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp

/**
 * True when the window is wide enough (>=600dp) for lists to flow into two columns.
 *
 * Mirrors [androidx.compose.ui.platform.WindowInfo] usage in BottomNavigationBar: reads
 * the raw window size and converts to dp. Below 600dp the app is single-pane (phone); the
 * two-pane split only activates at >=840dp where the content pane stays >=600dp wide, so a
 * window-width check is exact for every layout the app renders.
 */
private val DENSE_GRID_MIN_WIDTH = 600.dp

@Composable
fun rememberDenseGridEnabled(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    return with(density) { windowInfo.containerSize.width.toDp() } >= DENSE_GRID_MIN_WIDTH
}