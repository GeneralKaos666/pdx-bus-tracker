package com.trimettransit.tracker.util

import android.content.Context
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

fun formatDateTime(dateTime: DateTime, context: Context): String {
    val builder = StringBuilder()
    val daysOfWeek = context.resources.getStringArray(R.array.days_of_week)
    if (dateTime.toLocalDate() != DateTime.now().toLocalDate()) {
        builder.append(daysOfWeek[dateTime.dayOfWeek - 1])
        builder.append(", ")
    }
    builder.append(dateTime.toString(DateTimeFormat.forPattern("h:mm aa")))
    return builder.toString()
}

fun minutesUntil(dateTime: DateTime): Long {
    return (dateTime.millis - DateTime.now().millis) / 60000
}

/** Whole minutes until an arrival at [epochMillis] (floor: 0 until the minute is up). */
fun minutesUntil(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
    return (epochMillis - nowMillis) / 60000
}
