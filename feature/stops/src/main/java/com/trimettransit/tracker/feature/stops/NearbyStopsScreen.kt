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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
    // In-flight load, deduped so resume/re-entry can't stack overlapping fetches.
    var loadJob by remember { mutableStateOf<Job?>(null) }

    fun launchLoadNearbyStops() {
        loadJob?.cancel()
        val job = coroutineScope.launch {
            val me = coroutineContext[Job]!!
            loadNearbyStops(
                context = context,
                isCurrent = { loadJob == me },
                setStops = { stops = it },
                setLoading = { isLoading = it },
                setError = { errorMessage = it },
                setHasLoaded = { hasLoaded = true }
            )
        }
        loadJob = job
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
        if (granted) {
            launchLoadNearbyStops()
        } else {
            errorMessage = "Location permission is required to find nearby stops"
        }
    }

    // Shown once before the system permission dialog so users know why location is needed.
    var showLocationExplainer by remember { mutableStateOf(false) }
    if (showLocationExplainer) {
        AlertDialog(
            onDismissRequest = { showLocationExplainer = false },
            title = { Text("Use your location?") },
            text = {
                Text(
                    "PDX Bus Tracker uses your location only when you ask it to find TriMet " +
                        "stops near you. Your location is never stored by this app and is not " +
                        "used for anything else."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLocationExplainer = false
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showLocationExplainer = false }) { Text("Not now") }
            }
        )
    }

    fun loadIfPermissionGranted() {
        if (locationPermissionGranted) {
            launchLoadNearbyStops()
        } else {
            errorMessage = "Location permission is required to find nearby stops"
        }
    }

    // Explicit user action (Refresh/Try Again buttons): always allowed to prompt.
    fun promptForPermissionAndLoad() {
        if (locationPermissionGranted) {
            launchLoadNearbyStops()
        } else {
            showLocationExplainer = true
        }
    }

    // Auto-load on first composition if permission already granted
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted && !hasLoaded) {
            launchLoadNearbyStops()
        }
    }

    // Re-fetch on app re-entry; keep the last-known list on screen while refreshing
    RememberOnResume {
        if (hasLoaded) {
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
            onClick = { promptForPermissionAndLoad() },
            enabled = !isLoading,
            interactionSource = refreshSource,
            modifier = Modifier.fillMaxWidth().pressScale(refreshSource)
        ) {
            Crossfade(
                targetState = isLoading,
                animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                label = "refreshButtonState"
            ) { loading ->
                if (loading) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Loading...")
                    }
                } else {
                    Text("Refresh")
                }
            }
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
                                onClick = { promptForPermissionAndLoad() },
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
                        val listState = rememberLazyListState()
                        val smoothFling = rememberSmoothFlingBehavior()
                        LazyColumn(
                            state = listState,
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

@android.annotation.SuppressLint("MissingPermission")
private suspend fun loadNearbyStops(
    context: Context,
    isCurrent: () -> Boolean,
    setStops: (List<Stop>?) -> Unit,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    setHasLoaded: () -> Unit
) {
    setLoading(true)
    setError(null)
    setHasLoaded()

    try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val hasGps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val hasNetwork = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!hasGps && !hasNetwork) {
            setError("Location services are disabled.\nPlease enable location services.")
            return
        }

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation) {
            setError("Location permission is required.")
            return
        }

        val location = withTimeoutOrNull(10_000L) { requestFreshLocation(locationManager) }
            ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location == null) {
            setError("Unable to get current location.\nPlease try again.")
            return
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
    } catch (e: CancellationException) {
        // A newer load superseded this one — don't paint an error for a cancelled fetch.
        throw e
    } catch (e: Exception) {
        setError("Unable to find nearby stops.\nPlease try again.")
    } finally {
        // Only the current job may clear the loading state; a superseded job must not
        // clobber the newer load's spinner.
        if (isCurrent()) setLoading(false)
    }
}

/** Requests a fresh single fix; resumes with null if permission is missing. */
@android.annotation.SuppressLint("MissingPermission")
private suspend fun requestFreshLocation(locationManager: LocationManager): Location? {
    var updatesRemoved = false
    fun removeUpdatesIfNeeded(listener: LocationListener) {
        if (!updatesRemoved) {
            updatesRemoved = true
            locationManager.removeUpdates(listener)
        }
    }
    return suspendCancellableCoroutine { cont ->
        if (cont.isCancelled) return@suspendCancellableCoroutine
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (cont.isActive) {
                    cont.resume(location)
                    // Success path: drop the one-shot registration so we don't leak a
                    // live GPS/network listener (and battery drain) per refresh.
                    removeUpdatesIfNeeded(this)
                }
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
            removeUpdatesIfNeeded(listener)
        }
    }
}
