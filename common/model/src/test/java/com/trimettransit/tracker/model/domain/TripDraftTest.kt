package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TripDraftTest {
    private val now = 1_700_000_000_000L
    private val origin = TripPoint(latitude = 45.5152, longitude = -122.6784, description = "A")
    private val destination = TripPoint(latitude = 45.5051, longitude = -122.6750, description = "B")

    @Test
    fun `future arrive-by is submittable`() {
        val draft = TripDraft(origin = origin, destination = destination, arriveByMillis = now + 3_600_000L)
        assertTrue(draft.isSubmittable(now))
    }

    @Test
    fun `past arrive-by is stale and not submittable`() {
        val draft = TripDraft(origin = origin, destination = destination, arriveByMillis = now - 60_000L)
        assertTrue(draft.isArriveByStale(now))
        assertFalse(draft.isSubmittable(now))
    }

    @Test
    fun `arrive-by exactly now is stale, never sent`() {
        val draft = TripDraft(origin = origin, destination = destination, arriveByMillis = now)
        assertTrue(draft.isArriveByStale(now))
        assertFalse(draft.isSubmittable(now))
    }

    @Test
    fun `depart-now draft with both endpoints is submittable`() {
        val draft = TripDraft(origin = origin, destination = destination)
        assertFalse(draft.isArriveByStale(now))
        assertFalse(draft.isDepartAtStale(now))
        assertTrue(draft.isSubmittable(now))
    }

    @Test
    fun `future depart-at is submittable`() {
        val draft = TripDraft(origin = origin, destination = destination, departAtMillis = now + 60_000L)
        assertFalse(draft.isDepartAtStale(now))
        assertTrue(draft.isSubmittable(now))
    }

    @Test
    fun `past depart-at is stale and not submittable`() {
        val draft = TripDraft(origin = origin, destination = destination, departAtMillis = now - 60_000L)
        assertTrue(draft.isDepartAtStale(now))
        assertFalse(draft.isSubmittable(now))
    }

    @Test
    fun `depart-at exactly now is stale, never sent`() {
        val draft = TripDraft(origin = origin, destination = destination, departAtMillis = now)
        assertTrue(draft.isDepartAtStale(now))
        assertFalse(draft.isSubmittable(now))
    }

    @Test
    fun `draft missing an endpoint is not submittable`() {
        assertFalse(TripDraft(origin = null, destination = destination).isSubmittable(now))
        assertFalse(TripDraft(origin = origin, destination = null).isSubmittable(now))
    }

    @Test
    fun `age text is minutes between planned-at and now`() {
        val draft = TripDraft(origin = origin, destination = destination, plannedAtMillis = now - 5 * 60_000L)
        assertEquals("Updated 5 min ago", draft.ageText(now, "Updated %d min ago", "Updated just now"))
    }

    @Test
    fun `fresh plan reads just now`() {
        val draft = TripDraft(origin = origin, destination = destination, plannedAtMillis = now - 10_000L)
        assertEquals("Updated just now", draft.ageText(now, "Updated %d min ago", "Updated just now"))
    }

    @Test
    fun `unplanned draft has no age text`() {
        val draft = TripDraft(origin = origin, destination = destination)
        assertNull(draft.ageText(now, "Updated %d min ago", "Updated just now"))
    }

    @Test
    fun `depart-now cannot browse earlier`() {
        assertFalse(TripDraft(origin = origin, destination = destination).canShiftEarlier)
        assertTrue(
            TripDraft(origin = origin, destination = destination, departAtMillis = now + 60_000L)
                .canShiftEarlier
        )
        assertTrue(
            TripDraft(origin = origin, destination = destination, arriveByMillis = now + 60_000L)
                .canShiftEarlier
        )
    }

    @Test
    fun `shifting a depart-at keeps its mode and moves the time`() {
        val draft = TripDraft(origin = origin, destination = destination, departAtMillis = now + 60 * 60_000L)
        val shifted = draft.shiftedSchedule(30 * 60_000L, now)
        assertEquals(now + 90 * 60_000L, shifted.departAtMillis)
        assertNull(shifted.arriveByMillis)
    }

    @Test
    fun `shifting an arrive-by keeps its mode and moves the time`() {
        val draft = TripDraft(origin = origin, destination = destination, arriveByMillis = now + 60 * 60_000L)
        val shifted = draft.shiftedSchedule(-30 * 60_000L, now)
        assertEquals(now + 30 * 60_000L, shifted.arriveByMillis)
        assertNull(shifted.departAtMillis)
    }

    @Test
    fun `shifting depart-now anchors an explicit departure`() {
        val draft = TripDraft(origin = origin, destination = destination)
        val shifted = draft.shiftedSchedule(30 * 60_000L, now)
        assertEquals(now + 30 * 60_000L, shifted.departAtMillis)
        assertNull(shifted.arriveByMillis)
    }
}
