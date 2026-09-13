package com.trimettransit.tracker.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripLeg
import com.trimettransit.tracker.model.TripPlan
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitOnColor
import com.trimettransit.tracker.ui.theme.LocalCardStyle
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.util.clockTime
import org.joda.time.DateTime
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ItineraryResultsSheet(
    plan: TripPlan,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    val selected = plan.itineraries.getOrNull(selectedIndex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(plan.itineraries.size, key = { it }) { index ->
                    val itinerary = plan.itineraries[index]
                    val label = stringResource(
                        when (index % 3) {
                            0 -> R.string.itinerary_1
                            1 -> R.string.itinerary_2
                            else -> R.string.itinerary_3
                        }
                    )
                    FilterChip(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        label = { Text("$label · ${formatDurationMillis(itinerary.durationMillis)}") }
                    )
                }
            }

            if (selected != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TripSummaryHeader(itinerary = selected, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    itemsIndexed(selected.legs, key = { index, _ -> index }, contentType = { _, _ -> "leg" }) { _, leg ->
                        LegRow(leg = leg)
                    }
                }
            }
        }
    }
}

@Composable
internal fun TripSummaryHeader(itinerary: TripItinerary, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = itinerary.departure.clockTimeText(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = stringResource(R.string.walk),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = itinerary.arrival.clockTimeText(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatDurationMillis(itinerary.durationMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (itinerary.numberOfTransfers > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = pluralStringResource(R.plurals.transfers_count, itinerary.numberOfTransfers, itinerary.numberOfTransfers),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            itinerary.legs.filter { !it.isWalk }
                .distinctBy { it.routeNumber to it.routeName }
                .forEach { leg ->
                    RouteBadge(
                        leg = leg,
                        contentDescription = leg.routeName?.takeIf { it.isNotBlank() }
                            ?: leg.direction.takeIf { it.isNotBlank() }
                            ?: leg.mode.transitTypeLetter()
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
            Text(
                text = stringResource(
                    R.string.walk_transit_fmt,
                    formatDurationMillis(itinerary.walkTimeMillis),
                    formatDurationMillis(itinerary.transitTimeMillis)
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        itinerary.fare?.let { fare ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.fare_label, fare),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun RouteBadge(
    leg: TripLeg,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    val scheme = MaterialTheme.colorScheme
    val letter = leg.mode.transitTypeLetter()
    val badgeDescription = contentDescription
    val overrides = LocalAppearanceStyle.current.transitTypeColors
    Surface(
        shape = appCardShape(),
        color = transitColor(letter, scheme, overrides),
        modifier = if (badgeDescription != null) {
            modifier.semantics { this.contentDescription = badgeDescription }
        } else {
            modifier
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .clearAndSetSemantics { }
        ) {
            Text(
                text = (leg.routeNumber?.takeIf { it.isNotEmpty() && letter == "B" }) ?: letter,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = transitOnColor(letter, scheme, overrides)
            )
        }
    }
}

@Composable
internal fun LegRow(leg: TripLeg) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        if (leg.isWalk) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.walk),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (leg.direction.isNotBlank()) {
                    Text(
                        text = leg.direction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (leg.from.description.isNotBlank() && leg.to.description.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.walk_between_fmt, leg.from.description, leg.to.description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            val letter = leg.mode.transitTypeLetter()
            RouteBadge(leg = leg)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = leg.routeName?.takeIf { it.isNotBlank() }
                        ?: leg.direction.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.route_label),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.leg_transit_fmt,
                        leg.departure.clockTimeText(),
                        leg.from.description,
                        leg.arrival.clockTimeText(),
                        leg.to.description
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (leg.stayOnBoard) {
                    Surface(
                        shape = appCardShape(),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = stringResource(R.string.stay_on_board),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

internal fun formatDurationMillis(ms: Long): String {
    val totalMin = (ms / 60_000L).coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

/** Formats a scheduled time, or an em dash when the WS returned none (walk legs/itineraries). */
internal fun DateTime?.clockTimeText(): String =
    this?.let { val t = clockTime(it); "${t.text} ${t.period}" } ?: "—"
