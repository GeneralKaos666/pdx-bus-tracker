package com.trimettransit.tracker.feature.trips

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import org.maplibre.android.geometry.LatLng

internal const val LOCATION_FIX_TIMEOUT_MS = 10_000L

/** Last-known fix from any provider, with the device-time timestamp of the fix; null when
 *  the device has no stored fix yet. Comparing the timestamp against the current time lets
 *  the screen refresh a stale cached fix instead of trusting it forever. */
@android.annotation.SuppressLint("MissingPermission")
internal fun readLastKnownLocation(context: Context): Pair<LatLng, Long>? {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFineLocation) return null
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val location = try {
        locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
    } catch (e: SecurityException) {
        // Permission revoked between check and call — treat as no stored fix.
        null
    } ?: return null
    return LatLng(location.latitude, location.longitude) to location.time
}

/**
 * Requests a fresh fix via LocationManager.getCurrentLocation, probing GPS then the network
 * provider, each with a bounded wait. getLastKnownLocation() is often null on devices with
 * no prior fix, so this is the path that actually produces location for the trip origin.
 */
@android.annotation.SuppressLint("MissingPermission")
internal suspend fun requestCurrentLocation(context: Context): LatLng? {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFineLocation) return null
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val executor = ContextCompat.getMainExecutor(context)
    for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
        val deferred = CompletableDeferred<LatLng?>()
        val signal = android.os.CancellationSignal()
        try {
            locationManager.getCurrentLocation(provider, signal, executor) { location ->
                deferred.complete(location?.let { LatLng(it.latitude, it.longitude) })
            }
        } catch (e: SecurityException) {
            signal.cancel()
            deferred.complete(null)
        } catch (e: IllegalArgumentException) {
            // Provider not present on this device — try the next one.
            signal.cancel()
            deferred.complete(null)
            continue
        }
        val fix = withTimeoutOrNull(LOCATION_FIX_TIMEOUT_MS) { deferred.await() }
        signal.cancel()
        if (fix != null) return fix
    }
    return null
}

/** Straight-line distance in meters between two points (WGS84). */
internal fun distanceMeters(a: LatLng, b: LatLng): Double {
    val results = FloatArray(1)
    Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, results)
    return results[0].toDouble()
}