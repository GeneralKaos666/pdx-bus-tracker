package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripRequestOptions

/**
 * The user's in-progress trip-plan request: both endpoints plus the scheduling choice.
 * Depart-now is represented by leaving both times null; arrive-by sets [arriveByMillis].
 * ([departAtMillis] is reserved for a future depart-at choice; nothing sets it yet.)
 *
 * A plain value so the planner screen can hold it in `rememberSaveable` and so future
 * metadata (e.g. a planned-at timestamp) can join it without changing the API shape.
 */
data class TripDraft(
    val origin: TripPoint? = null,
    val destination: TripPoint? = null,
    val departAtMillis: Long? = null,
    val arriveByMillis: Long? = null,
    val options: TripRequestOptions = TripRequestOptions()
) {
    /**
     * True when the chosen arrive-by time has already gone (or is exactly now, which the
     * planner has already passed by the time it sends the request). Such a time is never
     * sent to the API. A depart-now draft is never stale.
     */
    fun isArriveByStale(nowMillis: Long): Boolean =
        arriveByMillis != null && arriveByMillis <= nowMillis

    /** True when the draft has both endpoints and no stale arrive-by time. */
    fun isSubmittable(nowMillis: Long): Boolean =
        origin != null && destination != null && !isArriveByStale(nowMillis)
}
