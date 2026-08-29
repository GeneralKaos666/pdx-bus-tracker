package com.trimettransit.tracker.wear

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.items
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.wear.data.WearDataPull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class WatchStopListState(
    val stops: List<Stop>,
    val isLoading: Boolean
)

/**
 * Watch-flavored stop list loader: refreshes the local cache from the phone
 * (Wearable Data Layer) before reading it, so the lists stay in sync.
 */
@Composable
private fun rememberWatchStopList(read: (DatabaseHelper) -> List<Stop>): WatchStopListState {
    val context = LocalContext.current
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        runCatching {
            withContext(Dispatchers.IO) { WearDataPull.pullInto(context) }
        }
        stops = withContext(Dispatchers.IO) { read(DatabaseHelper(context)) }
        isLoading = false
    }

    return WatchStopListState(stops, isLoading)
}

@Composable
fun StopListScreen(
    header: String,
    read: (DatabaseHelper) -> List<Stop>,
    emptyText: String,
    onStopClick: (Stop) -> Unit
) {
    val state = rememberWatchStopList(read)

    Column(modifier = Modifier.fillMaxSize()) {
        TimeText()
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            else -> ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
            ) {
                item { ListHeader { Text(header) } }
                if (state.stops.isEmpty()) {
                    item {
                        Text(
                            text = emptyText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    items(state.stops, key = { it.locId }) { stop ->
                        StopRow(stop, onClick = { onStopClick(stop) })
                    }
                }
            }
        }
    }
}

@Composable
private fun StopRow(stop: Stop, onClick: () -> Unit) {
    Chip(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        label = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = stop.desc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
                if (stop.dirDesc.isNotBlank()) {
                    Text(
                        text = stop.dirDesc,
                        style = MaterialTheme.typography.caption2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            }
        }
    )
}