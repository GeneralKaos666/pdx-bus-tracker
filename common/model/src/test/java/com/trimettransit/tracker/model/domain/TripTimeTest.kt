package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripRequestTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TripTimeTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `a depart-now request is never in the past`() {
        assertFalse(TripRequestTime(arriveBy = false, timeMillis = now - 60_000).isInThePast(now))
    }

    @Test
    fun `an arrive-by request with no time chosen is not in the past`() {
        assertFalse(TripRequestTime(arriveBy = true, timeMillis = null).isInThePast(now))
    }

    @Test
    fun `an arrive-by time earlier today is in the past`() {
        assertTrue(TripRequestTime(arriveBy = true, timeMillis = now - 1).isInThePast(now))
    }

    @Test
    fun `an arrive-by time of exactly now is treated as already gone`() {
        assertTrue(TripRequestTime(arriveBy = true, timeMillis = now).isInThePast(now))
    }

    @Test
    fun `a later arrive-by time is fine`() {
        assertFalse(TripRequestTime(arriveBy = true, timeMillis = now + 1).isInThePast(now))
    }
}
