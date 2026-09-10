package com.trimettransit.tracker.util

import android.content.Context
import org.joda.time.DateTime

/** A 12-hour clock time split for display, e.g. `9:05` / `AM`. */
data class ClockTime(val text: String, val period: String)

/** Formats [dateTime] as a 12-hour clock time in the user's zone: `9:05` and `AM`. */
fun clockTime(dateTime: DateTime): ClockTime {
    val raw = dateTime.hourOfDay
    val (hour, period) = when {
        raw == 0 -> 12 to "AM"
        raw < 12 -> raw to "AM"
        raw == 12 -> 12 to "PM"
        else -> (raw - 12) to "PM"
    }
    return ClockTime(text = "$hour:${dateTime.minuteOfHour.toString().padStart(2, '0')}", period = period)
}

fun formatDateTime(dateTime: DateTime, context: Context): String {
    val builder = StringBuilder()
    val daysOfWeek = context.resources.getStringArray(R.array.days_of_week)
    if (dateTime.toLocalDate() != DateTime.now().toLocalDate()) {
        builder.append(daysOfWeek[dateTime.dayOfWeek - 1])
        builder.append(", ")
    }
    val time = clockTime(dateTime)
    builder.append("${time.text} ${time.period}")
    return builder.toString()
}

fun minutesUntil(dateTime: DateTime): Long {
    return (dateTime.millis - DateTime.now().millis) / 60000
}

/**
 * Whole minutes until an arrival at [epochMillis]: the difference truncated toward
 * zero in whole minutes. Negative when [epochMillis] is in the past; 0 for a time
 * up to 59s in the future. Callers that need a floor of 0 must coerceAtLeast(0).
 */
fun minutesUntil(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
    return (epochMillis - nowMillis) / 60000
}
