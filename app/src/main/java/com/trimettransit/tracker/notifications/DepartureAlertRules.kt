package com.trimettransit.tracker.notifications

import com.trimettransit.tracker.model.Arrival
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Pure decision logic for departure alerts, kept Android-free so the rules are
 * unit-testable. [DepartureAlertWorker] feeds it arrivals and persists the
 * fired-key set via [DepartureAlertPrefs].
 */
object DepartureAlertRules {
    /** No alerts between 11 PM and 5 AM (TriMet's overnight service gap). */
    const val QUIET_FROM_HOUR = 23
    const val QUIET_UNTIL_HOUR = 5
    const val FIRED_CAP = 100

    /**
     * Minutes until the arrival passes — estimate when present, schedule otherwise.
     * Negative for arrivals already past; null when neither time is known.
     */
    fun minutesUntil(arrival: Arrival, nowMillis: Long): Long? {
        val atMillis = arrival.estimatedMillis.takeIf { it > 0 }
            ?: arrival.scheduledMillis.takeIf { it > 0 }
            ?: return null
        return (atMillis - nowMillis) / 60_000L
    }

    fun isQuietHours(nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean {
        val hour = ZonedDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone).hour
        return hour >= QUIET_FROM_HOUR || hour < QUIET_UNTIL_HOUR
    }

    /** Boardable arrivals (not drop-off-only) departing within [windowMinutes] from now. */
    fun actionableArrivals(
        arrivals: List<Arrival>,
        nowMillis: Long,
        windowMinutes: Int
    ): List<Arrival> = arrivals.filter { arrival ->
        !arrival.dropOffOnly &&
            minutesUntil(arrival, nowMillis) in 0L..windowMinutes.toLong()
    }

    /** Arrivals whose (stop, trip) has not already fired an alert. */
    fun filterNew(arrivals: List<Arrival>, fired: Set<String>, locId: Int): List<Arrival> =
        arrivals.filterNot { fired.contains(firedKey(locId, it)) }

    /**
     * Bounds the persisted fired set so it can't grow unbounded across restarts.
     * Keys carry no timestamps, so "oldest" is the in-memory insertion order of the
     * [LinkedHashSet] read from prefs within a run; across restarts
     * SharedPreferences does not preserve set order, making eviction arbitrary —
     * accepted because a wrong eviction only risks one duplicate alert.
     */
    fun prune(fired: Set<String>, cap: Int = FIRED_CAP): Set<String> =
        if (fired.size <= cap) fired
        else fired.toList().takeLast(cap).toCollection(LinkedHashSet())

    /**
     * One alert per departure — (stop, trip) identity key. Blank trip IDs (the API
     * omits them on some arrivals) fall back to the composite (stop, block,
     * schedule) so distinct trips never share one key and silence each other.
     */
    fun firedKey(locId: Int, tripId: String, blockId: Int = 0, scheduledMillis: Long = 0L): String =
        if (tripId.isNotBlank()) "$locId:$tripId" else "$locId:b$blockId:$scheduledMillis"

    /** [Arrival] convenience form of [firedKey]. */
    fun firedKey(locId: Int, arrival: Arrival): String =
        firedKey(locId, arrival.tripID, arrival.blockID, arrival.scheduledMillis)
}