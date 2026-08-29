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
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.itemsIndexed
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

    Column(modifier = Modifier.fillMaxSize()) {
        TimeText()
        when {
            result == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            arrivals.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No arrivals right now", textAlign = TextAlign.Center)
                }
            }
            else -> ScalingLazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
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
}

@Composable
private fun ArrivalRow(arrival: Arrival) {
    val displayTime: DateTime? =
        if (arrival.status == "estimated" && arrival.estimated != null) arrival.estimated
        else arrival.scheduled
    val minutes = displayTime?.let { minutesUntil(it) }

    Column(
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
            style = MaterialTheme.typography.caption2
        )
    }
}