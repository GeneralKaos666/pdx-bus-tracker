package com.trimettransit.tracker.feature.trips

import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripLeg
import com.trimettransit.tracker.model.TripLegMode
import com.trimettransit.tracker.model.TripPoint
import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class TripShareTextTest {
    private val origin = TripPoint(45.5, -122.6, "SE 82nd & Schiller")
    private val destination = TripPoint(45.6, -122.7, "SE Division & 12th")

    @Test
    fun `share text lists endpoints, date, times, duration, and routes in order`() {
        val itinerary = TripItinerary(
            departure = DateTime(2026, 11, 28, 16, 30),
            arrival = DateTime(2026, 11, 28, 17, 2),
            durationMillis = 32 * 60_000L,
            distanceMeters = 8_000.0,
            numberOfTransfers = 1,
            walkTimeMillis = 4 * 60_000L,
            transitTimeMillis = 21 * 60_000L,
            waitingTimeMillis = 11 * 60_000L,
            legs = listOf(
                walkLeg(),
                transitLeg("72"),
                transitLeg("72"),
                transitLeg("2")
            )
        )

        val text = buildTripShareText(
            originLabel = origin.description,
            destinationLabel = destination.description,
            dateLabel = "Sat, Nov 28",
            itinerary = itinerary
        )

        assertEquals(
            "SE 82nd & Schiller → SE Division & 12th\n" +
                "Sat, Nov 28\n" +
                "4:30 PM – 5:02 PM (32m)\n" +
                "72 → 2",
            text
        )
    }

    @Test
    fun `share text omits a blank date and a walk-only route line`() {
        val itinerary = TripItinerary(
            departure = null,
            arrival = null,
            durationMillis = 8 * 60_000L,
            distanceMeters = 700.0,
            numberOfTransfers = 0,
            walkTimeMillis = 8 * 60_000L,
            transitTimeMillis = 0L,
            waitingTimeMillis = 0L,
            legs = listOf(walkLeg())
        )

        val text = buildTripShareText(
            originLabel = "A",
            destinationLabel = "B",
            dateLabel = "",
            itinerary = itinerary
        )

        assertEquals("A → B\n— – — (8m)", text)
    }

    private fun walkLeg() = TripLeg(
        mode = TripLegMode.WALK,
        from = origin,
        to = destination,
        departure = null,
        arrival = null
    )

    private fun transitLeg(number: String) = TripLeg(
        mode = TripLegMode.BUS,
        routeNumber = number,
        routeName = "Route $number",
        from = origin,
        to = destination,
        departure = DateTime(2026, 11, 28, 16, 30),
        arrival = DateTime(2026, 11, 28, 16, 45)
    )
}
