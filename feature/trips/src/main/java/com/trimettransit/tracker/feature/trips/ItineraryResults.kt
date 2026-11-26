package com.trimettransit.tracker.feature.trips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripLeg
import com.trimettransit.tracker.model.TripPlan
import com.trimettransit.tracker.model.domain.ItineraryRank
import com.trimettransit.tracker.model.domain.ItineraryRowItem
import com.trimettransit.tracker.model.domain.itineraryRanks
import com.trimettransit.tracker.model.domain.itineraryRows
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitOnColor
import com.trimettransit.tracker.ui.theme.appCardBorder
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.util.clockTime
import org.joda.time.DateTime

/** Map strip height above the itinerary list; keeps the route visible without pushing legs offscreen. */
private val MAP_PREVIEW_HEIGHT = 180.dp

/**
 * Full-screen results view: a map preview of the selected itinerary on top, then every option
 * as a comparison card, then the selected option's totals and legs (with waits between them).
 * Replaces the planner rather than covering part of it, so the leg list gets the full height.
 */
@Composable
internal fun ItineraryResultsScreen(
    plan: TripPlan,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    ageText: String?,
    mapPreview: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val ranks = remember(plan.itineraries) { itineraryRanks(plan.itineraries) }
    val selected = plan.itineraries.getOrNull(selectedIndex) ?: plan.itineraries.firstOrNull()
    // Clear the floating nav pill, which sits over the bottom of every full-screen tab.
    val listBottomInset = navPillBottomPadding() + 8.dp
    val rows = remember(selected) { selected?.let { itineraryRows(it.legs) }.orEmpty() }

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MAP_PREVIEW_HEIGHT)
            ) {
                mapPreview()
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_to_planner),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(24.dp)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp)
            ) {
                Text(
                    text = ageText.orEmpty(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onRefresh, enabled = !isRefreshing) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(stringResource(R.string.refresh_trips))
                    }
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(
                    plan.itineraries,
                    key = { index, itinerary -> itinerary.id.ifBlank { "itinerary_$index" } }
                ) { index, itinerary ->
                    ItineraryOptionCard(
                        itinerary = itinerary,
                        rank = ranks.getOrElse(index) { ItineraryRank.OTHER },
                        optionNumber = index + 1,
                        selected = index == selectedIndex,
                        onClick = { onSelect(index) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            if (selected == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_trips_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = listBottomInset)
                ) {
                    item(key = "summary") {
                        TripSummaryHeader(
                            itinerary = selected,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    itemsIndexed(
                        rows,
                        key = { index, _ -> index },
                        contentType = { _, row -> row::class.simpleName.orEmpty() }
                    ) { _, row ->
                        when (row) {
                            is ItineraryRowItem.Leg -> LegRow(leg = row.leg)
                            is ItineraryRowItem.Wait -> WaitRow(millis = row.millis)
                        }
                    }
                }
            }
        }
    }
}

/** One comparison card in the options strip: times, duration, transfers, and route badges. */
@Composable
private fun ItineraryOptionCard(
    itinerary: TripItinerary,
    rank: ItineraryRank,
    optionNumber: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val rankLabel = when (rank) {
        ItineraryRank.FASTEST -> stringResource(R.string.itinerary_1)
        ItineraryRank.FEWEST_TRANSFERS -> stringResource(R.string.itinerary_2)
        ItineraryRank.LEAST_WALKING -> stringResource(R.string.itinerary_3)
        ItineraryRank.OTHER -> stringResource(R.string.itinerary_option, optionNumber)
    }
    Surface(
        onClick = onClick,
        shape = appCardShape(),
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = appCardBorder(),
        modifier = Modifier.width(176.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(
                text = rankLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(
                    R.string.trip_depart_arrive_fmt,
                    itinerary.departure.clockTimeText(),
                    itinerary.arrival.clockTimeText()
                ),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = formatDurationMillis(itinerary.durationMillis),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                itinerary.legs.filter { !it.isWalk }
                    .distinctBy { it.routeNumber to it.routeName }
                    .take(3)
                    .forEach { leg ->
                        RouteBadge(leg = leg, compact = true)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                Text(
                    text = pluralStringResource(
                        R.plurals.transfers_count,
                        itinerary.numberOfTransfers,
                        itinerary.numberOfTransfers
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    text = pluralStringResource(
                        R.plurals.transfers_count,
                        itinerary.numberOfTransfers,
                        itinerary.numberOfTransfers
                    ),
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
                text = if (itinerary.waitingTimeMillis > 0) {
                    stringResource(
                        R.string.walk_ride_wait_fmt,
                        formatDurationMillis(itinerary.walkTimeMillis),
                        formatDurationMillis(itinerary.transitTimeMillis),
                        formatDurationMillis(itinerary.waitingTimeMillis)
                    )
                } else {
                    stringResource(
                        R.string.walk_transit_fmt,
                        formatDurationMillis(itinerary.walkTimeMillis),
                        formatDurationMillis(itinerary.transitTimeMillis)
                    )
                },
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
    contentDescription: String? = null,
    compact: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val letter = leg.mode.transitTypeLetter()
    val badgeDescription = contentDescription
    val overrides = LocalAppearanceStyle.current.transitTypeColors
    val badgeSize: Dp = if (compact) 22.dp else 28.dp
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
                .size(badgeSize)
                .clearAndSetSemantics { }
        ) {
            Text(
                text = (leg.routeNumber?.takeIf { it.isNotEmpty() && letter == "B" }) ?: letter,
                style = if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
                fontWeight = FontWeight.Bold,
                color = transitOnColor(letter, scheme, overrides),
                maxLines = 1
            )
        }
    }
}

@Composable
internal fun LegRow(leg: TripLeg) {
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

/** A quiet divider between legs when the rider has to wait for the next departure. */
@Composable
private fun WaitRow(millis: Long) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(
                R.string.wait_minutes_fmt,
                (millis / 60_000L).coerceAtLeast(1L)
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
