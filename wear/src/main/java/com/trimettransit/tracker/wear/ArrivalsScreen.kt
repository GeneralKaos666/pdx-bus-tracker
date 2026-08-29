package com.trimettransit.tracker.wear

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
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.transit.TransitApi
import com.trimettransit.tracker.util.minutesUntil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.joda.time.DateTime

/** Live arrivals for one stop, refreshed while the screen is visible. */
@Composable
fun ArrivalsScreen(stop: Stop) {
    val context = LocalContext.current
    val displayName = stop.desc.ifBlank { "Stop ${stop.locId}" }
    var result by remember { mutableStateOf<ArrivalsResult?>(null) }

    LaunchedEffect(stop.locId) {
        while (true) {
            result = withContext(Dispatchers.IO) {
                TransitApi.fetchArrivals(
                    context,
                    listOf(stop.locId),
                    minutes = 30,
                    maxArrivals = 4
                )
            }
            delay(30_000)
        }
    }

    val arrivals = result?.arrivals.orEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            result == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            arrivals.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No arrivals right now", textAlign = TextAlign.Center)
                }
            }
            else -> ArrivalList(displayName, arrivals)
        }
    }
}

@Composable
private fun ArrivalList(displayName: String, arrivals: List<Arrival>) {
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(scrollState = listState) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item {
                ListHeader {
                    Text(displayName, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            itemsIndexed(arrivals) { _, arrival ->
                ArrivalRow(arrival)
            }
        }
    }
}

@Composable
private fun ArrivalRow(arrival: Arrival) {
    val displayTime: DateTime? =
        if (arrival.status == "estimated" && arrival.estimated != null) arrival.estimated
        else arrival.scheduled
    val minutes = displayTime?.let { minutesUntil(it) }

    androidx.compose.foundation.layout.Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Text(
            text = arrival.shortSign.ifBlank { arrival.fullSign },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = when {
                minutes == null -> "No time info"
                minutes <= 0L -> "Due"
                else -> "$minutes min"
            },
            style = MaterialTheme.typography.labelSmall
        )
    }
}
