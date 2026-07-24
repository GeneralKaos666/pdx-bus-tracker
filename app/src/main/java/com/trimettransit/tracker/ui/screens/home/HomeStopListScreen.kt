package com.trimettransit.tracker.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.ui.screens.components.EmptyState
import com.trimettransit.tracker.ui.screens.components.LoadingState
import com.trimettransit.tracker.ui.screens.components.StopListItem

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
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(stops.size, key = { stops[it].locId }) { index ->
                val stop = stops[index]
                StopListItem(
                    stop = stop,
                    onClick = { onNavigateToArrivals(stop) }
                )
            }
        }
    }
}
