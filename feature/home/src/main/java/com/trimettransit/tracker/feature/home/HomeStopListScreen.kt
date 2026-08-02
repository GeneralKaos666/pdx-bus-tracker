package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.EmptyState
import com.trimettransit.tracker.ui.components.LoadingState
import com.trimettransit.tracker.ui.components.StopListItem
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior

@Composable
fun HomeStopListScreen(
    stops: List<Stop>,
    isLoading: Boolean,
    emptyText: String,
    onNavigateToArrivals: (Stop) -> Unit
) {
    if (isLoading) {
        LoadingState()
    } else if (stops.isEmpty()) {
        EmptyState(message = emptyText)
    } else {
        ContentEntrance(modifier = Modifier.fillMaxSize()) {
        val smoothFling = rememberSmoothFlingBehavior()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            flingBehavior = smoothFling,
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(stops.size, key = { stops[it].locId }, contentType = { "stop" }) { index ->
                val stop = stops[index]
                StopListItem(
                    stop = stop,
                    onClick = { onNavigateToArrivals(stop) },
                    modifier = Modifier.animateItem()
                )
            }
        }
        }
    }
}
