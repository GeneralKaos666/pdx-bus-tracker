package com.trimettransit.tracker.ui.screens.stops

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.ui.TransitApi
import com.trimettransit.tracker.ui.screens.components.EmptyState
import com.trimettransit.tracker.ui.screens.components.ErrorState
import com.trimettransit.tracker.ui.screens.components.LoadingState
import com.trimettransit.tracker.ui.screens.components.StopListItem
import com.trimettransit.tracker.ui.screens.components.rememberOnResume

import kotlinx.coroutines.launch

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
            loadNearbyStops(context, coroutineScope, stops, { stops = it }, { isLoading = it }, { errorMessage = it }, { hasLoaded = true })
        } else {
            errorMessage = "Location permission is required to find nearby stops"
        }
    }

    fun loadIfPermissionGranted() {
        if (locationPermissionGranted) {
            loadNearbyStops(context, coroutineScope, stops, { stops = it }, { isLoading = it }, { errorMessage = it }, { hasLoaded = true })
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Auto-load on first composition if permission already granted
    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted && !hasLoaded) {
            loadNearbyStops(context, coroutineScope, stops, { stops = it }, { isLoading = it }, { errorMessage = it }, { hasLoaded = true })
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
        FilledTonalButton(
            onClick = { loadIfPermissionGranted() },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "Loading..." else "Refresh")
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                LoadingState(message = "Finding nearby stops...")
            }
            errorMessage != null && stops == null -> {
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
                        OutlinedButton(onClick = { loadIfPermissionGranted() }) {
                            Text("Try Again")
                        }
                    }
                }
            }
            stops != null -> {
                val safeStops = stops!!
                if (safeStops.isEmpty()) {
                    EmptyState(
                        message = if (hasLoaded) "No stops found nearby"
                                  else "Tap Refresh to find nearby stops"
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(safeStops, key = { it.locId }) { stop ->
                            StopListItem(
                                stop = stop,
                                onClick = { onNavigateToArrivals(stop, -1) }
                            )
                        }
                    }
                }
            }
            !hasLoaded -> {
                EmptyState(
                    message = "Tap Refresh to find nearby stops using your current location"
                )
            }
        }
    }
}

private fun loadNearbyStops(
    context: Context,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    currentStops: List<Stop>?,
    setStops: (List<Stop>?) -> Unit,
    setLoading: (Boolean) -> Unit,
    setError: (String?) -> Unit,
    setHasLoaded: () -> Unit
) {
    if (currentStops != null && currentStops.isEmpty().not()) return  // already loaded
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

            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
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
