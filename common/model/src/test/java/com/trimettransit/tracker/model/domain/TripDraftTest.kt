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
        assertTrue(draft.isSubmittable(now))
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
}
