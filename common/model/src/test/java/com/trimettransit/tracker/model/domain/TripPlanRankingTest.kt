package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripItinerary
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks itinerary ranking. The critical property is that the returned list is index-aligned
 * with the input (the chip row and the map both address itineraries by index), and that no
 * two itineraries can ever receive the same non-OTHER label.
 */
class TripPlanRankingTest {

    private fun itinerary(
        durationMillis: Long,
        transfers: Int,
        walkMillis: Long
    ) = TripItinerary(
        departure = null,
        arrival = null,
        durationMillis = durationMillis,
        distanceMeters = 0.0,
        numberOfTransfers = transfers,
        walkTimeMillis = walkMillis,
        transitTimeMillis = 0L,
        waitingTimeMillis = 0L
    )

    @Test
    fun `empty list yields empty ranks`() {
        assertEquals(emptyList<ItineraryRank>(), itineraryRanks(emptyList()))
    }

    @Test
    fun `single itinerary is fastest`() {
        assertEquals(
            listOf(ItineraryRank.FASTEST),
            itineraryRanks(listOf(itinerary(durationMillis = 30_000L, transfers = 0, walkMillis = 100L)))
        )
    }

    @Test
    fun `distinct bests get the three named ranks`() {
        val ranks = itineraryRanks(
            listOf(
                itinerary(durationMillis = 20_000L, transfers = 1, walkMillis = 500L),
                itinerary(durationMillis = 40_000L, transfers = 0, walkMillis = 400L),
                itinerary(durationMillis = 50_000L, transfers = 2, walkMillis = 100L)
            )
        )
        assertEquals(
            listOf(ItineraryRank.FASTEST, ItineraryRank.FEWEST_TRANSFERS, ItineraryRank.LEAST_WALKING),
            ranks
        )
    }

    @Test
    fun `identical itineraries produce exactly one fastest and the rest other`() {
        val ranks = itineraryRanks(
            List(6) { itinerary(durationMillis = 30_000L, transfers = 1, walkMillis = 200L) }
        )
        assertEquals(listOf(ItineraryRank.FASTEST), ranks.filter { it == ItineraryRank.FASTEST })
        assertEquals(5, ranks.count { it == ItineraryRank.OTHER })
    }

    @Test
    fun `no transfer label when the fastest already has the fewest transfers`() {
        val ranks = itineraryRanks(
            listOf(
                itinerary(durationMillis = 20_000L, transfers = 0, walkMillis = 100L),
                itinerary(durationMillis = 40_000L, transfers = 0, walkMillis = 300L)
            )
        )
        assertEquals(listOf(ItineraryRank.FASTEST, ItineraryRank.OTHER), ranks)
    }

    @Test
    fun `walk label is suppressed when the fastest walks the least`() {
        val ranks = itineraryRanks(
            listOf(
                itinerary(durationMillis = 20_000L, transfers = 0, walkMillis = 100L),
                itinerary(durationMillis = 40_000L, transfers = 2, walkMillis = 900L)
            )
        )
        assertEquals(listOf(ItineraryRank.FASTEST, ItineraryRank.OTHER), ranks)
    }

    @Test
    fun `ranks stay index aligned with the input order`() {
        val ranks = itineraryRanks(
            listOf(
                itinerary(durationMillis = 90_000L, transfers = 3, walkMillis = 900L),
                itinerary(durationMillis = 10_000L, transfers = 2, walkMillis = 800L),
                itinerary(durationMillis = 50_000L, transfers = 0, walkMillis = 700L)
            )
        )
        assertEquals(3, ranks.size)
        assertEquals(ItineraryRank.FASTEST, ranks[1])
        assertEquals(ItineraryRank.FEWEST_TRANSFERS, ranks[2])
    }

    // --- selection after a plan request (final-review fix) ---

    @Test
    fun `a fresh request starts at the best option`() {
        assertEquals(
            0,
            itinerarySelectionAfterPlan(currentIndex = 3, itineraryCount = 6, reset = true)
        )
    }

    @Test
    fun `a restore keeps the user's chosen option`() {
        assertEquals(
            3,
            itinerarySelectionAfterPlan(currentIndex = 3, itineraryCount = 6, reset = false)
        )
    }

    @Test
    fun `a restore clamps when the new option list is shorter`() {
        assertEquals(
            1,
            itinerarySelectionAfterPlan(currentIndex = 5, itineraryCount = 2, reset = false)
        )
    }

    @Test
    fun `a restore clamps a negative index to the first option`() {
        assertEquals(
            0,
            itinerarySelectionAfterPlan(currentIndex = -1, itineraryCount = 3, reset = false)
        )
    }

    @Test
    fun `an empty option list selects index zero`() {
        assertEquals(
            0,
            itinerarySelectionAfterPlan(currentIndex = 4, itineraryCount = 0, reset = false)
        )
        assertEquals(
            0,
            itinerarySelectionAfterPlan(currentIndex = 4, itineraryCount = 0, reset = true)
        )
    }
}
