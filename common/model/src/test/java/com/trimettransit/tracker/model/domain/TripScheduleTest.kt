package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripPoint
import org.joda.time.DateTimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripScheduleTest {
    private val zone = DateTimeZone.UTC

    @Test
    fun `same day is today`() {
        val now = 1_700_000_000_000L
        assertEquals(ScheduleDayKind.TODAY, scheduleDayKind(now + 3_600_000L, now, zone))
    }

    @Test
    fun `next day is tomorrow`() {
        val now = 1_700_000_000_000L
        assertEquals(ScheduleDayKind.TOMORROW, scheduleDayKind(now + 24 * 3_600_000L, now, zone))
    }

    @Test
    fun `two days out is later`() {
        val now = 1_700_000_000_000L
        assertEquals(ScheduleDayKind.LATER, scheduleDayKind(now + 48 * 3_600_000L, now, zone))
    }

    @Test
    fun `yesterday is later, never today`() {
        val now = 1_700_000_000_000L
        assertEquals(ScheduleDayKind.LATER, scheduleDayKind(now - 24 * 3_600_000L, now, zone))
    }

    @Test
    fun `depart-at request time carries the chosen instant without arrive-by`() {
        val draft = TripDraft(
            origin = TripPoint(45.5, -122.6),
            destination = TripPoint(45.4, -122.5),
            departAtMillis = 42L
        )
        val time = draft.requestTime()
        assertEquals(false, time.arriveBy)
        assertEquals(42L, time.timeMillis)
    }

    @Test
    fun `depart-now request time has no instant`() {
        val time = TripDraft(origin = TripPoint(45.5, -122.6), destination = TripPoint(45.4, -122.5)).requestTime()
        assertEquals(false, time.arriveBy)
        assertNull(time.timeMillis)
    }

    @Test
    fun `arrive-by request time wins when both are somehow set`() {
        val draft = TripDraft(
            origin = TripPoint(45.5, -122.6),
            destination = TripPoint(45.4, -122.5),
            departAtMillis = 42L,
            arriveByMillis = 99L
        )
        val time = draft.requestTime()
        assertEquals(true, time.arriveBy)
        assertEquals(99L, time.timeMillis)
    }
}
