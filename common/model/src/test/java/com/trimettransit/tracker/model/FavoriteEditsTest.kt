package com.trimettransit.tracker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteEditsTest {

    @Test
    fun `moveStops reorders stops by index`() {
        val stops = listOf(
            Stop(desc = "A", locId = 1),
            Stop(desc = "B", locId = 2),
            Stop(desc = "C", locId = 3)
        )
        assertEquals(listOf(2, 3, 1), FavoriteEdits.moveStops(stops, 0, 2).map { it.locId })
    }

    @Test
    fun `reorderTarget clamps drags past either end`() {
        // Dragging past the bottom lands on the last item instead of no-op.
        assertEquals(2, FavoriteEdits.reorderTarget(from = 1, delta = 10, size = 3))
        // Dragging past the top lands on the first item.
        assertEquals(0, FavoriteEdits.reorderTarget(from = 1, delta = -10, size = 3))
    }

    @Test
    fun `reorderTarget applies in-range deltas unchanged`() {
        assertEquals(2, FavoriteEdits.reorderTarget(from = 0, delta = 2, size = 4))
        assertEquals(0, FavoriteEdits.reorderTarget(from = 2, delta = -2, size = 4))
    }

    @Test
    fun `reorderTarget on empty list returns from`() {
        assertEquals(3, FavoriteEdits.reorderTarget(from = 3, delta = 5, size = 0))
    }

    @Test
    fun `welcome shows only for first-run empty loaded list`() {
        assertEquals(true, FavoriteEdits.shouldShowWelcome(alreadyShown = false, isEmpty = true, isLoading = false))
        assertEquals(false, FavoriteEdits.shouldShowWelcome(alreadyShown = true, isEmpty = true, isLoading = false))
        assertEquals(false, FavoriteEdits.shouldShowWelcome(alreadyShown = false, isEmpty = false, isLoading = false))
        assertEquals(false, FavoriteEdits.shouldShowWelcome(alreadyShown = false, isEmpty = true, isLoading = true))
    }
}
