package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripPlan
import com.trimettransit.tracker.model.TripPoint

/**
 * Whether a plan already on screen still describes the endpoints being requested.
 *
 * Endpoints match in either direction: the swap control re-plans the return trip in
 * place, and the commit contract for the planner is that re-planning never blanks the
 * map or closes the results. Same-direction and reversed pairs both keep the previous
 * plan visible while the new request runs; any other pair clears it so stale results
 * are never mistaken for the new ones.
 */
fun keepPlanVisible(current: TripPlan?, from: TripPoint, to: TripPoint): Boolean {
    if (current == null) return false
    val sameDirection = current.from == from && current.to == to
    val reversed = current.from == to && current.to == from
    return sameDirection || reversed
}
