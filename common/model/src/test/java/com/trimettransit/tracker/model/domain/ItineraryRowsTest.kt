package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripLeg
import com.trimettransit.tracker.model.TripLegMode
import com.trimettransit.tracker.model.TripPoint
import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItineraryRowsTest {
    private val point = TripPoint(45.5, -122.6, "X")

    private fun leg(
        mode: TripLegMode = TripLegMode.BUS,
        departure: Long? = null,
        arrival: Long? = null
    ) = TripLeg(
        mode = mode,
        from = point,
        to = point,
        departure = departure?.let { DateTime(it) },
        arrival = arrival?.let { DateTime(it) }
    )

    @Test
    fun `first leg never gets a wait`() {
        val rows = itineraryRows(listOf(leg(departure = 0L, arrival = 600_000L)))
        assertEquals(1, rows.size)
        assertEquals(ItineraryRowItem.Leg(0, leg(departure = 0L, arrival = 600_000L)), rows.first())
    }

    @Test
    fun `a real gap inserts a wait row before the next leg`() {
        val boarding = leg(departure = 0L, arrival = 600_000L)
        val transfer = leg(departure = 1_200_000L, arrival = 1_800_000L)
        val rows = itineraryRows(listOf(boarding, transfer))
        assertEquals(
            listOf(
                ItineraryRowItem.Leg(0, boarding),
                ItineraryRowItem.Wait(600_000L, afterLegIndex = 1),
                ItineraryRowItem.Leg(1, transfer)
            ),
            rows
        )
    }

    @Test
    fun `a gap under a minute is noise`() {
        val first = leg(departure = 0L, arrival = 600_000L)
        val second = leg(departure = 630_000L, arrival = 900_000L)
        assertEquals(2, itineraryRows(listOf(first, second)).size)
    }

    @Test
    fun `walk legs without times never produce waits`() {
        val ride = leg(departure = 0L, arrival = 600_000L)
        val walk = leg(mode = TripLegMode.WALK)
        assertEquals(2, itineraryRows(listOf(ride, walk)).size)
        assertNull(waitBefore(ride, walk))
    }

    @Test
    fun `negative gap is dropped`() {
        val first = leg(departure = 0L, arrival = 600_000L)
        val second = leg(departure = 500_000L, arrival = 900_000L)
        assertNull(waitBefore(first, second))
    }

    @Test
    fun `missing previous leg has no wait`() {
        assertNull(waitBefore(null, leg(departure = 0L, arrival = 1L)))
    }

    @Test
    fun `leg keys ignore the row position so replans keep identity`() {
        val leg = TripLeg(
            mode = TripLegMode.BUS,
            routeNumber = "12",
            from = TripPoint(45.5, -122.6, "A", stopId = "8337"),
            to = TripPoint(45.6, -122.7, "B", stopId = "8340"),
            departure = DateTime(2026, 11, 27, 9, 0),
            arrival = DateTime(2026, 11, 27, 9, 20)
        )
        assertEquals(
            ItineraryRowItem.Leg(0, leg).key(),
            ItineraryRowItem.Leg(3, leg).key()
        )
    }

    @Test
    fun `wait keys include the duration`() {
        assertEquals("wait:0:120000", ItineraryRowItem.Wait(120_000).key())
    }

    @Test
    fun `two equal waits in one itinerary get distinct keys`() {
        val legs = listOf(
            leg(departure = 0L, arrival = 600_000L),
            leg(departure = 1_200_000L, arrival = 1_800_000L),
            leg(departure = 2_400_000L, arrival = 3_000_000L)
        )
        val keys = itineraryRows(legs).filterIsInstance<ItineraryRowItem.Wait>().map { it.key() }
        assertEquals(2, keys.size)
        assertEquals(keys.size, keys.distinct().size)
    }
}
