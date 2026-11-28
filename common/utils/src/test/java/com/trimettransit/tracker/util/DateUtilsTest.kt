package com.trimettransit.tracker.util

import java.util.Locale
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

    // nextMinuteBoundaryDelayMillis(nowMillis) -> ms until the next wall-clock minute boundary

    @Test
    fun `nextMinuteBoundaryDelayMillis waits a full minute on the boundary`() {
        // 60_000ms is exactly a boundary (unlike 100_000ms, which sits 40s into a minute),
        // so the next boundary is a full minute away rather than a zero delay.
        assertEquals(60_000L, nextMinuteBoundaryDelayMillis(60_000L))
    }

    @Test
    fun `nextMinuteBoundaryDelayMillis counts the seconds remaining in the minute`() {
        // 100_000ms = 1min 40s into the hour; 20s remain to the next minute boundary.
        assertEquals(20_000L, nextMinuteBoundaryDelayMillis(100_000L))
        assertEquals(1_000L, nextMinuteBoundaryDelayMillis(119_000L))
    }

    @Test
    fun `nextMinuteBoundaryDelayMillis rolls back up at the top of the minute`() {
        // 120_000ms is exactly a boundary, so the next one is a full minute away.
        assertEquals(60_000L, nextMinuteBoundaryDelayMillis(120_000L))
        assertEquals(59_000L, nextMinuteBoundaryDelayMillis(121_000L))
    }

    // clockTime

    @Test
    fun `clockTime formats midnight morning`() {
        assertEquals(ClockTime("12:30", "AM"), clockTime(DateTime(2026, 9, 11, 0, 30), Locale.US))
        assertEquals(ClockTime("9:05", "AM"), clockTime(DateTime(2026, 9, 11, 9, 5), Locale.US))
    }

    @Test
    fun `clockTime formats noon as 12 PM`() {
        assertEquals(ClockTime("12:00", "PM"), clockTime(DateTime(2026, 9, 11, 12, 0), Locale.US))
    }

    @Test
    fun `clockTime formats afternoon and evening`() {
        assertEquals(ClockTime("1:05", "PM"), clockTime(DateTime(2026, 9, 11, 13, 5), Locale.US))
        assertEquals(ClockTime("11:59", "PM"), clockTime(DateTime(2026, 9, 11, 23, 59), Locale.US))
    }

    @Test
    fun `clockTime follows the locale's 24 hour clock`() {
        val spanish = clockTime(DateTime(2026, 9, 11, 21, 5), Locale.forLanguageTag("es"))
        assertEquals("21:05", spanish.text)
        assertEquals("", spanish.period)
    }

    @Test
    fun `clockTime keeps the US 12 hour split`() {
        val us = clockTime(DateTime(2026, 9, 11, 21, 5), Locale.US)
        assertEquals("9:05", us.text)
        assertEquals("PM", us.period)
    }

    @Test
    fun `formatDuration uses the caller's hour and minute templates`() {
        assertEquals("3 h 20 min", formatDuration(12_000_000L, "%1\$d h %2\$d min", "%2\$d min", Locale.US))
        assertEquals("20 min", formatDuration(1_200_000L, "%1\$d h %2\$d min", "%2\$d min", Locale.US))
        assertEquals("0 min", formatDuration(0L, "%1\$d h %2\$d min", "%2\$d min", Locale.US))
    }
}
