package com.trimettransit.tracker.model.domain

/**
 * How a screen should recover from a missing location permission. Distinguishing
 * [PERMANENTLY_DENIED] is what lets the UI offer a route to App Settings instead of
 * re-firing a system prompt Android no longer shows.
 */
enum class LocationPermissionState { GRANTED, PROMPTABLE, PERMANENTLY_DENIED }

/**
 * Pure permission-policy decision shared by the Nearby Stops and Trip Planner screens.
 *
 * Android stops showing the system prompt after a second denial; at that point
 * `shouldShowRequestPermissionRationale` is false and the only recovery is App Settings.
 * A coarse-only ("Approximate") grant is enough for a stops-nearby lookup, so it counts.
 */
fun locationPermissionState(
    fineGranted: Boolean,
    coarseGranted: Boolean,
    hasRequested: Boolean,
    shouldShowRationale: Boolean
): LocationPermissionState = when {
    fineGranted || coarseGranted -> LocationPermissionState.GRANTED
    !hasRequested -> LocationPermissionState.PROMPTABLE
    shouldShowRationale -> LocationPermissionState.PROMPTABLE
    else -> LocationPermissionState.PERMANENTLY_DENIED
}
