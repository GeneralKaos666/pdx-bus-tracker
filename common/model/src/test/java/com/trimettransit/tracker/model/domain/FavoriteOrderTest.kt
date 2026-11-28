package com.trimettransit.tracker.model.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class FavoriteOrderTest {

    @Test
    fun `arranged rows come first in their stored order, then insertion order`() {
        val rows = listOf(
            FavoriteOrderRow(locId = 40, sortOrder = 0, id = 1),
            FavoriteOrderRow(locId = 20, sortOrder = 1, id = 2),
            FavoriteOrderRow(locId = 30, sortOrder = 2, id = 3)
        )
        // Loc 40 was never arranged (sort 0), so its fallback key is its row id (1),
        // which ties with loc 20's arranged position 1; the row-id tie-break puts 40 first.
        assertEquals(listOf(40, 20, 30), orderedFavoriteIds(rows))
    }

    @Test
    fun `equal sort positions fall back to the row id so the order is stable`() {
        val rows = listOf(
            FavoriteOrderRow(locId = 7, sortOrder = 5, id = 9),
            FavoriteOrderRow(locId = 8, sortOrder = 5, id = 4)
        )
        assertEquals(listOf(8, 7), orderedFavoriteIds(rows))
    }
}
