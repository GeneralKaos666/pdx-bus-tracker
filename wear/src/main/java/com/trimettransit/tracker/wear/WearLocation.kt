package com.trimettransit.tracker.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull

private const val LOCATION_FIX_TIMEOUT_MS = 10_000L

/**
 * Requests a fresh fix via LocationManager.getCurrentLocation, probing GPS then the network
 * provider, each with a bounded wait. Returns a (latitude, longitude) pair or null when
 * permission is missing or no provider answers in time. Portable to the watch because it relies
 * only on the platform LocationManager (no Google Play Services).
 */
@android.annotation.SuppressLint("MissingPermission")
internal suspend fun wearRequestCurrentLocation(context: Context): Pair<Double, Double>? {
    val hasFineLocation = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasFineLocation) return null
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
        ?: return null
    val executor = ContextCompat.getMainExecutor(context)
    for (provider in listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)) {
        val deferred = CompletableDeferred<Pair<Double, Double>?>()
        val signal = android.os.CancellationSignal()
        try {
            locationManager.getCurrentLocation(provider, signal, executor) { location ->
                deferred.complete(location?.let { it.latitude to it.longitude })
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
internal fun wearDistanceMeters(
    aLat: Double,
    aLng: Double,
    bLat: Double,
    bLng: Double
): Double {
    val results = FloatArray(1)
    Location.distanceBetween(aLat, aLng, bLat, bLng, results)
    return results[0].toDouble()
}