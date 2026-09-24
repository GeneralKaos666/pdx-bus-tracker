package com.trimettransit.tracker.feature.trips

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripPlannerError
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.domain.matchDirection
import com.trimettransit.tracker.model.domain.sliceStopsForLeg
import com.trimettransit.tracker.model.repository.TransitRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3EffectsFast

@Composable
internal fun EndpointRow(
    label: String,
    point: TripPoint?,
    accentColor: Color,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = source,
        shape = appCardShape(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .pressScale(source)
            .semantics { role = Role.Button }
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(accentColor, appCardShape())
            )
            Spacer(modifier = Modifier.width(10.dp))
            Crossfade(
                targetState = point,
                animationSpec = m3EffectsDefault(),
                label = "endpointContent",
                modifier = Modifier.weight(1f)
            ) { currentPoint ->
                Text(
                    text = currentPoint?.description?.takeIf { it.isNotBlank() } ?: label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (currentPoint != null) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    // The visible text is the place name once a point is set, so the field's role
                    // has to be spoken rather than seen. Set on this Text only: putting it on the
                    // row would swallow the clear button.
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = currentPoint?.description
                                ?.takeIf { it.isNotBlank() }
                                ?.let { "$label, $it" }
                                ?: label
                        }
                )
            }
            AnimatedVisibility(
                visible = point != null,
                enter = fadeIn(m3EffectsDefault()),
                exit = fadeOut(m3EffectsFast())
            ) {
                IconButton(onClick = onClear, modifier = Modifier.size(48.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

internal fun tripPlannerErrorString(context: Context, error: TripPlannerError): String {
    return when (error) {
        TripPlannerError.NO_STOPS_NEAR_ORIGIN,
        TripPlannerError.NO_STOPS_NEAR_DESTINATION -> context.getString(R.string.trip_planner_error_no_stops)
        TripPlannerError.NO_SERVICE_AT_ORIGIN,
        TripPlannerError.NO_SERVICE_AT_DESTINATION -> context.getString(R.string.trip_planner_error_no_service)
        TripPlannerError.TRIP_NOT_POSSIBLE -> context.getString(R.string.trip_planner_error_not_possible)
        TripPlannerError.TRIVIAL_DISTANCE -> context.getString(R.string.trip_planner_error_trivial)
        TripPlannerError.AMBIGUOUS_ORIGIN -> context.getString(R.string.trip_planner_error_ambiguous_origin)
        TripPlannerError.AMBIGUOUS_DESTINATION -> context.getString(R.string.trip_planner_error_ambiguous_destination)
        TripPlannerError.ORIGIN_NOT_FOUND,
        TripPlannerError.DESTINATION_NOT_FOUND -> context.getString(R.string.trip_planner_error_not_found)
        TripPlannerError.OUTSIDE_DISTRICT -> context.getString(R.string.trip_planner_error_outside_district)
        TripPlannerError.SYSTEM_OUTAGE -> context.getString(R.string.trip_planner_error_outage)
        TripPlannerError.NETWORK -> context.getString(R.string.no_connection)
        TripPlannerError.UNKNOWN -> context.getString(R.string.trip_planner_error_unknown)
    }
}

/**
 * Resolves the itinerary's transit legs into real route geometry. The Trip Planner WS
 * returns no geometry, so each non-walk leg with a numeric route number is matched to its
 * route config direction (via the leg's compass label) and the stop sequence is sliced
 * between the boarding and alighting points. Legs that can't be resolved contribute no
 * entry and the map keeps its straight stick line. Directions and stop sequences are
 * memoized per (route, direction) so multi-leg or repeated plans avoid re-fetching.
 * Distinct routes/stop-sequences fetch in parallel (one request per distinct key);
 * slicing stays sequential pure CPU work over the cached sequences.
 */
internal suspend fun fetchLegGeometries(
    transitRepository: TransitRepository,
    itinerary: TripItinerary?,
    directionsCache: MutableState<Map<Int, List<Direction>>>,
    stopsCache: MutableState<Map<Pair<Int, Int>, List<Stop>>>
): Map<Int, List<GeoPoint>> = coroutineScope {
    val result = mutableMapOf<Int, List<GeoPoint>>()
    val plan = itinerary ?: return@coroutineScope result
    // 1. Distinct routes needed by non-walk legs; fetch missing directions in parallel.
    val wantedRoutes = plan.legs.mapNotNull { leg ->
        if (leg.isWalk) null else leg.routeNumber?.toIntOrNull()
    }.distinct().filterNot { directionsCache.value.containsKey(it) }
    if (wantedRoutes.isNotEmpty()) {
        val fetched = wantedRoutes.map { routeId ->
            async { routeId to transitRepository.getDirections(routeId) }
        }.awaitAll()
        var merged = directionsCache.value
        for ((routeId, directions) in fetched) {
            if (directions != null) merged = merged + (routeId to directions)
        }
        directionsCache.value = merged
    }
    // 2. Resolve each leg's direction against the now-complete cache.
    val directionsSnapshot = directionsCache.value
    val legKeys = mutableMapOf<Int, Pair<Int, Int>>()
    for ((legIndex, leg) in plan.legs.withIndex()) {
        if (leg.isWalk) continue
        val routeId = leg.routeNumber?.toIntOrNull() ?: continue
        val directions = directionsSnapshot[routeId] ?: continue
        val direction = matchDirection(directions, leg.direction) ?: continue
        legKeys[legIndex] = routeId to direction.dir
    }
    // 3. Fetch missing stop sequences in parallel (distinct keys only).
    val missingKeys = legKeys.values.distinct().filterNot { stopsCache.value.containsKey(it) }
    if (missingKeys.isNotEmpty()) {
        val fetchedStops = missingKeys.map { key ->
            async { key to transitRepository.getStops(key.first, key.second) }
        }.awaitAll()
        var mergedStops = stopsCache.value
        for ((key, stops) in fetchedStops) {
            if (stops != null) mergedStops = mergedStops + (key to stops)
        }
        stopsCache.value = mergedStops
    }
    // 4. Slice sequentially over the cached sequences.
    val stopsSnapshot = stopsCache.value
    for ((legIndex, key) in legKeys) {
        val leg = plan.legs[legIndex]
        val sequence = stopsSnapshot[key] ?: continue
        val slice = sliceStopsForLeg(
            sequence,
            leg.from.latitude, leg.from.longitude,
            leg.to.latitude, leg.to.longitude
        )
        if (slice.size < 2) continue
        result[legIndex] = slice.map { GeoPoint(it.latitude, it.longitude) }
    }
    result
}