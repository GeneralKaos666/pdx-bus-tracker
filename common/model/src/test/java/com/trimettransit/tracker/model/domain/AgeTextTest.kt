package com.trimettransit.tracker.model.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AgeTextTest {

    @Test
    fun `sub-minute age reads just now`() {
        assertEquals("just now", ageText(1_000L, 1_500L, "(%d min)", "just now"))
    }

    @Test
    fun `whole minutes are formatted`() {
        assertEquals("(5 min)", ageText(0L + 60_000L, 6 * 60_000L, "(%d min)", "just now"))
    }

    @Test
    fun `a never-recorded timestamp is unknown`() {
        assertNull(ageText(0L, 10_000L, "(%d min)", "just now"))
        assertNull(ageText(-1L, 10_000L, "(%d min)", "just now"))
    }

    @Test
    fun `clock skew clamps to just now`() {
        assertEquals("just now", ageText(10_000L, 5_000L, "(%d min)", "just now"))
    }
}
