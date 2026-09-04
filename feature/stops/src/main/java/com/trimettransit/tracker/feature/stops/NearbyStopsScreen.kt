package com.trimettransit.tracker.feature.stops

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.NavState
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
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun NearbyStopsScreen(
    transitRepository: TransitRepository,
    onNavigateToArrivals: (Stop, Int) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionRequired = stringResource(R.string.location_permission_required)

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

    val listState = rememberLazyListState()

    // Collapsed bottom-bar pill: scroll the nearby-stops list back to the top.
    DisposableEffect(Unit) {
        NavState.onScrollToTop = {
            coroutineScope.launch { listState.animateScrollToItem(0) }
        }
        onDispose { NavState.onScrollToTop = null }
    }

    fun launchLoadNearbyStops() {
        loadJob?.cancel()
        val job = coroutineScope.launch {
            val me = coroutineContext[Job]!!
            loadNearbyStops(
                context = context,
                transitRepository = transitRepository,
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
            errorMessage = locationPermissionRequired
        }
    }

    // Shown once before the system permission dialog so users know why location is needed.
    var showLocationExplainer by remember { mutableStateOf(false) }
    if (showLocationExplainer) {
        AlertDialog(
            onDismissRequest = { showLocationExplainer = false },
            title = { Text(stringResource(R.string.use_your_location_question)) },
            text = {
                Text(
                    stringResource(R.string.location_explainer)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLocationExplainer = false
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) { Text(stringResource(R.string.continue_)) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationExplainer = false }) { Text(stringResource(R.string.not_now)) }
            }
        )
    }

    fun loadIfPermissionGranted() {
        if (locationPermissionGranted) {
            launchLoadNearbyStops()
        } else {
            errorMessage = locationPermissionRequired
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
            text = stringResource(R.string.nearby_stops_list_title),
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
                        Text(stringResource(R.string.loading))
                    }
                } else {
                    Text(stringResource(R.string.refresh))
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
                    LoadingState(message = stringResource(R.string.finding_nearby_stops))
                }
                1 -> {
                    ErrorState(
                        message = errorMessage ?: stringResource(R.string.unknown_error),
                        onRetry = { promptForPermissionAndLoad() }
                    )
                }
                2 -> {
                    EmptyState(
                        message = if (hasLoaded) stringResource(R.string.no_stops_found_nearby)
                                  else stringResource(R.string.tap_refresh_to_find)
                    )
                }
                3 -> {
                    ContentEntrance(modifier = Modifier.fillMaxSize()) {
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
                        message = stringResource(R.string.tap_refresh_using_location)
                    )
                }
            }
        }
    }
}

@android.annotation.SuppressLint("MissingPermission")
private suspend fun loadNearbyStops(
    context: Context,
    transitRepository: TransitRepository,
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
            setError(context.getString(R.string.location_services_disabled))
            return
        }

        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation) {
            setError(context.getString(R.string.location_permission_required_short))
            return
        }

        val location = withTimeoutOrNull(10_000L) { requestFreshLocation(locationManager) }
            ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        if (location == null) {
            setError(context.getString(R.string.unable_to_get_location))
            return
        }

        val stops = transitRepository.getStopsByLocation(
            ll = "${location.latitude},${location.longitude}",
            feet = 500,
            showRoutes = true
        )
        if (stops != null) {
            setStops(stops)
        } else {
            setError(context.getString(R.string.unable_to_find_nearby_stops))
        }
    } catch (e: CancellationException) {
        // A newer load superseded this one — don't paint an error for a cancelled fetch.
        throw e
    } catch (e: Exception) {
        setError(context.getString(R.string.unable_to_find_nearby_stops))
    } finally {
        // Only the current job may clear the loading state; a superseded job must not
        // clobber the newer load's spinner.
        if (isCurrent()) setLoading(false)
    }
}

/** Requests a fresh single fix; resumes with null if permission is missing. Uses the
 *  non-deprecated [LocationManager.getCurrentLocation] API, probing GPS then network. */
@android.annotation.SuppressLint("MissingPermission")
private suspend fun requestFreshLocation(locationManager: LocationManager): Location? {
    val executor = java.util.concurrent.Executors.newSingleThreadExecutor()
    return try {
        for (provider in listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )) {
            if (!locationManager.isProviderEnabled(provider)) continue
            val deferred = kotlinx.coroutines.CompletableDeferred<Location?>()
            val signal = android.os.CancellationSignal()
            try {
                locationManager.getCurrentLocation(provider, signal, executor) { location ->
                    deferred.complete(location)
                }
            } catch (e: SecurityException) {
                signal.cancel()
                deferred.complete(null)
            } catch (e: IllegalArgumentException) {
                signal.cancel()
                deferred.complete(null)
                continue
            }
            val fix = withTimeoutOrNull(10_000L) { deferred.await() }
            signal.cancel()
            if (fix != null) return fix
        }
        null
    } finally {
        executor.shutdown()
    }
}
