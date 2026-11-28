package com.trimettransit.tracker.feature.trips

import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.util.clockTime
import com.trimettransit.tracker.util.formatDuration
import org.joda.time.DateTime

/** Formats a scheduled time, or an em dash when the WS returned none (walk legs/itineraries). */
internal fun DateTime?.clockTimeText(): String =
    this?.let { t ->
        val time = clockTime(t)
        if (time.period.isEmpty()) time.text else "${time.text} ${time.period}"
    } ?: "—"

/**
 * Plain-text version of one itinerary for the share sheet. Kept free of Android types so the
 * format is directly testable: endpoints, date, times with duration, then the route numbers
 * in boarding order. Separators and duration wording arrive as parameters so the share text
 * is translatable instead of hardcoding arrows.
 */
internal fun buildTripShareText(
    originLabel: String,
    destinationLabel: String,
    dateLabel: String,
    itinerary: TripItinerary,
    separator: String = " → ",
    dash: String = " – ",
    hoursMinutesFmt: String = "%1\$d h %2\$d min",
    minutesOnlyFmt: String = "%2\$d min",
    hoursOnlyFmt: String = "%1\$d h"
): String {
    val times = "${itinerary.departure.clockTimeText()}$dash${itinerary.arrival.clockTimeText()}"
    val routes = itinerary.legs
        .filterNot { it.isWalk }
        .mapNotNull { leg -> leg.routeNumber?.takeIf { it.isNotBlank() } }
        .distinct()
        .joinToString(separator)
    return buildString {
        append(originLabel)
        append(separator)
        append(destinationLabel)
        append('\n')
        if (dateLabel.isNotBlank()) {
            append(dateLabel)
            append('\n')
        }
        append(times)
        append(" (")
        append(
            formatDuration(
                itinerary.durationMillis,
                hoursMinutesFmt,
                minutesOnlyFmt,
                hoursOnlyFmt = hoursOnlyFmt
            )
        )
        append(')')
        if (routes.isNotBlank()) {
            append('\n')
            append(routes)
        }
    }
}
