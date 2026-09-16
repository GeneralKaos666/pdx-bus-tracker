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

fun minutesUntil(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
    return (epochMillis - nowMillis) / 60000
}

/**
 * Milliseconds until the start of the next wall-clock-minute boundary (second 0 of the
 * upcoming minute) — a full minute when already exactly on a boundary, so a tick loop
 * doing `delay(nextMinuteBoundaryDelayMillis())` never spins on a zero delay. Used to
 * re-align countdown ticks so an arrivals flip rolls exactly when "8 min" becomes
 * "7 min" instead of up to ~30s late on a fixed-interval timer.
 */
fun nextMinuteBoundaryDelayMillis(nowMillis: Long = System.currentTimeMillis()): Long =
    60_000L - nowMillis % 60_000L
