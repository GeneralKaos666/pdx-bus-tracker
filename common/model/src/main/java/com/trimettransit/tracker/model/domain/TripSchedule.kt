package com.trimettransit.tracker.model.domain

import org.joda.time.DateTime
import org.joda.time.DateTimeZone

/** How a chosen schedule time relates to today, for compact labels like "Today 4:30 PM". */
enum class ScheduleDayKind { TODAY, TOMORROW, LATER }

/**
 * Classifies [chosenMillis] against [nowMillis] in [zone]: today, tomorrow, or an
 * unspecified later day (including dates in the past, which the picker never confirms).
 */
fun scheduleDayKind(
    chosenMillis: Long,
    nowMillis: Long,
    zone: DateTimeZone = DateTimeZone.getDefault()
): ScheduleDayKind {
    val chosenDay = DateTime(chosenMillis, zone).toLocalDate()
    val today = DateTime(nowMillis, zone).toLocalDate()
    return when (chosenDay) {
        today -> ScheduleDayKind.TODAY
        today.plusDays(1) -> ScheduleDayKind.TOMORROW
        else -> ScheduleDayKind.LATER
    }
}
