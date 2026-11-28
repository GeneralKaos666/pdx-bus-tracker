package com.trimettransit.tracker.feature.trips

import com.trimettransit.tracker.ui.components.rememberPressSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.TransitBadge
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitOnColor
import com.trimettransit.tracker.ui.theme.appCardBorder
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.util.clockTime
import com.trimettransit.tracker.util.formatDuration
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
    originLabel: String,
    destinationLabel: String,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    ageText: String?,
    canBrowseEarlier: Boolean,
    onEarlier: () -> Unit,
    onLater: () -> Unit,
    onSwapEndpoints: () -> Unit,
    onShare: () -> Unit,
    onLegClick: (TripLeg) -> Unit,
    mapPreview: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val ranks = remember(plan.itineraries) { itineraryRanks(plan.itineraries) }
    // Clear the floating nav pill, which sits over the bottom of every full-screen tab.
    val listBottomInset = navPillBottomPadding() + 8.dp

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(MAP_PREVIEW_HEIGHT)
            ) {
                mapPreview()
                val backSource = rememberPressSource()
                Surface(
                    onClick = onBack,
                    interactionSource = backSource,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .pressScale(backSource)
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back_to_planner),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                }
                val shareSource = rememberPressSource()
                Surface(
                    onClick = onShare,
                    interactionSource = shareSource,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .pressScale(shareSource)
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Share,
                        contentDescription = stringResource(R.string.share_trip),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(12.dp)
                            .size(24.dp)
                    )
                }
            }

            // Where the results go. The map alone is ambiguous once the sheet is reopened
            // later, and the swap control is the one-tap return trip.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp, top = 4.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }
            ) {
                Text(
                    text = stringResource(R.string.trip_depart_arrive_fmt, originLabel, destinationLabel),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                val swapSource = rememberPressSource()
                IconButton(
                    onClick = onSwapEndpoints,
                    enabled = !isRefreshing,
                    interactionSource = swapSource,
                    modifier = Modifier
                        .size(48.dp)
                        .pressScale(swapSource)
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = stringResource(R.string.swap_origin_destination),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
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
                val browses = !isRefreshing
                IconButton(
                    onClick = onEarlier,
                    enabled = browses && canBrowseEarlier,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = stringResource(R.string.earlier_trip),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onLater,
                    enabled = browses,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.later_trip),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                val refreshSource = rememberPressSource()
                TextButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    interactionSource = refreshSource,
                    modifier = Modifier.pressScale(refreshSource)
                ) {
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

            // No Crossfade around the list: it disposed the LazyColumn on every selection
            // change, resetting scroll and composing two leg lists during the transition.
            val selectedItinerary = plan.itineraries.getOrNull(selectedIndex)
                ?: plan.itineraries.firstOrNull()
            if (selectedItinerary == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_trips_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                val rows = remember(selectedItinerary) { itineraryRows(selectedItinerary.legs) }
                val listState = remember(selectedIndex) { LazyListState() }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(bottom = listBottomInset)
                ) {
                    item(key = "summary") {
                        TripSummaryHeader(
                            itinerary = selectedItinerary,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    itemsIndexed(
                        rows,
                        key = { _, row -> row.key() },
                        contentType = { _, row -> row::class.simpleName.orEmpty() }
                    ) { _, row ->
                        when (row) {
                            is ItineraryRowItem.Leg -> {
                                val boardStop = row.leg.takeUnless { it.isWalk }
                                    ?.from
                                    ?.takeIf { !it.stopId.isNullOrBlank() }
                                LegRow(
                                    leg = row.leg,
                                    onClick = boardStop?.let {
                                        { onLegClick(row.leg) }
                                    },
                                    clickLabel = boardStop?.let {
                                        stringResource(R.string.open_stop_arrivals_fmt, it.description)
                                    }
                                )
                            }
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
    val containerColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = m3EffectsDefault(),
        label = "itineraryOptionColor"
    )
    val interactionSource = rememberPressSource()
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = appCardShape(),
        color = containerColor,
        border = appCardBorder(),
        modifier = Modifier
            .widthIn(min = 176.dp)
            .semantics {
                this.selected = selected
                role = Role.Tab
            }
            .pressScale(interactionSource)
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
                text = durationText(itinerary.durationMillis),
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
                text = durationText(itinerary.durationMillis),
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
                        durationText(itinerary.walkTimeMillis),
                        durationText(itinerary.transitTimeMillis),
                        durationText(itinerary.waitingTimeMillis)
                    )
                } else {
                    stringResource(
                        R.string.walk_transit_fmt,
                        durationText(itinerary.walkTimeMillis),
                        durationText(itinerary.transitTimeMillis)
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
    val letter = leg.mode.transitTypeLetter()
    val label = (leg.routeNumber?.takeIf { it.isNotEmpty() && letter == "B" }) ?: letter
    TransitBadge(
        label = label,
        transitLetter = letter,
        modifier = modifier,
        contentDescription = contentDescription,
        compact = compact
    )
}
@Composable
internal fun LegRow(
    leg: TripLeg,
    onClick: (() -> Unit)? = null,
    clickLabel: String? = null
) {
    val interactionSource = rememberPressSource()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = LocalIndication.current,
                            role = Role.Button,
                            onClickLabel = clickLabel,
                            onClick = onClick
                        )
                        .pressScale(interactionSource)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        if (leg.isWalk) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
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
            Column(modifier = Modifier.weight(1f)) {
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
        if (onClick != null) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
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


/** Trip duration in the user's language; 24-hour locales get their own templates. */
@Composable
private fun durationText(ms: Long): String = formatDuration(
    ms,
    stringResource(R.string.duration_hours_minutes_fmt),
    stringResource(R.string.duration_minutes_fmt),
    hoursOnlyFmt = stringResource(R.string.duration_hours_fmt)
)

