package com.trimettransit.tracker.feature.trips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import com.trimettransit.tracker.util.nextMinuteBoundaryDelayMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * "Updated X min ago" text that re-computes on each wall-clock minute, for this label only.
 *
 * Returns a [State] because the caller's emit site must observe the tick; returning a plain
 * String froze the chip at its first-composition value. The tick lives here so a minute
 * passing recomposes the text, not the whole planner. [active] stops the loop while the page
 * is offscreen, since the pager keeps neighbouring pages composed.
 */
@Composable
fun rememberAgeLabel(active: Boolean, label: (nowMillis: Long) -> String?): State<String> {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    val currentLabel = rememberUpdatedState(label)
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        now = System.currentTimeMillis()
        while (isActive) {
            delay(nextMinuteBoundaryDelayMillis())
            now = System.currentTimeMillis()
        }
    }
    return remember { derivedStateOf { currentLabel.value(now).orEmpty() } }
}
