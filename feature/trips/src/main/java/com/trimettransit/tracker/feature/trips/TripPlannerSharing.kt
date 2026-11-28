package com.trimettransit.tracker.feature.trips

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripPoint
import java.util.Date
import com.trimettransit.tracker.feature.trips.R

/** Shares the selected option as plain text so a rider can forward a plan. */
internal fun shareTrip(
    context: Context,
    originLabel: String,
    destinationLabel: String,
    itinerary: TripItinerary
) {
    val dateLabel = itinerary.departure?.let { departure ->
        DateFormat.getMediumDateFormat(context).format(Date(departure.millis))
    }.orEmpty()
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_SUBJECT,
            context.getString(R.string.share_trip_subject_fmt, originLabel, destinationLabel)
        )
        putExtra(
            Intent.EXTRA_TEXT,
            buildTripShareText(
                originLabel = originLabel,
                destinationLabel = destinationLabel,
                dateLabel = dateLabel,
                itinerary = itinerary,
                separator = context.getString(R.string.trip_share_separator),
                dash = context.getString(R.string.trip_share_dash),
                hoursMinutesFmt = context.getString(R.string.duration_hours_minutes_fmt),
                minutesOnlyFmt = context.getString(R.string.duration_minutes_fmt),
                hoursOnlyFmt = context.getString(R.string.duration_hours_fmt)
            )
        )
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_trip)))
}

/** Stable identity for a from→to pair, used to decide whether a restored plan is still relevant. */
internal fun endpointKey(from: TripPoint, to: TripPoint): String =
    "${from.latitude},${from.longitude}->${to.latitude},${to.longitude}"
