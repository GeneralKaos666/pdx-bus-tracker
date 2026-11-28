package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.BlockPosition
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackedPositionsTest {

    private fun bp(vehicle: Int, route: Int) = BlockPosition(vehicleID = vehicle, routeNumber = route)

    private fun routeOf(positions: List<BlockPosition>): (Int) -> Int? =
        { vehicle -> positions.firstOrNull { it.vehicleID == vehicle }?.routeNumber }

    @Test
    fun `tracked vehicle wins over other same-line vehicles`() {
        val positions = listOf(bp(vehicle = 1, route = 12), bp(vehicle = 2, route = 12))
        val tracked = trackedBlockPositions(positions, trackingVehicleId = 2, trackingRouteId = 12, routeOfVehicle = routeOf(positions))
        assertEquals(listOf(2), tracked.map { it.vehicleID })
    }

    @Test
    fun `no position for the tracked vehicle falls back to the line`() {
        val positions = listOf(bp(vehicle = 1, route = 12), bp(vehicle = 2, route = 12))
        val tracked = trackedBlockPositions(positions, trackingVehicleId = 99, trackingRouteId = 12, routeOfVehicle = routeOf(positions))
        assertEquals(listOf(1, 2), tracked.map { it.vehicleID })
    }

    @Test
    fun `line tracking ignores other lines`() {
        val positions = listOf(bp(vehicle = 1, route = 12), bp(vehicle = 2, route = 33))
        val tracked = trackedBlockPositions(positions, trackingVehicleId = 0, trackingRouteId = 33, routeOfVehicle = routeOf(positions))
        assertEquals(listOf(2), tracked.map { it.vehicleID })
    }
}
