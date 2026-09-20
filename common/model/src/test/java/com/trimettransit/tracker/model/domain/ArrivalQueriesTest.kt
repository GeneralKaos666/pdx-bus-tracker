package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.TransitAlert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalQueriesTest {

    private fun arrival(
        tripID: String = "11269782",
        routeId: Int = 12,
        scheduledMillis: Long = 1_000L,
        blockID: Int = 5002,
        vehicleID: Int = 3518,
        status: String = "estimated",
        estimatedMillis: Long = 0L
    ) = Arrival(
        tripID = tripID,
        routeId = routeId,
        scheduledMillis = scheduledMillis,
        blockID = blockID,
        vehicleID = vehicleID,
        status = status,
        estimatedMillis = estimatedMillis
    )

    // arrivalKey

    @Test
    fun `arrivalKey is stable for identical logical arrivals`() {
        val a = arrival()
        val b = arrival()
        assertEquals(arrivalKey(a), arrivalKey(b))
    }

    @Test
    fun `arrivalKey differentiates every identity field`() {
        val base = arrival()
        assertNotEquals(arrivalKey(base), arrivalKey(arrival(tripID = "other")))
        assertNotEquals(arrivalKey(base), arrivalKey(arrival(routeId = 19)))
        assertNotEquals(arrivalKey(base), arrivalKey(arrival(scheduledMillis = 2_000L)))
        assertNotEquals(arrivalKey(base), arrivalKey(arrival(blockID = 6001)))
        assertNotEquals(arrivalKey(base), arrivalKey(arrival(vehicleID = 12)))
    }

    @Test
    fun `arrivalKey differentiates stops`() {
        val base = arrival().copy(locId = 1)
        val other = base.copy(locId = 2)
        assertNotEquals(arrivalKey(base), arrivalKey(other))
    }

    // dedupeArrivals

    @Test
    fun `dedupeArrivals collapses duplicates preserving first-seen order`() {
        val a = arrival()
        val anotherA = arrival()
        val b = arrival(tripID = "other")
        val result = dedupeArrivals(listOf(a, b, anotherA))
        assertEquals(listOf(a, b), result)
    }

    @Test
    fun `dedupeArrivals leaves unique arrivals untouched`() {
        val a = arrival()
        val b = arrival(tripID = "other")
        assertEquals(listOf(a, b), dedupeArrivals(listOf(a, b)))
    }

    @Test
    fun `dedupeArrivals is empty-safe`() {
        assertTrue(dedupeArrivals(emptyList()).isEmpty())
    }

    // filterArrivalsByRoute

    @Test
    fun `filterArrivalsByRoute passes through on routeId zero or negative`() {
        val arrivals = listOf(arrival(), arrival(routeId = 19))
        assertEquals(arrivals, filterArrivalsByRoute(arrivals, 0))
        assertEquals(arrivals, filterArrivalsByRoute(arrivals, -1))
    }

    @Test
    fun `filterArrivalsByRoute keeps only the requested route`() {
        val expected = arrival()
        val result = filterArrivalsByRoute(listOf(expected, arrival(routeId = 19)), 12)
        assertEquals(listOf(expected), result)
    }

    @Test
    fun `filterArrivalsByRoute returns empty when nothing matches`() {
        assertTrue(filterArrivalsByRoute(listOf(arrival(routeId = 19)), 12).isEmpty())
    }

    // detoursForLine

    @Test
    fun `detoursForLine keeps detours whose routes contain the line`() {
        val match = Detour(id = 1, desc = "Detour", routes = listOf(12, 9))
        assertEquals(listOf(match), detoursForLine(listOf(match), 12))
    }

    @Test
    fun `detoursForLine filters lines not affected`() {
        val other = Detour(id = 2, desc = "Other", routes = listOf(19))
        assertTrue(detoursForLine(listOf(other), 12).isEmpty())
    }

    @Test
    fun `detoursForLine hides elements with null or empty routes`() {
        val nullRoutes = Detour(id = 3, desc = "Null", routes = emptyList())
        val emptyRoutes = Detour(id = 4, desc = "Empty", routes = emptyList())
        val match = Detour(id = 1, desc = "Match", routes = listOf(12))
        assertEquals(listOf(match), detoursForLine(listOf(nullRoutes, emptyRoutes, match), 12))
    }

    @Test
    fun `detoursForLine is null-safe for the whole list`() {
        assertTrue(detoursForLine(null, 12).isEmpty())
    }

    // alertsForLine

    @Test
    fun `alertsForLine keeps alerts whose routeIds contain the line`() {
        val match = TransitAlert(id = 1, desc = "Alert", routeIds = listOf(12, 9))
        assertEquals(listOf(match), alertsForLine(listOf(match), 12))
    }

    @Test
    fun `alertsForLine filters lines not affected`() {
        val other = TransitAlert(id = 2, desc = "Other", routeIds = listOf(19))
        assertTrue(alertsForLine(listOf(other), 12).isEmpty())
    }

    @Test
    fun `alertsForLine hides elements with empty routeIds even when system-wide`() {
        val systemWide = TransitAlert(id = 3, desc = "System-wide", systemWide = true, routeIds = emptyList())
        val match = TransitAlert(id = 1, desc = "Match", routeIds = listOf(12))
        assertEquals(listOf(match), alertsForLine(listOf(systemWide, match), 12))
    }

    @Test
    fun `alertsForLine is null-safe for the whole list`() {
        assertTrue(alertsForLine(null, 12).isEmpty())
    }

    @Test
    fun `TransitAlert displayTitle falls back to desc when header is blank`() {
        assertEquals("Desc only", TransitAlert(header = "", desc = "Desc only").displayTitle)
        assertEquals("Header", TransitAlert(header = "Header", desc = "Desc").displayTitle)
    }

    // status + display time

    @Test
    fun `isEstimated and isCanceled follow the status token`() {
        assertTrue(arrival(status = "estimated").isEstimated)
        assertTrue(arrival(status = "canceled").isCanceled)
        assertFalse(arrival(status = "scheduled").isEstimated)
        assertFalse(arrival(status = "scheduled").isCanceled)
        assertFalse(arrival(status = "").isEstimated)
    }

    @Test
    fun `displayTimeMillis prefers a positive live estimate`() {
        val live = arrival(status = "estimated", estimatedMillis = 5_000L, scheduledMillis = 1_000L)
        assertEquals(5_000L, live.displayTimeMillis)
    }

    @Test
    fun `displayTimeMillis falls back to scheduled for estimated with no estimate`() {
        val noEstimate = arrival(status = "estimated", estimatedMillis = 0L, scheduledMillis = 1_000L)
        assertEquals(1_000L, noEstimate.displayTimeMillis)
    }

    @Test
    fun `displayTimeMillis falls back to scheduled for non-estimated`() {
        val scheduled = arrival(status = "scheduled", estimatedMillis = 5_000L, scheduledMillis = 1_000L)
        assertEquals(1_000L, scheduled.displayTimeMillis)
    }
}