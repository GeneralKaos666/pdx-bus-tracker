package com.trimettransit.tracker.widget

import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.Detour
import org.joda.time.DateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetSnapshotCacheTest {

    @Test
    fun `cleanArrivals sorts by time and caps at four`() {
        val now = DateTime.now().millis
        val arrivals = listOf(
            arrival(now + 3_000L),
            arrival(now + 1_000L),
            arrival(now + 2_000L),
            arrival(now + 5_000L),
            arrival(now + 4_000L),
            arrival(now + 6_000L)
        )
        val cleaned = WidgetSnapshotCache.cleanArrivals(arrivals)
        assertEquals(4, cleaned.size)
        assertEquals(
            listOf(1_000L, 2_000L, 3_000L, 4_000L),
            cleaned.map { it.atMillis - now }
        )
    }

    @Test
    fun `cleanArrivals drops arrivals without a usable time`() {
        val now = DateTime.now().millis
        val cleaned = WidgetSnapshotCache.cleanArrivals(
            listOf(
                Arrival(estimatedMillis = 0, scheduledMillis = 0),
                arrival(now)
            )
        )
        assertEquals(1, cleaned.size)
        assertEquals(now, cleaned.first().atMillis)
    }

    @Test
    fun `cleanArrivals dedupes identical times`() {
        val now = DateTime.now().millis
        val cleaned = WidgetSnapshotCache.cleanArrivals(
            listOf(arrival(now), arrival(now))
        )
        assertEquals(1, cleaned.size)
    }

    @Test
    fun `cleanArrivals preserves canceled status and prefers estimated time`() {
        val now = DateTime.now().millis
        val canceled = Arrival(
            shortSign = "4-Division",
            estimatedMillis = now,
            scheduledMillis = now + 60_000L,
            status = "canceled",
            dropOffOnly = false
        )
        val cleaned = WidgetSnapshotCache.cleanArrivals(listOf(canceled))
        assertEquals("4-Division", cleaned.first().sign)
        assertEquals(now, cleaned.first().atMillis)
        assertEquals("canceled", cleaned.first().status)
        assertFalse(cleaned.first().dropOffOnly)
    }

    @Test
    fun `cleanArrivals prefers estimated over scheduled and keeps dropOffOnly`() {
        val now = DateTime.now().millis
        val cleaned = WidgetSnapshotCache.cleanArrivals(
            listOf(
                Arrival(
                    shortSign = "12-Barrington",
                    estimatedMillis = now,
                    scheduledMillis = now + 120_000L,
                    dropOffOnly = true
                )
            )
        )
        assertEquals(now, cleaned.first().atMillis)
        assertTrue(cleaned.first().dropOffOnly)
    }

    @Test
    fun `dedupeDetours keeps the first entry per id and maps routes to routeIds`() {
        val detours = listOf(
            Detour(id = 7, desc = "Closure on 5th", routes = listOf(4, 17)),
            Detour(id = 7, desc = "Duplicate closure", routes = listOf(4)),
            Detour(id = 9, desc = "MAX disruptions", routes = null),
            Detour(id = 0, desc = "No id, kept once", routes = listOf(1))
        )
        val result = WidgetSnapshotCache.dedupeDetours(detours)
        assertEquals(3, result.size)
        assertEquals(7, result[0].id)
        assertEquals("Closure on 5th", result[0].desc)
        assertEquals(listOf(4, 17), result[0].routeIds)
        assertEquals(9, result[1].id)
        assertEquals(emptyList<Int>(), result[1].routeIds)
    }

    @Test
    fun `dedupeDetours handles empty input`() {
        assertEquals(0, WidgetSnapshotCache.dedupeDetours(emptyList()).size)
    }

    private fun arrival(atMillis: Long) = Arrival(
        shortSign = "4-Division",
        estimatedMillis = atMillis
    )

    private fun assertTrue(value: Boolean) {
        org.junit.Assert.assertTrue(value)
    }

    private fun assertFalse(value: Boolean) {
        org.junit.Assert.assertFalse(value)
    }
}