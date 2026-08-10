package com.trimettransit.tracker.util

import android.content.Context
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

fun formatDateTime(dateTime: DateTime, context: Context): String {
    val builder = StringBuilder()
    val daysOfWeek = context.resources.getStringArray(R.array.days_of_week)
    if (dateTime.dayOfWeek != DateTime.now().dayOfWeek) {
        builder.append(daysOfWeek[dateTime.dayOfWeek - 1])
        builder.append(", ")
    }
    builder.append(dateTime.toString(DateTimeFormat.forPattern("h:mm aa")))
    return builder.toString()
}

fun minutesUntil(dateTime: DateTime): Long {
    return (dateTime.millis - DateTime.now().millis) / 60000
}
