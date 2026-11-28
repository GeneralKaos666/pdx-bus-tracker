package com.trimettransit.tracker.transit

/** Pure URL builders for the TriMet WS endpoints, kept apart from transport. */
/**
 * Assembles the `/ws/v2/alerts` request URL. Pure and unit-testable, mirroring
 * the other URL builders' convention of path-segment filters.
 */
internal fun buildAlertsUrl(
    baseUrl: String,
    apiKey: String,
    routes: List<Int>? = null,
    locIds: List<Int>? = null
): String = buildString {
    append(baseUrl)
    append("/appID/").append(apiKey)
    if (!routes.isNullOrEmpty()) append("/routes/").append(routes.joinToString(","))
    if (!locIds.isNullOrEmpty()) append("/locIDs/").append(locIds.joinToString(","))
}

/**
 * Assembles the `/ws/v2/stops` request URL. Pure so the opt-in arrivals-embedding
 * layout is unit-testable without Android framework calls; `ll`/`bbox` arrive already
 * Uri-encoded by the caller, matching [buildAlertsUrl]'s convention. `embedArrivals`,
 * `maxStopArrivals`, `minutes`, and `maxArrivals` are only appended when `includeArrivals`
 * is true, so a non-combined caller's URL is byte-identical to before this parameter
 * existed.
 */
internal fun buildStopsByLocationUrl(
    baseUrl: String,
    apiKey: String,
    ll: String,
    feet: Int? = null,
    meters: Int? = null,
    bbox: String? = null,
    maxStops: Int? = null,
    showRoutes: Boolean = true,
    includeArrivals: Boolean = false,
    embedArrivals: Boolean = true,
    maxStopArrivals: Int? = null,
    showRouteDirs: Boolean = false,
    minutes: Int? = null,
    maxArrivals: Int? = null
): String = buildString {
    append(baseUrl)
    append("/appID/").append(apiKey)
    append("/ll/").append(ll)
    if (feet != null) append("/feet/").append(feet)
    if (meters != null) append("/meters/").append(meters)
    if (!bbox.isNullOrEmpty()) append("/bbox/").append(bbox)
    if (maxStops != null) append("/maxStops/").append(maxStops)
    if (showRoutes) append("/showRoutes/true")
    if (includeArrivals) {
        if (embedArrivals) append("/embedArrivals/true")
        if (maxStopArrivals != null) append("/maxStopArrivals/").append(maxStopArrivals)
        if (minutes != null) append("/minutes/").append(minutes)
        if (maxArrivals != null) append("/arrivals/").append(maxArrivals)
    }
    if (showRouteDirs) append("/showRouteDirs/true")
}

internal fun encodePathSegment(raw: String): String =
    buildString(raw.length) {
        for (ch in raw) {
            if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' ||
                ch == '-' || ch == '.' || ch == '_' || ch == '~'
            ) {
                append(ch)
            } else {
                val bytes = ch.toString().toByteArray(Charsets.UTF_8)
                for (b in bytes) {
                    append('%')
                    append(Character.forDigit((b.toInt() shr 4) and 0xF, 16).uppercaseChar())
                    append(Character.forDigit(b.toInt() and 0xF, 16).uppercaseChar())
                }
            }
        }
    }

internal fun buildTripStatusUrl(
    baseUrl: String,
    apiKey: String,
    tripIds: List<String>? = null,
    blockIds: List<Int>? = null,
    showRoutes: Boolean = true,
    showStops: Boolean = true
): String = buildString {
    append(baseUrl).append("/appID/").append(apiKey)
    if (!tripIds.isNullOrEmpty()) append("/tripIDs/").append(tripIds.joinToString(",") { encodePathSegment(it) })
    if (!blockIds.isNullOrEmpty()) append("/blockIDs/").append(blockIds.joinToString(","))
    if (showRoutes) append("/showRoutes/true")
    if (showStops) append("/showStops/true")
}

