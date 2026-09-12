package com.trimettransit.tracker.transit

import com.trimettransit.tracker.model.TripRequestOptions
import java.util.Locale

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
    append("/maxIntineraries/").append(options.itineraryCount)
    append("/format/xml")
    append("/appID/").append(apiKey)
}