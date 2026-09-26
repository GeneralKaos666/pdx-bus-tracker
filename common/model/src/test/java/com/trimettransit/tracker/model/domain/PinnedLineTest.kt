package com.trimettransit.tracker.model.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PinnedLineTest {

    @Test
    fun `migration sql adds a nullable integer column`() {
        assertEquals(
            "ALTER TABLE favorites ADD COLUMN pinned_line INTEGER",
            ADD_PINNED_LINE_COLUMN_SQL
        )
        // No NOT NULL / DEFAULT clause: existing rows must read back NULL.
        assertFalse(ADD_PINNED_LINE_COLUMN_SQL.contains("NOT NULL"))
        assertFalse(ADD_PINNED_LINE_COLUMN_SQL.contains("DEFAULT"))
    }

    @Test
    fun `null cell maps to no default`() {
        assertNull(mapPinnedLine(isNull = true, value = 0))
    }

    @Test
    fun `stored value passes through`() {
        assertEquals(72, mapPinnedLine(isNull = false, value = 72))
    }

    @Test
    fun `pin initializes the session filter when navigation has no line`() {
        assertEquals(72, resolveSessionLineFilter(pinnedLine = 72, navRouteId = 0))
        assertEquals(72, resolveSessionLineFilter(pinnedLine = 72, navRouteId = -1))
    }

    @Test
    fun `explicit navigation line wins over the stored pin`() {
        assertEquals(12, resolveSessionLineFilter(pinnedLine = 72, navRouteId = 12))
    }

    @Test
    fun `no pin and no navigation line means no filter`() {
        assertEquals(0, resolveSessionLineFilter(pinnedLine = null, navRouteId = 0))
        assertEquals(0, resolveSessionLineFilter(pinnedLine = null, navRouteId = -1))
    }

    @Test
    fun `non-positive pins never filter`() {
        assertEquals(0, resolveSessionLineFilter(pinnedLine = 0, navRouteId = 0))
        assertEquals(0, resolveSessionLineFilter(pinnedLine = -1, navRouteId = 0))
    }
}
