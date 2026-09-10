package com.trimettransit.tracker.widget

import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.widget.WidgetSnapshotCache.Row
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetRowConfigTest {

    @Test
    fun `orders rows by selectedStopIds`() {
        val rows = listOf(rowAt(1), rowAt(2), rowAt(3))
        val config = WidgetConfig().copy(selectedStopIds = listOf("3", "1", "2"))
        assertEquals(
            listOf("3", "1", "2"),
            applyRowConfig(rows, config).map { it.stop.locId.toString() }
        )
    }

    @Test
    fun `keeps favorite order when no stop selected`() {
        val rows = listOf(rowAt(3), rowAt(1), rowAt(2))
        val config = WidgetConfig()
        assertEquals(
            listOf(3, 1, 2),
            applyRowConfig(rows, config).map { it.stop.locId }
        )
    }

    @Test
    fun `drops rows not in the selectedStopIds`() {
        val rows = listOf(rowAt(1), rowAt(2), rowAt(3))
        val config = WidgetConfig().copy(selectedStopIds = listOf("3"))
        assertEquals(listOf(3), applyRowConfig(rows, config).map { it.stop.locId })
    }

    @Test
    fun `filters rows by routeFilter`() {
        val rows = listOf(
            rowAt(1, route = 4),
            rowAt(2, route = 9),
            rowAt(3, route = 12)
        )
        val config = WidgetConfig().copy(routeFilter = listOf("4", "9"))
        assertEquals(listOf(4, 9), applyRowConfig(rows, config).map { it.stop.routeNum })
    }

    @Test
    fun `routeFilter alone keeps favorite order`() {
        val rows = listOf(
            rowAt(3, route = 9),
            rowAt(1, route = 4),
            rowAt(2, route = 9)
        )
        val config = WidgetConfig().copy(routeFilter = listOf("9"))
        assertEquals(listOf(3, 2), applyRowConfig(rows, config).map { it.stop.locId })
    }

    @Test
    fun `caps rows at maxStops`() {
        val rows = (1..5).map { rowAt(it) }
        val config = WidgetConfig().copy(maxStops = 2)
        assertEquals(listOf(1, 2), applyRowConfig(rows, config).map { it.stop.locId })
    }

    @Test
    fun `empty selection and routeFilter return all rows`() {
        val rows = listOf(rowAt(1, route = 4), rowAt(2, route = 12))
        val config = WidgetConfig()
        assertEquals(2, applyRowConfig(rows, config).size)
    }

    private fun rowAt(locId: Int, route: Int = 1) = Row(
        stop = Stop(
            desc = "Stop $locId",
            latitude = 45.5,
            longitude = -122.6,
            transitType = "bus",
            locId = locId,
            routeNum = route
        ),
        arrivals = emptyList()
    )
}