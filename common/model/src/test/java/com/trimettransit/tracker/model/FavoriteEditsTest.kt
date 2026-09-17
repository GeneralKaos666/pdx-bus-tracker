package com.trimettransit.tracker.model

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteEditsTest {

    @Test
    fun `reorder moves id preserving rest without duplicates`() {
        assertEquals(
            listOf(2, 3, 1),
            FavoriteEdits.reorder(listOf(1, 2, 3), from = 0, to = 2)
        )
    }

    @Test
    fun `reorder out of bounds returns unchanged`() {
        val current = listOf(1, 2, 3)
        assertEquals(current, FavoriteEdits.reorder(current, from = -1, to = 1))
        assertEquals(current, FavoriteEdits.reorder(current, from = 0, to = 5))
    }

    @Test
    fun `reorder same index returns unchanged`() {
        val current = listOf(1, 2, 3)
        assertEquals(current, FavoriteEdits.reorder(current, from = 1, to = 1))
    }

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
    fun `welcome shows only for first-run empty loaded list`() {
        assertEquals(true, FavoriteEdits.shouldShowWelcome(alreadyShown = false, isEmpty = true, isLoading = false))
        assertEquals(false, FavoriteEdits.shouldShowWelcome(alreadyShown = true, isEmpty = true, isLoading = false))
        assertEquals(false, FavoriteEdits.shouldShowWelcome(alreadyShown = false, isEmpty = false, isLoading = false))
        assertEquals(false, FavoriteEdits.shouldShowWelcome(alreadyShown = false, isEmpty = true, isLoading = true))
    }
}
