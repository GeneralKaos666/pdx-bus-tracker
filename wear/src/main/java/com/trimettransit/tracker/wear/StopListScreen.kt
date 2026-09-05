package com.trimettransit.tracker.wear

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
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
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.trimettransit.tracker.model.Stop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class WatchStopListState(
    val stops: List<Stop>,
    val isLoading: Boolean
)

/**
 * Watch-flavored stop list loader: reads the stop list straight from this
 * device's local database via the repository boundary (standalone — no phone
 * sync involved).
 */
@Composable
private fun rememberWatchStopList(read: suspend () -> List<Stop>): WatchStopListState {
    val context = LocalContext.current
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        stops = withContext(Dispatchers.IO) { read() }
        isLoading = false
    }

    return WatchStopListState(stops, isLoading)
}

@Composable
fun StopListScreen(
    header: String,
    read: suspend () -> List<Stop>,
    emptyText: String,
    onStopClick: (Stop) -> Unit
) {
    val state = rememberWatchStopList(read)
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    WearFadeInOnce { CircularProgressIndicator() }
                }
            }
            else -> WearContentEntrance(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                TransformingLazyColumn(
                    state = listState
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
}

@Composable
private fun StopRow(stop: Stop, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().wearPressScale(interactionSource),
        label = {
            Text(
                text = stop.desc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        },
        secondaryLabel = {
            if (stop.dirDesc.isNotBlank()) {
                Text(
                    text = stop.dirDesc,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Start
                )
            }
        }
    )
}
