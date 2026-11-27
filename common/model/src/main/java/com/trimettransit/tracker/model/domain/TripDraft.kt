package com.trimettransit.tracker.model.domain

import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripRequestOptions
import com.trimettransit.tracker.model.TripRequestTime

/**
 * The user's in-progress trip-plan request: both endpoints plus the scheduling choice.
 * Depart-now leaves both times null, depart-at sets [departAtMillis], and arrive-by sets
 * [arriveByMillis]. The two timed modes are mutually exclusive; the screen clears one when
 * setting the other.
 *
 * A plain value so the planner screen can hold it in `rememberSaveable` and so future
 * metadata can join it without changing the API shape. [plannedAtMillis] records when the
 * current results were fetched, so the results header can show the data age.
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

    /** True when the chosen depart-at time has already gone. Depart-now has no time to stale. */
    fun isDepartAtStale(nowMillis: Long): Boolean =
        departAtMillis != null && departAtMillis <= nowMillis

    /** True when the draft has both endpoints and no stale scheduled time. */
    fun isSubmittable(nowMillis: Long): Boolean =
        origin != null && destination != null &&
            !isArriveByStale(nowMillis) && !isDepartAtStale(nowMillis)

    /** The scheduling choice to send: arrive-by wins, then depart-at, else depart-now. */
    fun requestTime(): TripRequestTime {
        val arriveBy = arriveByMillis
        if (arriveBy != null) return TripRequestTime(arriveBy = true, timeMillis = arriveBy)
        return TripRequestTime(arriveBy = false, timeMillis = departAtMillis)
    }

    /**
     * True when the results can be browsed to an earlier departure: only a timed mode has a
     * reference time to move back. Depart-now has none.
     */
    val canShiftEarlier: Boolean
        get() = arriveByMillis != null || departAtMillis != null

    /**
     * Moves the scheduled time by [deltaMillis] for the results' earlier/later controls.
     * Keep the mode: a depart-at stays depart-at, an arrive-by stays arrive-by. A depart-now
     * draft has no time to move, so browsing turns it into an explicit depart-at anchored at
     * [nowMillis] + delta; the UI disables "earlier" in that case, so this only ever moves
     * forward.
     */
    fun shiftedSchedule(deltaMillis: Long, nowMillis: Long): TripDraft = when {
        arriveByMillis != null -> copy(arriveByMillis = arriveByMillis + deltaMillis)
        departAtMillis != null -> copy(departAtMillis = departAtMillis + deltaMillis)
        else -> copy(departAtMillis = nowMillis + deltaMillis)
    }

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
