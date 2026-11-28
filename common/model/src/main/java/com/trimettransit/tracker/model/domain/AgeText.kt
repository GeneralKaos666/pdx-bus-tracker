package com.trimettransit.tracker.model.domain

/**
 * Human age of a timestamp, or null when it was never recorded ([timestampMillis] <= 0).
 *
 * Sub-minute ages read as [justNow]; older ages format [minutesFmt] with whole minutes
 * elapsed. Future timestamps (clock skew) clamp to [justNow]. Shared by the trip planner's
 * data-age chip and the home-screen widget so both say the same thing at the same minute.
 */
fun ageText(
    timestampMillis: Long,
    nowMillis: Long,
    minutesFmt: String,
    justNow: String
): String? {
    if (timestampMillis <= 0L) return null
    val mins = ((nowMillis - timestampMillis) / 60_000L).coerceAtLeast(0)
    return if (mins < 1) justNow else minutesFmt.format(mins)
}
