package com.trimettransit.tracker.notifications

import com.trimettransit.tracker.model.Arrival
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DepartureAlertRulesTest {

    private val zone: ZoneId = ZoneId.of("America/Los_Angeles")

    private fun millisAt(hour: Int, minute: Int, second: Int = 0): Long =
        LocalDateTime.of(2026, 9, 11, hour, minute, second)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

    private fun arrivalWith(
        estimated: Long = 0,
        scheduled: Long = 0,
        dropOffOnly: Boolean = false,
        tripID: String = "trial"
    ) =
        Arrival(
            estimatedMillis = estimated,
            scheduledMillis = scheduled,
            dropOffOnly = dropOffOnly,
            fullSign = "20-Burnside/Stark",
            tripID = tripID
        )

    @Test
    fun `minutesUntil prefers estimated over scheduled`() {
        val now = millisAt(12, 0)
        val arrival = arrivalWith(
            estimated = now + 5 * 60_000,
            scheduled = now + 6 * 60_000
        )
        assertEquals(5L, DepartureAlertRules.minutesUntil(arrival, now))
    }

    @Test
    fun `minutesUntil falls back to scheduled when estimated is unset`() {
        val now = millisAt(12, 0)
        val arrival = arrivalWith(scheduled = now + 6 * 60_000)
        assertEquals(6L, DepartureAlertRules.minutesUntil(arrival, now))
    }

    @Test
    fun `minutesUntil is null when neither time is set`() {
        assertNull(DepartureAlertRules.minutesUntil(arrivalWith(), millisAt(12, 0)))
    }

    @Test
    fun `quiet hours cover late night through just before five`() {
        assertTrue(DepartureAlertRules.isQuietHours(millisAt(23, 30), zone))
        assertTrue(DepartureAlertRules.isQuietHours(millisAt(0, 30), zone))
        assertTrue(DepartureAlertRules.isQuietHours(millisAt(4, 59), zone))
    }

    @Test
    fun `quiet hours end at five am and resume after eleven pm`() {
        assertFalse(DepartureAlertRules.isQuietHours(millisAt(5, 0), zone))
        assertFalse(DepartureAlertRules.isQuietHours(millisAt(12, 0), zone))
        assertFalse(DepartureAlertRules.isQuietHours(millisAt(22, 59), zone))
    }

    @Test
    fun `actionable arrivals are within the window on the future side only`() {
        val now = millisAt(12, 0)
        val onTime = arrivalWith(estimated = now + 10 * 60_000)
        val inPast = arrivalWith(estimated = now - 60_000)
        val late = arrivalWith(estimated = now + 11 * 60_000)
        val dropOff = arrivalWith(estimated = now + 5 * 60_000, dropOffOnly = true)

        val actionable = DepartureAlertRules.actionableArrivals(
            listOf(onTime, inPast, late, dropOff),
            now,
            windowMinutes = 10
        )

        assertEquals(listOf(onTime), actionable)
    }

    @Test
    fun `boundary arrivals exactly at the window edge are actionable`() {
        val now = millisAt(12, 0)
        val atEdge = arrivalWith(estimated = now + 10 * 60_000)
        assertTrue(
            DepartureAlertRules.actionableArrivals(listOf(atEdge), now, windowMinutes = 10)
                .contains(atEdge)
        )
    }

    @Test
    fun `filterNew drops already-fired departures`() {
        val now = millisAt(12, 0)
        val first = arrivalWith(estimated = now + 5 * 60_000, tripID = "trip-1")
        val second = arrivalWith(estimated = now + 6 * 60_000, tripID = "trip-2")

        val fresh = DepartureAlertRules.filterNew(
            listOf(first, second),
            fired = setOf(DepartureAlertRules.firedKey(0, "trip-1")),
            locId = 0
        )

        assertEquals(listOf(second), fresh)
    }

    @Test
    fun `prune caps the fired set and leaves small sets untouched`() {
        val small = setOf("a", "b")
        assertEquals(small, DepartureAlertRules.prune(small))

        val big = (0 until 150).mapIndexed { i, _ -> "key-$i" }.toSet()
        val pruned = DepartureAlertRules.prune(big)
        assertEquals(100, pruned.size)
        assertTrue(pruned.contains("key-149"))
    }

    @Test
    fun `fired key round-trips stop and trip ids`() {
        assertEquals("7783:some-trip", DepartureAlertRules.firedKey(7783, "some-trip"))
    }
}