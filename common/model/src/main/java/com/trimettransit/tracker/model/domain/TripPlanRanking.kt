package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripItinerary

/** Why an itinerary chip carries the name it does. */
enum class ItineraryRank { FASTEST, FEWEST_TRANSFERS, LEAST_WALKING, OTHER }

/**
 * Names each itinerary by merit **without reordering** [itineraries], so the returned list is
 * index-aligned with the input — the chip row, the selected index, and the drawn map geometry
 * all address itineraries by index, and reordering would silently desynchronise them.
 *
 * - FASTEST: smallest [TripItinerary.durationMillis]; the first minimum wins ties.
 * - FEWEST_TRANSFERS: smallest [TripItinerary.numberOfTransfers] among the rest, only when it
 *   is strictly fewer than the FASTEST pick's.
 * - LEAST_WALKING: smallest [TripItinerary.walkTimeMillis] among the rest, only when it is
 *   strictly less than every already-named pick.
 * - OTHER: everything else (the UI renders these as "Option N").
 */
fun itineraryRanks(itineraries: List<TripItinerary>): List<ItineraryRank> {
    if (itineraries.isEmpty()) return emptyList()
    val ranks = MutableList(itineraries.size) { ItineraryRank.OTHER }
    val named = mutableSetOf<Int>()

    val fastest = itineraries.indices.minByOrNull { itineraries[it].durationMillis }
    if (fastest != null) {
        ranks[fastest] = ItineraryRank.FASTEST
        named += fastest
    }

    val transfersCandidate = itineraries.indices
        .filterNot { it in named }
        .minByOrNull { itineraries[it].numberOfTransfers }
    if (fastest != null && transfersCandidate != null &&
        itineraries[transfersCandidate].numberOfTransfers < itineraries[fastest].numberOfTransfers
    ) {
        ranks[transfersCandidate] = ItineraryRank.FEWEST_TRANSFERS
        named += transfersCandidate
    }

    val walkCandidate = itineraries.indices
        .filterNot { it in named }
        .minByOrNull { itineraries[it].walkTimeMillis }
    if (walkCandidate != null &&
        named.all { itineraries[walkCandidate].walkTimeMillis < itineraries[it].walkTimeMillis }
    ) {
        ranks[walkCandidate] = ItineraryRank.LEAST_WALKING
    }

    return ranks
}

/**
 * The itinerary index to select once a plan request returns.
 *
 * A brand-new request (the user pressed "Find trips") starts at the best option, so [reset] is
 * true. A restore or re-entry request re-fetches the *same* endpoints, so the previous choice is
 * still meaningful and must survive — [reset] false keeps [currentIndex], clamped in case the
 * returned option list is shorter than the one the index came from.
 */
fun itinerarySelectionAfterPlan(
    currentIndex: Int,
    itineraryCount: Int,
    reset: Boolean
): Int = when {
    itineraryCount <= 0 -> 0
    reset -> 0
    else -> currentIndex.coerceIn(0, itineraryCount - 1)
}
