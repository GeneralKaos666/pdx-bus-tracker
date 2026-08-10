package com.trimettransit.tracker.feature.stops

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Bundle
import java.util.concurrent.Executor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.transit.TransitApi
import com.trimettransit.tracker.ui.components.EmptyState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.LoadingState
import com.trimettransit.tracker.ui.components.StopListItem
import com.trimettransit.tracker.ui.components.rememberOnResume
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior

import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

@Composable
fun NearbyStopsScreen(
    onNavigateToArrivals: (Stop, Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var stops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasLoaded by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
        if (granted) {
            loadNearbyStops(context, coroutineScope, { stops = it }, { isLoading = it }, { errorMessage = it }, { hasLoaded = true })
        } else {
            errorMessage = "Location permission is required to find nearby stops"
        }
    }

    fun loadIfPermissionGranted() {
        if (locationPermissionGranted) {
            loadNearbyStops(context, coroutineScope, { stops = it }, { isLoading = it }, { errorMessage = it }, { hasLoaded = true })
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Auto-load on first composition if permission already granted
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted && !hasLoaded) {
            loadNearbyStops(context, coroutineScope, { stops = it }, { isLoading = it }, { errorMessage = it }, { hasLoaded = true })
        }
    }

    // Re-fetch on app re-entry
    rememberOnResume {
        if (hasLoaded) {
            stops = null
            loadIfPermissionGranted()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Nearby Stops",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Refresh button
        val refreshSource = remember { MutableInteractionSource() }
        FilledTonalButton(
            onClick = { loadIfPermissionGranted() },
            enabled = !isLoading,
            interactionSource = refreshSource,
            modifier = Modifier.fillMaxWidth().pressScale(refreshSource)
        ) {
            Text(if (isLoading) "Loading..." else "Refresh")
        }

        Spacer(modifier = Modifier.height(12.dp))

        val safeStops = stops
        Crossfade(
            targetState = when {
                isLoading -> 0
                errorMessage != null && stops == null -> 1
                stops != null && safeStops?.isEmpty() == true -> 2
                stops != null -> 3
                else -> 4
            },
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
            label = "nearbyState"
        ) { state ->
            when (state) {
                0 -> {
                    LoadingState(message = "Finding nearby stops...")
                }
                1 -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = errorMessage ?: "Unknown error",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            val retrySource = remember { MutableInteractionSource() }
                            OutlinedButton(
                                onClick = { loadIfPermissionGranted() },
                                interactionSource = retrySource,
                                modifier = Modifier.pressScale(retrySource)
                            ) {
                                Text("Try Again")
                            }
                        }
                    }
                }
                2 -> {
                    EmptyState(
                        message = if (hasLoaded) "No stops found nearby"
                                  else "Tap Refresh to find nearby stops"
                    )
                }
                3 -> {
                    ContentEntrance(modifier = Modifier.fillMaxSize()) {
                    val smoothFling = rememberSmoothFlingBehavior()
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        flingBehavior = smoothFling,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(safeStops ?: emptyList(), key = { it.locId }, contentType = { "stop" }) { stop ->
                            StopListItem(
                                stop = stop,
                                onClick = { onNavigateToArrivals(stop, -1) },
                                modifier = Modifier.animateItem()
                            )
                        }
                    }
                    }
                }
                else -> {
                    EmptyState(
                        message = "Tap Refresh to find nearby stops using your current location"
                    )
                }
            }
        }
    }
}

private fun loadNearbyStops(
    context: Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    setStops: (List<Stop>?) -> Unit,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    setHasLoaded: () -> Unit
) {
    setLoading(true)
    setError(null)
    setHasLoaded()

    coroutineScope.launch {
        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            if (!hasGps) {
                setError("GPS is disabled.\nPlease enable location services.")
                setLoading(false)
                return@launch
            }

            val hasFineLocation = ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasFineLocation) {
                setError("Location permission is required.")
                setLoading(false)
                return@launch
            }

            val location = withTimeoutOrNull(10_000L) { requestFreshLocation(locationManager) }
                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (location == null) {
                setError("Unable to get current location.\nPlease try again.")
                setLoading(false)
                return@launch
            }

            val stops = TransitApi.fetchStopsByLocation(
                context = context,
                ll = "${location.latitude},${location.longitude}",
                feet = 500,
                showRoutes = true
            )
            if (stops != null) {
                setStops(stops)
            } else {
                setError("Unable to find nearby stops.\nPlease try again.")
            }
        } catch (e: Exception) {
            setError("Unable to find nearby stops.\nPlease try again.")
        } finally {
            setLoading(false)
        }
    }
}

/** Requests a fresh single fix; resumes with null if permission is missing. */
private suspend fun requestFreshLocation(locationManager: LocationManager): Location? =
    suspendCancellableCoroutine { cont ->
        if (cont.isCancelled) return@suspendCancellableCoroutine
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (cont.isActive) cont.resume(location)
            }

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

            @Deprecated("Deprecated in Java")
            override fun onProviderEnabled(provider: String) {}

            @Deprecated("Deprecated in Java")
            override fun onProviderDisabled(provider: String) {}
        }
        val request = LocationRequest.Builder(0L)
            .setQuality(LocationRequest.QUALITY_HIGH_ACCURACY)
            .build()
        val executor = Executor { command -> command.run() }
        val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            LocationManager.GPS_PROVIDER
        } else {
            LocationManager.NETWORK_PROVIDER
        }
        try {
            locationManager.requestLocationUpdates(provider, request, executor, listener)
        } catch (e: SecurityException) {
            if (cont.isActive) cont.resume(null)
            return@suspendCancellableCoroutine
        }
        cont.invokeOnCancellation {
            locationManager.removeUpdates(listener)
        }
    }
