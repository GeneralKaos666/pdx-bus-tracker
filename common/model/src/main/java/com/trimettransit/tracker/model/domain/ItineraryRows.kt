package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripLeg

/** Waits shorter than this are noise; the leg list only shows a wait that is worth reading. */
const val MIN_WAIT_DISPLAY_MILLIS = 60_000L

/** One row of the itinerary leg list: either a leg or the wait before the next leg. */
sealed interface ItineraryRowItem {
    data class Leg(val index: Int, val leg: TripLeg) : ItineraryRowItem
    data class Wait(val millis: Long) : ItineraryRowItem

    /**
     * Stable list key. Identity must survive a re-plan that reorders options, so a leg is
     * keyed by what it is (line, boarding stop, scheduled departure), not by the row it
     * happens to occupy. [Leg.index] is deliberately excluded.
     */
    fun key(): String = when (this) {
        is Leg -> {
            val stop = leg.from.stopId.orEmpty()
            val departure = leg.departure?.millis
            if (departure == null && stop.isEmpty()) {
                "leg:${leg.routeNumber.orEmpty()}:$index"
            } else {
                "leg:${leg.routeNumber.orEmpty()}:${departure ?: 0L}:$stop"
            }
        }
        is Wait -> "wait:$millis"
    }
}

/**
 * Flattens [legs] into leg rows with a [ItineraryRowItem.Wait] before any leg whose scheduled
 * departure leaves a gap of at least [MIN_WAIT_DISPLAY_MILLIS] after the previous arrival.
 * Walk legs carry no times, so they never produce waits.
 */
fun itineraryRows(legs: List<TripLeg>): List<ItineraryRowItem> = buildList {
    legs.forEachIndexed { index, leg ->
        waitBefore(legs.getOrNull(index - 1), leg)?.let { add(ItineraryRowItem.Wait(it)) }
        add(ItineraryRowItem.Leg(index, leg))
    }
}

/** The wait between [previous]'s arrival and [next]'s departure, or null when not meaningful. */
fun waitBefore(previous: TripLeg?, next: TripLeg): Long? {
    val arrival = previous?.arrival ?: return null
    val departure = next.departure ?: return null
    val wait = departure.millis - arrival.millis
    return wait.takeIf { it >= MIN_WAIT_DISPLAY_MILLIS }
}
