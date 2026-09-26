package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripRequestOptions

/**
 * The user's in-progress trip-plan request: both endpoints plus the scheduling choice.
 * Depart-now is represented by leaving both times null; arrive-by sets [arriveByMillis].
 * ([departAtMillis] is reserved for a future depart-at choice; nothing sets it yet.)
 *
 * A plain value so the planner screen can hold it in `rememberSaveable` and so future
 * metadata can join it without changing the API shape. [plannedAtMillis] records when the
 * current results were fetched, so the sheet header can show the data age.
 */
data class TripDraft(
    val origin: TripPoint? = null,
    val destination: TripPoint? = null,
    val departAtMillis: Long? = null,
    val arriveByMillis: Long? = null,
    val options: TripRequestOptions = TripRequestOptions(),
    val plannedAtMillis: Long? = null
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

    /**
     * Human-readable age of the fetched results, or null when nothing has been planned yet.
     * Sub-minute ages read as [justNow]; older ages format [minutesFmt] with the whole
     * minutes elapsed. Future timestamps (clock skew) clamp to [justNow].
     */
    fun ageText(nowMillis: Long, minutesFmt: String, justNow: String): String? {
        val planned = plannedAtMillis ?: return null
        val mins = ((nowMillis - planned) / 60_000L).coerceAtLeast(0)
        return if (mins < 1) justNow else minutesFmt.format(mins)
    }
}
