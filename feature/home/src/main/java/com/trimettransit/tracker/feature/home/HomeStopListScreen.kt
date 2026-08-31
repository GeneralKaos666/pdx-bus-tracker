package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.EmptyState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.ListLoadingSkeleton
import com.trimettransit.tracker.ui.components.StopListItem
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior

@Composable
fun HomeStopListScreen(
    stops: List<Stop>,
    isLoading: Boolean,
    isError: Boolean,
    emptyText: String,
    onNavigateToArrivals: (Stop) -> Unit
) {
    Crossfade(
        targetState = when {
            isLoading && stops.isEmpty() -> 0
            isError && stops.isEmpty() -> 1
            stops.isEmpty() -> 2
            else -> 3
        },
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "homeStopList"
    ) { state ->
        when (state) {
            0 -> ListLoadingSkeleton()
            1 -> ErrorState(message = "Unable to load.\nCheck your connection.")
            2 -> EmptyState(message = emptyText)
            else -> ContentEntrance(modifier = Modifier.fillMaxSize()) {
                val listState = rememberLazyListState()
                val smoothFling = rememberSmoothFlingBehavior()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    flingBehavior = smoothFling,
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(stops.size, key = { stops[it].locId }, contentType = { "stop" }) { index ->
                        val stop = stops[index]
                        StopListItem(
                            stop = stop,
                            onClick = { onNavigateToArrivals(stop) },
                            modifier = Modifier.animateItem(),
                            zoomOnTap = true
                        )
                    }
                }
            }
        }
    }
}