package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.Detour

/**
 * Pure, side-effect-free helpers for shaping arrival data before it is rendered.
 * Lives in `common:model` (shared across phone + wear) so the funneling logic
 * (dedup, route filtering, per-line detour lookup) can be unit tested.
 *
 * Together these model the app's business rules: a stop can report the same
 * logical trip more than once (same trip/route/schedule/block/vehicle), which
 * must be collapsed to one row or the UI crashes on duplicate LazyColumn keys.
 */

/** Stable identity of a logical arrival row. */
fun arrivalKey(arrival: Arrival): String =
    "${arrival.tripID}_${arrival.routeId}_${arrival.scheduledMillis}_${arrival.blockID}_${arrival.vehicleID}"

/** Collapse duplicate logical arrivals, preserving first-seen order. */
fun dedupeArrivals(arrivals: List<Arrival>): List<Arrival> {
    val seen = HashSet<String>()
    return arrivals.filter { seen.add(arrivalKey(it)) }
}

/** Keep only arrivals on [routeId]; empty/null-safe. */
fun filterArrivalsByRoute(arrivals: List<Arrival>, routeId: Int): List<Arrival> =
    if (routeId > 0) arrivals.filter { it.routeId == routeId } else arrivals

/** Alerts that apply to a specific [routeId]. */
fun detoursForLine(detours: List<Detour>?, routeId: Int): List<Detour> =
    detours.orEmpty().filter { it.routes?.contains(routeId) == true }

/**
 * Dedupes and optionally filters arrivals in one pass — the exact funnel the
 * arrivals screen applies on each fetch.
 */
fun shapeArrivals(arrivals: List<Arrival>, routeId: Int = 0): List<Arrival> =
    filterArrivalsByRoute(dedupeArrivals(arrivals), routeId)
