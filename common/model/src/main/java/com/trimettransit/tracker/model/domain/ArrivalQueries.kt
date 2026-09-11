package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.Detour
import org.joda.time.DateTime

/**
 * Pure, side-effect-free helpers for shaping arrival data before it is rendered.
 * Lives in `common:model` (shared across phone and widget) so the funneling logic
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

/** TriMet arrival status tokens used by the API parse and the UI. */
private const val STATUS_ESTIMATED = "estimated"
private const val STATUS_CANCELED = "canceled"

/** True when the API reports a live (estimated) arrival. */
val Arrival.isEstimated: Boolean get() = status == STATUS_ESTIMATED

/** True when the API reports the arrival as canceled (see [Arrival.reason]). */
val Arrival.isCanceled: Boolean get() = status == STATUS_CANCELED

/**
 * The time the UI should display for this arrival: the live estimate when the
 * API has one, otherwise the scheduled time. Epoch millis so the shared helpers
 * stay free of joda `DateTime` construction at call sites.
 */
val Arrival.displayTimeMillis: Long
    get() {
        val fallback = scheduledMillis
        return if (isEstimated) estimatedMillis.takeIf { it > 0L } ?: fallback else fallback
    }
