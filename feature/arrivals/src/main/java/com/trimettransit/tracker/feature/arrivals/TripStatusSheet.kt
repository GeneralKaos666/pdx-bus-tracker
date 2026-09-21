package com.trimettransit.tracker.feature.arrivals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.TripStatus
import com.trimettransit.tracker.model.TripStopStatus
import org.joda.time.DateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripStatusSheet(
    trip: TripStatus?,
    isLoading: Boolean,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Expanded,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.trip_status_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.padding(24.dp))
                trip == null -> Text(
                    text = stringResource(R.string.trip_status_unavailable),
                    modifier = Modifier.padding(20.dp)
                )
                else -> TripStatusContent(trip)
            }
        }
    }
}

@Composable
private fun TripStatusContent(trip: TripStatus) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = listOfNotNull(
                trip.routeNumber.takeIf { it > 0 }?.toString(),
                trip.destination.takeIf { it.isNotBlank() }
            ).joinToString(" · ").ifBlank { trip.tripId },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        val badges = listOfNotNull(
            trip.extra.takeIf { it }?.let { stringResource(R.string.trip_status_extra) },
            trip.modified.takeIf { it }?.let { stringResource(R.string.trip_status_modified) }
        )
        if (badges.isNotEmpty()) {
            Text(
                text = badges.joinToString(" · "),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        trip.progress?.let { progress ->
            Text(
                text = stringResource(R.string.trip_status_progress, (progress * 100).toInt()),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(trip.stops, key = { "${it.locId}_${it.stopSequence}" }) { stop ->
                val status = when (stop.status) {
                    TripStopStatus.CANCELED -> stringResource(R.string.trip_status_canceled)
                    TripStopStatus.DROP_OFF_ONLY -> stringResource(R.string.trip_status_dropoff_only)
                    TripStopStatus.NORMAL -> if (stop.isPassed) {
                        stringResource(R.string.trip_status_passed)
                    } else {
                        ""
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stop.description.ifBlank {
                                stringResource(R.string.stop_number, stop.locId)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (stop.isPassed) FontWeight.Normal else FontWeight.Medium
                        )
                        val delay = stop.arrivalDelaySeconds ?: stop.departureDelaySeconds
                        if (status.isNotBlank() || delay != null) {
                            Text(
                                text = listOfNotNull(
                                    status.takeIf { it.isNotBlank() },
                                    delay?.let { pluralStringResource(R.plurals.trip_status_delay, it, it) }
                                ).joinToString(" · "),
                                color = if (stop.isCanceled) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Text(
                        text = DateTime(stop.effectiveArrivalMillis).toString("h:mm a"),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
