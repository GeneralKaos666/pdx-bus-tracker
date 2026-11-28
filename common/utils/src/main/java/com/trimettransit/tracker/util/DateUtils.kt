package com.trimettransit.tracker.util

import android.content.Context
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import org.joda.time.DateTime

/**
 * A localized short clock time split for display: `9:05` plus the locale's `AM`/`PM`
 * marker, or the whole `21:05` with an empty period in 24-hour locales.
 */
data class ClockTime(val text: String, val period: String)

/**
 * Formats [dateTime] with the user's locale and 12/24-hour convention.
 *
 * A hardcoded 12-hour clock showed English `AM`/`PM` to Spanish riders. The time now
 * follows the locale's 12/24-hour convention; a user who overrides 24-hour format on an
 * English device is still served by the locale convention (wiring the platform toggle
 * needs a Context at every call site). The time zone comes from the [DateTime] itself so
 * tests and callers keep their pinned zone.
 */
fun clockTime(dateTime: DateTime, locale: Locale = Locale.getDefault()): ClockTime {
    val format = DateFormat.getTimeInstance(DateFormat.SHORT, locale)
    format.timeZone = dateTime.zone.toTimeZone()
    // Android may join the time and marker with a narrow no-break space; normalise first
    // so the marker split works on every locale/API level.
    val rendered = format.format(Date(dateTime.millis))
        .replace('\u202F', ' ')
        .replace('\u00A0', ' ')
        .trim()
    val tokens = rendered.split(" ").filter { it.isNotBlank() }
    val marker = tokens.lastOrNull()?.takeIf { token -> token.any { it.isLetter() } }
    return if (marker == null || tokens.size < 2) {
        ClockTime(text = rendered, period = "")
    } else {
        ClockTime(text = tokens.dropLast(1).joinToString(" "), period = marker)
    }
}

fun formatDateTime(dateTime: DateTime, context: Context): String {
    val builder = StringBuilder()
    val daysOfWeek = context.resources.getStringArray(R.array.days_of_week)
    if (dateTime.toLocalDate() != DateTime.now().toLocalDate()) {
        builder.append(daysOfWeek[dateTime.dayOfWeek - 1])
        builder.append(", ")
    }
    val time = clockTime(dateTime)
    builder.append(time.text)
    // 24-hour locales carry no AM/PM marker.
    if (time.period.isNotEmpty()) {
        builder.append(" ").append(time.period)
    }
    return builder.toString()
}

/**
 * Trip duration in the caller's words: [hoursMinutesFmt] when the trip is an hour or
 * longer, [minutesOnlyFmt] otherwise. Both templates receive `(hours, minutes)` so a
 * locale can order or abbreviate them freely.
 */
fun formatDuration(
    millis: Long,
    hoursMinutesFmt: String,
    minutesOnlyFmt: String,
    locale: Locale = Locale.getDefault(),
    hoursOnlyFmt: String? = null
): String {
    val totalMinutes = (millis.coerceAtLeast(0L) / 60_000L)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val template = when {
        hours > 0 && minutes == 0L && hoursOnlyFmt != null -> hoursOnlyFmt
        hours > 0 -> hoursMinutesFmt
        else -> minutesOnlyFmt
    }
    return String.format(locale, template, hours, minutes)
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
