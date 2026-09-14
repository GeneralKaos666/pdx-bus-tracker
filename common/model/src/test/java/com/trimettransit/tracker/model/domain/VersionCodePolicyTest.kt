package com.trimettransit.tracker.model.domain

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Locks in the release versionCode policy (AUTO vs AUTO_OFFSET) so it cannot silently regress into
 * the "full offset" inflation bug that once pushed codes into the thousands. Inputs mirror GPP 4.1.1.
 */
class VersionCodePolicyTest {

    // AUTO (offsetMode = false)

    @Test
    fun `AUTO single output equals liveMax plus one when liveMax is at or above default minus one`() {
        assertEquals(listOf(7711), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 7710, outputCount = 1))
        assertEquals(listOf(7710), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 7709, outputCount = 1))
        assertEquals(listOf(8001), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 8000, outputCount = 1))
    }

    @Test
    fun `AUTO keeps the hardcoded default when liveMax is far below default`() {
        assertEquals(listOf(7710), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 517, outputCount = 1))
        assertEquals(listOf(7710), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 1, outputCount = 1))
    }

    @Test
    fun `AUTO indexes each additional output above the base patch`() {
        assertEquals(listOf(7710, 7711, 7712), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 7709, outputCount = 3))
        assertEquals(listOf(7710, 7711, 7712), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 100, outputCount = 3))
    }

    @Test
    fun `AUTO uses liveMax plus one per output when liveMax exceeds default`() {
        assertEquals(listOf(8001, 8002), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 8000, outputCount = 2))
    }

    // AUTO_OFFSET (offsetMode = true) - the strategy that once inflated codes

    @Test
    fun `AUTO_OFFSET keeps defaults when every default already exceeds liveMax`() {
        assertEquals(listOf(7710), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 100, outputCount = 1, offsetMode = true))
    }

    @Test
    fun `AUTO_OFFSET adds liveMax as a full offset`() {
        assertEquals(listOf(15710), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 8000, outputCount = 1, offsetMode = true))
        assertEquals(listOf(15710, 15710), effectiveVersionCodes(defaultCode = 7710, liveMaxCode = 8000, outputCount = 2, offsetMode = true))
    }
}