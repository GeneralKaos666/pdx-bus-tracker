package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.BlockPosition

/**
 * The vehicle positions worth drawing while a rider tracks one arrival.
 *
 * Prefer the tracked vehicle itself; when it reports no position, fall back to the other
 * live vehicles on the tracked line so the map is never empty. [routeOfVehicle] resolves
 * a vehicle id to its route without the caller building an intermediate map per frame.
 */
fun trackedBlockPositions(
    positions: List<BlockPosition>,
    trackingVehicleId: Int,
    trackingRouteId: Int,
    routeOfVehicle: (Int) -> Int?
): List<BlockPosition> {
    if (trackingVehicleId > 0) {
        val own = positions.filter { it.vehicleID == trackingVehicleId }
        if (own.isNotEmpty()) return own
    }
    return positions.filter { routeOfVehicle(it.vehicleID) == trackingRouteId }
}
