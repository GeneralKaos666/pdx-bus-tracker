package com.trimettransit.tracker.util

import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DateUtilsTest {

    private var originalZone: DateTimeZone? = null

    @Before
    fun pinToUtc() {
        originalZone = DateTimeZone.getDefault()
        DateTimeZone.setDefault(DateTimeZone.UTC)
    }

    @After
    fun restoreZone() {
        originalZone?.let(DateTimeZone::setDefault)
    }

    // minutesUntil(epochMillis, now)

    @Test
    fun `minutesUntil is whole minutes truncent toward zero`() {
        val now = 100_000L
        assertEquals(1L, minutesUntil(now + 60_000L, now))
        assertEquals(1L, minutesUntil(now + 60_000L + 59_999L, now))
        assertEquals(0L, minutesUntil(now + 59_999L, now))
    }

    @Test
    fun `minutesUntil is negative for past times`() {
        val now = 100_000L
        assertEquals(-1L, minutesUntil(now - 60_000L, now))
        assertEquals(0L, minutesUntil(now - 1L, now))
    }

    @Test
    fun `minutesUntil is zero at the exact boundary`() {
        assertEquals(0L, minutesUntil(100_000L, 100_000L))
    }

    @Test
    fun `minutesUntil callers can floor to zero`() {
        val now = 100_000L
        assertEquals(0L, minutesUntil(now + 59_999L, now).coerceAtLeast(0L))
        assertEquals(0L, minutesUntil(now - 60_000L, now).coerceAtLeast(0L))
    }

    // clockTime

    @Test
    fun `clockTime formats midnight morning`() {
        assertEquals(ClockTime("12:30", "AM"), clockTime(DateTime(2026, 9, 11, 0, 30)))
        assertEquals(ClockTime("9:05", "AM"), clockTime(DateTime(2026, 9, 11, 9, 5)))
    }

    @Test
    fun `clockTime formats noon as 12 PM`() {
        assertEquals(ClockTime("12:00", "PM"), clockTime(DateTime(2026, 9, 11, 12, 0)))
    }

    @Test
    fun `clockTime formats afternoon and evening`() {
        assertEquals(ClockTime("1:05", "PM"), clockTime(DateTime(2026, 9, 11, 13, 5)))
        assertEquals(ClockTime("11:59", "PM"), clockTime(DateTime(2026, 9, 11, 23, 59)))
    }
}