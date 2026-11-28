package com.trimettransit.tracker.ui.components

import com.trimettransit.tracker.model.Stop
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StopSearchTest {

    private fun stop(locId: Int, desc: String, dir: String = "") = Stop(
        desc = desc,
        dirDesc = dir,
        latitude = 0.0,
        longitude = 0.0,
        locId = locId
    )

    @Test
    fun `blank query returns nothing`() {
        assertTrue(searchStops(listOf(stop(1, "A")), "   ").isEmpty())
        assertTrue(searchStops(listOf(stop(1, "A")), "").isEmpty())
    }

    @Test
    fun `matches name and direction case-insensitively`() {
        val stops = listOf(stop(1, "Pioneer Square", "Northbound"), stop(2, "Division", "Eastbound"))
        assertEquals(listOf(1), searchStops(stops, "pioneer").map { it.locId })
        assertEquals(listOf(2), searchStops(stops, "east").map { it.locId })
    }

    @Test
    fun `matches a stop id by digits`() {
        val stops = listOf(stop(8337, "Pioneer Square"), stop(8340, "Division"))
        assertEquals(listOf(8337), searchStops(stops, "8337").map { it.locId })
        assertEquals(listOf(8337), searchStops(stops, "833").map { it.locId })
    }

    @Test
    fun `results are capped`() {
        val stops = (1..300).map { stop(it, "Stop $it") }
        assertEquals(STOP_SEARCH_MAX_RESULTS, searchStops(stops, "Stop").size)
    }
}
