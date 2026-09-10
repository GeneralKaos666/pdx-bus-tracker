package com.trimettransit.tracker.ui.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Bottom clearance needed so scrollable content isn't hidden under the floating
 * bottom navigation pill. Returns the nav-bar inset plus the pill's own height
 * and its 16.dp margin. Pass directly into a list's `contentPadding` or use with
 * `Modifier.padding(navPillBottomPadding())`.
 */
@Composable
fun navPillBottomPadding(): Dp {
    val navBarsBottom = WindowInsets.navigationBars
        .asPaddingValues().calculateBottomPadding()
    return navBarsBottom + 56.dp
}