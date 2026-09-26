package com.trimettransit.tracker.widget

import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.Detour
import com.trimettransit.tracker.model.Stop
import org.joda.time.DateTime
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
            Detour(id = 9, desc = "MAX disruptions", routes = emptyList()),
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

    @Test
    fun `corrupt row does not void snapshot`() {
        val rows = JSONArray()
            .put(
                JSONObject()
                    .put("locId", 1)
                    .put("name", "Stop A")
                    .put("arrivals", JSONArray().put(JSONObject().put("sign", "4").put("at", 1000L)))
            )
            .put("corrupt")
        val json = JSONObject()
            .put("hasFavorites", true)
            .put("updated", 123L)
            .put("rows", rows)
            .toString()
        val snap = WidgetSnapshotCache.parseSnapshotLenient(json)
        assertEquals(1, snap.rows.size)
        assertEquals(1, snap.rows[0].stop.locId)
    }

    @Test
    fun `empty arrival rows are kept`() {
        val rows = JSONArray()
            .put(JSONObject().put("locId", 5).put("name", "Stop E").put("arrivals", JSONArray()))
        val json = JSONObject()
            .put("hasFavorites", true)
            .put("updated", 123L)
            .put("rows", rows)
            .toString()
        val snap = WidgetSnapshotCache.parseSnapshotLenient(json)
        assertEquals(1, snap.rows.size)
        assertEquals(5, snap.rows[0].stop.locId)
    }

    @Test
    fun `cleanArrivals keeps same-time arrivals with different signs`() {
        val now = DateTime.now().millis
        val cleaned = WidgetSnapshotCache.cleanArrivals(
            listOf(
                Arrival(shortSign = "4-Division", estimatedMillis = now),
                Arrival(shortSign = "6-Martin Luther King Jr", estimatedMillis = now)
            )
        )
        assertEquals(2, cleaned.size)
    }

    @Test
    fun `saved snapshot records when it was written`() {
        val before = System.currentTimeMillis()
        val stop = Stop(desc = "Stop A", locId = 1)
        val json = WidgetSnapshotCache.snapshotToJson(
            favorites = listOf(stop),
            rows = listOf(WidgetSnapshotCache.Row(stop, emptyList()))
        )
        val loaded = WidgetSnapshotCache.parseSnapshotLenient(json)
        assertNotNull(loaded)
        assertTrue(loaded.updatedAtMillis >= before)
        assertTrue(loaded.updatedAtMillis <= System.currentTimeMillis())
    }

    @Test
    fun `write path pins the timestamp key alongside existing fields`() {
        val now = 1_700_000_000_000L
        val stop = Stop(desc = "Stop A", locId = 1)
        val loaded = WidgetSnapshotCache.parseSnapshotLenient(
            WidgetSnapshotCache.snapshotToJson(
                favorites = listOf(stop),
                rows = listOf(WidgetSnapshotCache.Row(stop, emptyList())),
                nowMillis = now
            )
        )
        assertEquals(now, loaded.updatedAtMillis)
        assertTrue(loaded.hasFavorites)
        assertEquals(1, loaded.rows.size)
        assertEquals(1, loaded.rows[0].stop.locId)
    }

    @Test
    fun `empty cache loads null, never epoch`() {
        val snap = WidgetSnapshotCache.parseSnapshotLenient("{}")
        assertEquals(0L, snap.updatedAtMillis)
        assertNull(
            snap.ageText(System.currentTimeMillis(), "Updated %d min ago", "Updated just now")
        )
    }

    @Test
    fun `age text is minutes between update and now`() {
        val now = System.currentTimeMillis()
        val json = JSONObject()
            .put("hasFavorites", true)
            .put("updated", now - 5 * 60_000L)
            .put("rows", JSONArray())
            .toString()
        val snap = WidgetSnapshotCache.parseSnapshotLenient(json)
        assertEquals(
            "Updated 5 min ago",
            snap.ageText(now, "Updated %d min ago", "Updated just now")
        )
    }

    @Test
    fun `fresh snapshot reads just now`() {
        val now = System.currentTimeMillis()
        val json = JSONObject()
            .put("hasFavorites", true)
            .put("updated", now - 10_000L)
            .put("rows", JSONArray())
            .toString()
        val snap = WidgetSnapshotCache.parseSnapshotLenient(json)
        assertEquals(
            "Updated just now",
            snap.ageText(now, "Updated %d min ago", "Updated just now")
        )
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