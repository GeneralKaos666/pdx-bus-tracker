package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.TripRequestOptions
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// The Transit service runs on Portland local time. Resolved via java.time (whose tzdb is
// present on both JVM unit tests and API 31+ devices): the Joda-Time artifact in use
// (net.danlew:android.joda) only serves named zones after JodaTimeAndroid.init(), which
// the app never calls, so DateTimeZone.forID("America/Los_Angeles") throws on every
// runtime here — including plain unit tests (probed) and, worse, production class-load.
/** Transit service's local zone: request dates and response wall-clocks are Portland time. */
internal val TRIP_PLANNER_ZONE: ZoneId = ZoneId.of("America/Los_Angeles")

private val TRIP_DATE_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("M-d-yyyy").withZone(TRIP_PLANNER_ZONE)
private val TRIP_CLOCK_FORMAT: DateTimeFormatter =
    DateTimeFormatter.ofPattern("h:mm a").withZone(TRIP_PLANNER_ZONE)

/** Renders the trip request date (`M-d-yyyy`) in the Transit service's local zone. */
internal fun formatTripPlannerDate(instantMillis: Long): String =
    TRIP_DATE_FORMAT.format(Instant.ofEpochMilli(instantMillis))

/** Renders the trip request clock (`h:mm a`) in the Transit service's local zone. */
internal fun formatTripPlannerClock(instantMillis: Long): String =
    TRIP_CLOCK_FORMAT.format(Instant.ofEpochMilli(instantMillis))

/**
 * Assembles the tripplanner WS request URL from already-rendered pieces. Pure so the
 * parameter layout (including the tunable [TripRequestOptions]) is unit-testable without
 * Android framework calls; `fromPlace`/`toPlace`/`clock` arrive Uri-encoded by the caller.
 */
internal fun buildTripPlannerRequestUrl(
    baseUrl: String,
    apiKey: String,
    fromPlace: String,
    fromCoord: String,
    toPlace: String,
    toCoord: String,
    date: String,
    clock: String,
    arriveBy: Boolean,
    options: TripRequestOptions
): String = buildString {
    append(baseUrl)
    append("/fromPlace/").append(fromPlace)
    append("/fromCoord/").append(fromCoord)
    append("/toPlace/").append(toPlace)
    append("/toCoord/").append(toCoord)
    append("/date/").append(date)
    append("/time/").append(clock)
    append("/arr/").append(if (arriveBy) "A" else "D")
    append("/min/T")
    append("/mode/").append(options.mode.wsCode)
    append("/walk/").append(String.format(Locale.US, "%.1f", options.maxWalkMiles))
    // "maxIntineraries" (missing the second "i") is the parameter the live Trip Planner
    // WS actually expects; "correcting" it to maxItineraries makes the server silently
    // ignore the count. TripPlanUrlBuilderTest locks this spelling in.
    append("/maxIntineraries/").append(options.itineraryCount)
    append("/format/xml")
    append("/appID/").append(apiKey)
}