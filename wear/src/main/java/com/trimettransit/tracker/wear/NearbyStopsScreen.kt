package com.trimettransit.tracker.wear

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.trimettransit.tracker.R
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.transit.TransitRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface NearbyState {
    object MissingPermission : NearbyState
    object Loading : NearbyState
    object Error : NearbyState
    object Empty : NearbyState
    data class Content(val stops: List<Stop>, val lat: Double, val lng: Double) : NearbyState
}

/**
 * Location-based "what's near me" stop finder. Requests the fine-location permission once,
 * reads a fresh fix from the platform LocationManager (GPS→network), then lists stops within
 * 500 ft with their straight-line distance. Standalone — only the TriMet API is consulted.
 */
@Composable
fun NearbyStopsScreen(onStopClick: (Stop) -> Unit) {
    val context = LocalContext.current
    val transitRepository = remember { TransitRepositoryImpl(context.applicationContext) }
    var state by remember { mutableStateOf<NearbyState>(NearbyState.Loading) }
    val scope = rememberCoroutineScope()

    fun load() {
        scope.launch {
            state = NearbyState.Loading
            val fix = withContext(Dispatchers.IO) {
                wearRequestCurrentLocation(context)
            }
            if (fix == null) {
                state = if (hasLocationPermission(context)) NearbyState.Error
                else NearbyState.MissingPermission
                return@launch
            }
            val (lat, lng) = fix
            val stops = withContext(Dispatchers.IO) {
                transitRepository.getStopsByLocation("$lat,$lng", feet = 500)
            }
            if (stops == null) {
                state = NearbyState.Error
            } else if (stops.isEmpty()) {
                state = NearbyState.Empty
            } else {
                state = NearbyState.Content(stops, lat, lng)
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) load() else state = NearbyState.MissingPermission
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission(context)) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            load()
        }
    }

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = { ScrollIndicator(listState) }
    ) { contentPadding ->
        when (state) {
            is NearbyState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                    contentAlignment = Alignment.Center
                ) {
                    WearFadeInOnce { CircularProgressIndicator() }
                }
            }
            is NearbyState.MissingPermission -> {
                CenteredMessageWithAction(
                    message = stringResource(R.string.no_location_permission),
                    actionLabel = stringResource(R.string.grant_location_permission)
                ) { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
            }
            is NearbyState.Error -> {
                CenteredMessageWithAction(
                    message = stringResource(R.string.location_unavailable),
                    actionLabel = stringResource(R.string.retry)
                ) { load() }
            }
            is NearbyState.Empty -> {
                CenteredMessageWithAction(
                    message = stringResource(R.string.no_stops_nearby),
                    actionLabel = stringResource(R.string.retry)
                ) { load() }
            }
            is NearbyState.Content -> {
                val content = state as NearbyState.Content
                WearContentEntrance(modifier = Modifier.fillMaxSize()) {
                    TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
                        item {
                            ListHeader(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .transformedHeight(this, transformationSpec)
                                    .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                                transformation = SurfaceTransformation(transformationSpec)
                            ) { Text(stringResource(R.string.nearby_stops)) }
                        }
                        items(content.stops, key = { it.locId }) { stop ->
                            NearbyStopRow(
                                stop = stop,
                                lat = content.lat,
                                lng = content.lng,
                                onClick = { onStopClick(stop) },
                                modifier = Modifier
                                    .transformedHeight(this, transformationSpec)
                                    .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding),
                                transformation = SurfaceTransformation(transformationSpec)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NearbyStopRow(
    stop: Stop,
    lat: Double,
    lng: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    transformation: SurfaceTransformation
) {
    val meters = remember(stop.locId, lat, lng) {
        wearDistanceMeters(lat, lng, stop.latitude, stop.longitude).toInt()
    }
    val secondary = "${meters} m" + if (stop.dirDesc.isNotBlank()) {
        " · ${stop.dirDesc}"
    } else {
        ""
    }
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
        label = {
            Text(
                text = stop.desc,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        },
        secondaryLabel = {
            Text(
                text = secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Start
            )
        }
    )
}

@Composable
private fun CenteredMessageWithAction(
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        WearFadeInOnce {
            androidx.compose.foundation.layout.Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = message, textAlign = TextAlign.Center)
                Button(
                    onClick = onAction,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED