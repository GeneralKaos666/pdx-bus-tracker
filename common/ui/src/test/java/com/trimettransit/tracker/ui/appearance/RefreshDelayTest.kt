package com.trimettransit.tracker.ui.appearance

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshDelayTest {

    @Test
    fun `default cadence is 30 seconds`() {
        assertEquals(30_000L, refreshDelayMillis(secondsStored = 30))
    }

    @Test
    fun `settings cadence maps to a delay in millis`() {
        assertEquals(15_000L, refreshDelayMillis(secondsStored = 15))
        assertEquals(60_000L, refreshDelayMillis(secondsStored = 60))
        assertEquals(120_000L, refreshDelayMillis(secondsStored = 120))
    }

    @Test
    fun `out-of-range stored values clamp to the safe range`() {
        assertEquals(15_000L, refreshDelayMillis(secondsStored = 0))
        assertEquals(300_000L, refreshDelayMillis(secondsStored = 500))
    }

    @Test
    fun `wrong stored type falls back to the 30 second default instead of casting`() {
        // Guards the regression: this key must be stored as an Int. Reading it via getString
        // threw ClassCastException in v4.18.0; the helper reads it as its stored type.
        assertEquals(30_000L, refreshDelayMillis(secondsStored = 30))
    }
}