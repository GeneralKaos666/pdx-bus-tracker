package com.trimettransit.tracker.feature.stops

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.animation.Crossfade
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.domain.LocationPermissionState
import com.trimettransit.tracker.model.domain.displayTimeMillis
import com.trimettransit.tracker.model.domain.locationPermissionState
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.util.minutesUntil
import com.trimettransit.tracker.ui.components.EmptyState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.LoadingState
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.StopListItem
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.util.SingleJobRunner

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@Composable
fun NearbyStopsScreen(
    transitRepository: TransitRepository,
    onNavigateToArrivals: (Stop, Int) -> Unit,
    onRegisterScrollToTop: ((() -> Unit)?) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val locationPermissionRequired = stringResource(R.string.location_permission_required)

    val activity = LocalActivity.current
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }

    fun readPermissionState(): LocationPermissionState = locationPermissionState(
        fineGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED,
        coarseGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED,
        hasRequested = hasRequestedPermission,
        shouldShowRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    var permissionState by remember { mutableStateOf(readPermissionState()) }
    val locationPermissionGranted = permissionState == LocationPermissionState.GRANTED
    var stops by remember { mutableStateOf<List<Stop>?>(null) }
    // Next-arrival preview per stop, populated only when the combined fast-path fetch
    // (getStopsWithArrivals) succeeds; empty on the plain getStopsByLocation fallback.
    var arrivalsByStop by remember { mutableStateOf<Map<Int, List<Arrival>>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasLoaded by remember { mutableStateOf(false) }
    // In-flight load, deduped so resume/re-entry can't stack overlapping fetches.
    val runner = remember { SingleJobRunner(coroutineScope) }

    val listState = rememberLazyListState()

    // Collapsed bottom-bar pill: scroll the nearby-stops list back to the top.
    DisposableEffect(Unit) {
        onRegisterScrollToTop {
            coroutineScope.launch { listState.animateScrollToItem(0) }
        }
        onDispose { onRegisterScrollToTop(null) }
    }

    fun launchLoadNearbyStops() {
        runner.launchWithJob { job ->
            loadNearbyStops(
                context = context,
                transitRepository = transitRepository,
                isCurrent = { runner.isCurrent(job) },
                setStops = { stops = it },
                setArrivalsByStop = { arrivalsByStop = it },
                setLoading = { isLoading = it },
                setError = { errorMessage = it },
                setHasLoaded = { hasLoaded = true }
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasRequestedPermission = true
        permissionState = readPermissionState()
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

    // Explicit user action (Refresh/Try Again buttons). Permanently denied means the system
    // prompt will not appear again, so send the user to App Settings instead of a no-op.
    fun promptForPermissionAndLoad() {
        when (permissionState) {
            LocationPermissionState.GRANTED -> launchLoadNearbyStops()
            LocationPermissionState.PERMANENTLY_DENIED -> openAppSettings(context)
            LocationPermissionState.PROMPTABLE -> showLocationExplainer = true
        }
    }

    // Auto-load on first composition if permission already granted
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted && !hasLoaded) {
            launchLoadNearbyStops()
        }
    }

    // Re-fetch on app re-entry; keep the last-known list on screen while refreshing.
    // Re-read the permission first: the user may have changed it in system Settings while away.
    RememberOnResume {
        permissionState = readPermissionState()
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
                animationSpec = m3EffectsDefault(),
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
            animationSpec = m3EffectsDefault(),
            label = "nearbyState"
        ) { state ->
            when (state) {
                0 -> {
                    LoadingState(message = stringResource(R.string.finding_nearby_stops))
                }
                1 -> {
                    val permanentlyDenied = permissionState == LocationPermissionState.PERMANENTLY_DENIED
                    ErrorState(
                        message = errorMessage ?: stringResource(R.string.unknown_error),
                        onRetry = { promptForPermissionAndLoad() },
                        retryLabel = if (permanentlyDenied) {
                            stringResource(R.string.open_app_settings)
                        } else {
                            null
                        }
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
                            contentPadding = PaddingValues(bottom = navPillBottomPadding()),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(safeStops ?: emptyList(), key = { it.locId }, contentType = { "stop" }) { stop ->
                                StopListItem(
                                    stop = stop,
                                    onClick = { onNavigateToArrivals(stop, -1) },
                                    modifier = Modifier.animateItem(),
                                    trailingContent = nextArrivalPreview(arrivalsByStop[stop.locId])
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

/** Nearby-stops fast-path tuning: modest window since this is only a list preview. */
private const val NEARBY_ARRIVALS_MINUTES = 30
private const val NEARBY_MAX_ARRIVALS_PER_STOP = 1

@android.annotation.SuppressLint("MissingPermission")
private suspend fun loadNearbyStops(
    context: Context,
    transitRepository: TransitRepository,
    isCurrent: () -> Boolean,
    setStops: (List<Stop>?) -> Unit,
    setArrivalsByStop: (Map<Int, List<Arrival>>) -> Unit,
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
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasFineLocation && !hasCoarseLocation) {
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

        val ll = "${location.latitude},${location.longitude}"

        // Fast path: stops + their next arrival(s) in one request. Route directions
        // are already embedded on each stop's routes, so no extra per-route directions
        // fetch is needed either way.
        val combined = transitRepository.getStopsWithArrivals(
            ll = ll,
            feet = 500,
            showRoutes = true,
            showRouteDirs = true,
            maxStopArrivals = 25,
            minutes = NEARBY_ARRIVALS_MINUTES,
            maxArrivals = NEARBY_MAX_ARRIVALS_PER_STOP
        )
        if (combined != null) {
            setStops(combined.stops)
            setArrivalsByStop(combined.arrivalsByStop)
            return
        }

        // Fallback: the combined endpoint failed/unavailable — get stops alone, with
        // no arrival preview rather than failing the whole screen.
        val stops = transitRepository.getStopsByLocation(
            ll = ll,
            feet = 500,
            showRoutes = true
        )
        if (stops != null) {
            setStops(stops)
            setArrivalsByStop(emptyMap())
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

/**
 * Trailing "next arrival in N min" preview for a nearby-stops row, built from the
 * fast-path's embedded arrivals. Returns null (no trailing content) when there is
 * nothing to show, matching [StopListItem]'s optional `trailingContent` contract.
 */
private fun nextArrivalPreview(arrivals: List<Arrival>?): (@Composable () -> Unit)? {
    val soonest = arrivals?.minByOrNull { it.displayTimeMillis } ?: return null
    if (soonest.displayTimeMillis <= 0L) return null
    return {
        val minutes = minutesUntil(soonest.displayTimeMillis).coerceAtLeast(0L).toInt()
        Text(
            text = pluralStringResource(R.plurals.nearby_stop_next_arrival_minutes, minutes, minutes),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
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

/** Opens this app's system Settings page, where a permanently-denied permission can be re-granted. */
private fun openAppSettings(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", context.packageName, null)
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
