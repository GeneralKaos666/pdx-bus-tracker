package com.trimettransit.tracker.feature.trips

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.trimettransit.tracker.util.nextMinuteBoundaryDelayMillis
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/**
 * "Updated X min ago" text that re-computes on each wall-clock minute, for this label only.
 *
 * The tick lives here so a minute passing recomposes the text, not the whole planner (map,
 * endpoint card, results). [active] stops the loop while the page is offscreen, since the
 * pager keeps neighbouring pages composed.
 */
@Composable
fun rememberAgeLabel(active: Boolean, label: (nowMillis: Long) -> String?): String {
    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        if (!active) return@LaunchedEffect
        now = System.currentTimeMillis()
        while (isActive) {
            delay(nextMinuteBoundaryDelayMillis())
            now = System.currentTimeMillis()
        }
    }
    return label(now).orEmpty()
}
