package com.trimettransit.tracker.feature.trips

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripLeg
import com.trimettransit.tracker.model.TripPlan
import com.trimettransit.tracker.model.TripPlannerError
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripRequestTime
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.components.badgeBitmap
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.ui.components.searchStops
import com.trimettransit.tracker.ui.components.StopSearchItem
import com.trimettransit.tracker.ui.components.transitBadgeLetters
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.components.transitOnColor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.PropertyFactory.iconAllowOverlap
import org.maplibre.android.style.layers.PropertyFactory.iconAnchor
import org.maplibre.android.style.layers.PropertyFactory.iconIgnorePlacement
import org.maplibre.android.style.layers.PropertyFactory.iconImage
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.Calendar
import java.util.Locale

private const val TRIP_MAP_STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val TRIP_MAP_STYLE_URL_DARK = "https://tiles.openfreemap.org/styles/dark"
private const val PLAN_CAMERA_ZOOM = 14.0
private const val DEFAULT_ARRIVE_BY_ADVANCE_MS = 60L * 60_000L
private const val MAX_CAMERA_FIT_ATTEMPTS = 3

private enum class PickSlot { NONE, ORIGIN, DEST }

/**
 * Map-first from→to trip planner (the "Trips" tab). Tap the map (or search) to pick an
 * origin and destination, then plan; the resulting itinerary options and their legs are
 * drawn over the basemap from the TriMet Trip Planner WS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripPlannerScreen(
    transitRepository: TransitRepository,
    pageVisible: Boolean,
    isDark: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Resource labels captured at composition (they must not be read via LocalContext in
    // non-composable lambdas below, which wouldn't track configuration changes).
    val myLocationLabel = stringResource(R.string.my_location)
    val pinnedLocationLabel = stringResource(R.string.pinned_location)

    var origin by remember { mutableStateOf<TripPoint?>(null) }
    var dest by remember { mutableStateOf<TripPoint?>(null) }
    var picking by remember { mutableStateOf(PickSlot.NONE) }
    var pickerSlot by remember { mutableStateOf<PickSlot?>(null) }
    var showResults by remember { mutableStateOf(false) }

    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAskedPermission by remember { mutableStateOf(false) }
    var showLocationExplainer by remember { mutableStateOf(false) }
    var pendingMyLocationOrigin by remember { mutableStateOf(false) }

    var arriveBy by remember { mutableStateOf(false) }
    var arriveByTimeMillis by remember { mutableStateOf<Long?>(null) }

    var planResult by remember { mutableStateOf<TripPlanResult?>(null) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var isPlanning by remember { mutableStateOf(false) }
    var planJob by remember { mutableStateOf<Job?>(null) }
    var locationJob by remember { mutableStateOf<Job?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        locationPermissionGranted = granted
    }

    if (showLocationExplainer) {
        AlertDialog(
            onDismissRequest = { showLocationExplainer = false },
            title = { Text(stringResource(R.string.use_your_location_question)) },
            text = { Text(stringResource(R.string.location_explainer)) },
            confirmButton = {
                TextButton(onClick = {
                    showLocationExplainer = false
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }) { Text(stringResource(R.string.continue_)) }
            },
            dismissButton = {
                TextButton(onClick = { showLocationExplainer = false }) {
                    Text(stringResource(R.string.not_now))
                }
            }
        )
    }

    fun refreshLocation() {
        if (locationJob?.isActive == true) return
        locationJob = coroutineScope.launch {
            val fix = requestCurrentLocation(context)
            if (fix != null) myLocation = fix
        }
    }

    // Ask for location once, and only while this page is visible (the pager pre-composes
    // adjacent pages). The explainer dialog is shown before the system permission dialog.
    LaunchedEffect(pageVisible, locationPermissionGranted) {
        if (pageVisible && !locationPermissionGranted && !hasAskedPermission) {
            hasAskedPermission = true
            showLocationExplainer = true
        }
    }

    LaunchedEffect(locationPermissionGranted) {
        if (locationPermissionGranted) {
            val lastKnown = readLastKnownLocation(context)
            myLocation = lastKnown?.first ?: requestCurrentLocation(context)
        }
    }

    // A "Use my location" origin selection lands once a fix is available.
    LaunchedEffect(pendingMyLocationOrigin, myLocation) {
        if (pendingMyLocationOrigin) {
            val location = myLocation
            if (location != null) {
                origin = TripPoint(location.latitude, location.longitude, myLocationLabel)
                pendingMyLocationOrigin = false
            }
        }
    }

    fun useMyLocationAsOrigin() {
        if (locationPermissionGranted) {
            if (myLocation != null) {
                pendingMyLocationOrigin = true
            } else {
                pendingMyLocationOrigin = true
                refreshLocation()
            }
        } else {
            pendingMyLocationOrigin = true
            showLocationExplainer = true
        }
    }

    fun onMapTap(value: LatLng) {
        when (picking) {
            PickSlot.ORIGIN -> origin = TripPoint(value.latitude, value.longitude, pinnedLocationLabel)
            PickSlot.DEST -> dest = TripPoint(value.latitude, value.longitude, pinnedLocationLabel)
            PickSlot.NONE -> return
        }
        picking = PickSlot.NONE
    }

    fun planIt() {
        val from = origin ?: return
        val to = dest ?: return
        planJob?.cancel()
        planResult = null
        isPlanning = true
        planJob = coroutineScope.launch {
            val time = TripRequestTime(
                arriveBy = arriveBy,
                timeMillis = if (arriveBy) {
                    arriveByTimeMillis ?: (System.currentTimeMillis() + DEFAULT_ARRIVE_BY_ADVANCE_MS)
                } else null
            )
            val result = transitRepository.planTrip(from, to, time)
            val successPlan = (result as? TripPlanResult.Success)?.plan
            if (successPlan?.itineraries?.isNotEmpty() == true) {
                selectedIndex = 0
            }
            planResult = result
            showResults = result is TripPlanResult.Success &&
                successPlan?.itineraries?.isNotEmpty() == true
            isPlanning = false
        }
    }

    // Re-plan on app re-entry only if a plan already exists (keeps the map fresh without
    // surprising the user with a new request before they've picked anything).
    RememberOnResume {
        if (planResult != null && origin != null && dest != null) {
            planIt()
        }
    }

    val selectedItinerary: TripItinerary? =
        (planResult as? TripPlanResult.Success)?.plan?.itineraries?.getOrNull(selectedIndex)

    val resolvedError: String? = when (val result = planResult) {
        null -> null
        is TripPlanResult.Error -> tripPlannerErrorString(context, result.error)
        is TripPlanResult.Success -> {
            if (result.plan?.itineraries.isNullOrEmpty()) {
                stringResource(R.string.no_trips_found)
            } else {
                null
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        TripMap(
            origin = origin,
            dest = dest,
            itinerary = selectedItinerary,
            myLocation = myLocation,
            pickingActive = picking != PickSlot.NONE,
            onMapTap = { onMapTap(it) },
            isDark = isDark,
            pageVisible = pageVisible,
            modifier = Modifier.fillMaxSize()
        )

        // "Location permission is off" chip (mirrors the other location screens).
        AnimatedVisibility(
            visible = pageVisible && !locationPermissionGranted && hasAskedPermission,
            enter = fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(durationMillis = 250, easing = FastOutSlowInEasing)) { -it },
            exit = fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                slideOutVertically(tween(durationMillis = 180, easing = FastOutSlowInEasing)) { -it / 3 }
        ) {
            Surface(
                onClick = { showLocationExplainer = true },
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.location_permission_off),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Endpoint + scheduling card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shadowElevation = 6.dp,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.trip_planner_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    EndpointRow(
                        label = stringResource(R.string.origin_field_hint),
                        point = origin,
                        accentColor = MaterialTheme.colorScheme.primary,
                        onClick = {
                            pickerSlot = PickSlot.ORIGIN
                            picking = PickSlot.NONE
                        },
                        onClear = { origin = null; planResult = null }
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        val swapSource = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = {
                                val from = origin
                                origin = dest
                                dest = from
                                planResult = null
                            },
                            enabled = origin != null || dest != null,
                            interactionSource = swapSource,
                            modifier = Modifier
                                .size(28.dp)
                                .pressScale(swapSource)
                        ) {
                            Icon(
                                Icons.Default.SwapVert,
                                contentDescription = stringResource(R.string.swap_origin_destination),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    EndpointRow(
                        label = stringResource(R.string.destination_field_hint),
                        point = dest,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        onClick = {
                            pickerSlot = PickSlot.DEST
                            picking = PickSlot.NONE
                        },
                        onClear = { dest = null; planResult = null }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !arriveBy,
                            onClick = {
                                arriveBy = false
                                planResult = null
                            },
                            label = { Text(stringResource(R.string.depart_now)) }
                        )
                        FilterChip(
                            selected = arriveBy,
                            onClick = {
                                arriveBy = true
                                if (arriveByTimeMillis == null) {
                                    arriveByTimeMillis = System.currentTimeMillis() + DEFAULT_ARRIVE_BY_ADVANCE_MS
                                }
                                planResult = null
                            },
                            label = { Text(stringResource(R.string.arrive_by)) }
                        )
                        if (arriveBy) {
                            TextButton(onClick = {
                                val cal = Calendar.getInstance()
                                val initial = arriveByTimeMillis ?: (System.currentTimeMillis() + DEFAULT_ARRIVE_BY_ADVANCE_MS)
                                cal.timeInMillis = initial
                                TimePickerDialog(
                                    context,
                                    { _, hour, minute ->
                                        cal.set(Calendar.HOUR_OF_DAY, hour)
                                        cal.set(Calendar.MINUTE, minute)
                                        arriveByTimeMillis = cal.timeInMillis
                                        planResult = null
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY),
                                    cal.get(Calendar.MINUTE),
                                    false
                                ).show()
                            }) {
                                Text(
                                    DateTimeFormat.forPattern("h:mm a")
                                        .print(DateTime(arriveByTimeMillis ?: (System.currentTimeMillis() + DEFAULT_ARRIVE_BY_ADVANCE_MS)))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val planSource = remember { MutableInteractionSource() }
                    FilledTonalButton(
                        onClick = { planIt() },
                        enabled = origin != null && dest != null && !isPlanning,
                        interactionSource = planSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .pressScale(planSource)
                    ) {
                        Crossfade(
                            targetState = isPlanning,
                            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
                            label = "planButtonState"
                        ) { loading ->
                            if (loading) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.planning_trips))
                                }
                            } else {
                                Text(stringResource(R.string.find_trips))
                            }
                        }
                    }
                }
            }

            // Map-pin hint when a slot is awaiting a map tap.
            AnimatedVisibility(
                visible = picking != PickSlot.NONE,
                enter = fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                    slideInVertically(tween(durationMillis = 250, easing = FastOutSlowInEasing)) { -it },
                exit = fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutVertically(tween(durationMillis = 180, easing = FastOutSlowInEasing)) { -it / 3 }
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (picking == PickSlot.ORIGIN) {
                                stringResource(R.string.tap_map_to_set_origin)
                            } else {
                                stringResource(R.string.tap_map_to_set_destination)
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                        )
                        IconButton(
                            onClick = { picking = PickSlot.NONE },
                            modifier = Modifier.size(32.dp).pressScale(remember { MutableInteractionSource() })
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.cancel),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Inline plan status / error / empty result surface.
            val statusText = resolvedError
            if (statusText != null) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = if (resolvedError != null && planResult is TripPlanResult.Error) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (resolvedError != null && planResult is TripPlanResult.Error) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }

    // Endpoint picker sheet
    pickerSlot?.let { slot ->
        EndpointPickerSheet(
            slot = slot,
            transitRepository = transitRepository,
            onStopPicked = { stop ->
                val point = TripPoint(stop.latitude, stop.longitude, stop.desc)
                if (slot == PickSlot.ORIGIN) origin = point else dest = point
                planResult = null
                pickerSlot = null
            },
            onMyLocationPicked = {
                useMyLocationAsOrigin()
                pickerSlot = null
            },
            onMapPinPicked = {
                picking = slot
                pickerSlot = null
            },
            onDismiss = { pickerSlot = null }
        )
    }

    // Itinerary results sheet
    if (showResults) {
        val plan = (planResult as? TripPlanResult.Success)?.plan
        if (plan != null && plan.itineraries.isNotEmpty()) {
            ItineraryResultsSheet(
                plan = plan,
                selectedIndex = selectedIndex,
                onSelect = {
                    selectedIndex = it
                    picking = PickSlot.NONE
                },
                onDismiss = { showResults = false }
            )
        }
    }
}

@Composable
private fun EndpointRow(
    label: String,
    point: TripPoint?,
    accentColor: Color,
    onClick: () -> Unit,
    onClear: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source)
            .clickable(interactionSource = source, indication = LocalIndication.current, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(accentColor, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = point?.description?.takeIf { it.isNotBlank() } ?: label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (point != null) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (point != null) {
            IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = stringResource(R.string.clear),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EndpointPickerSheet(
    slot: PickSlot,
    transitRepository: TransitRepository,
    onStopPicked: (Stop) -> Unit,
    onMyLocationPicked: () -> Unit,
    onMapPinPicked: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tab by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Text(
            text = stringResource(
                if (slot == PickSlot.ORIGIN) R.string.add_origin else R.string.add_destination
            ),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        TabRow(selectedTabIndex = tab) {
            Tab(
                selected = tab == 0,
                onClick = { tab = 0 },
                text = { Text(stringResource(R.string.search_stops_tab)) }
            )
            Tab(
                selected = tab == 1,
                onClick = { tab = 1 },
                text = { Text(stringResource(R.string.map_pin_tab)) }
            )
        }

        Box(modifier = Modifier.heightIn(max = 480.dp)) {
            if (tab == 0) {
                StopSearchPanel(
                    transitRepository = transitRepository,
                    onStopClick = onStopPicked,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (slot == PickSlot.ORIGIN) {
                            stringResource(R.string.map_pin_origin_hint)
                        } else {
                            stringResource(R.string.map_pin_dest_hint)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalButton(onClick = onMapPinPicked) {
                        Text(stringResource(R.string.pick_on_map))
                    }
                }
            }
        }

        if (slot == PickSlot.ORIGIN) {
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            val myLocSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(myLocSource)
                    .clickable(
                        interactionSource = myLocSource,
                        indication = LocalIndication.current,
                        onClick = onMyLocationPicked
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.use_my_location),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StopSearchPanel(
    transitRepository: TransitRepository,
    onStopClick: (Stop) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    var allStops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Stop>>(emptyList()) }

    LaunchedEffect(allStops == null, query.isNotBlank()) {
        if (allStops == null && query.isNotBlank()) {
            isLoading = true
            allStops = withContext(Dispatchers.IO) { transitRepository.searchStops() }
            isLoading = false
        }
    }

    LaunchedEffect(query, allStops) {
        results = emptyList()
        if (query.isNotBlank() && allStops != null) {
            results = searchStops(allStops!!, query)
        }
    }

    Column(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp)
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (query.isBlank()) {
                        Text(
                            text = stringResource(R.string.search_stops_hint),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = modalSearchTextStyle(),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 44.dp, top = 14.dp, bottom = 14.dp, end = 12.dp)
                )
            }
        }

        when {
            isLoading && allStops == null && query.isNotBlank() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }
            allStops == null && query.isNotBlank() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_connection),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            query.isBlank() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.search_stops_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            results.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.no_stops_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            else -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp)
                ) {
                    items(results, key = { it.locId }, contentType = { "stopSearch" }) { stop ->
                        StopSearchItem(
                            stop = stop,
                            onClick = { onStopClick(stop) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        }
    }
}

/** Slim text style for the search input field. */
@Composable
private fun modalSearchTextStyle() =
    MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItineraryResultsSheet(
    plan: TripPlan,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val selected = plan.itineraries.getOrNull(selectedIndex)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                plan.itineraries.forEachIndexed { index, itinerary ->
                    val label = stringResource(
                        when (index % 3) {
                            0 -> R.string.itinerary_1
                            1 -> R.string.itinerary_2
                            else -> R.string.itinerary_3
                        }
                    )
                    FilterChip(
                        selected = selectedIndex == index,
                        onClick = { onSelect(index) },
                        label = { Text("$label · ${formatDurationMillis(itinerary.durationMillis)}") }
                    )
                }
            }

            if (selected != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                TripSummaryHeader(itinerary = selected, modifier = Modifier.padding(horizontal = 20.dp))
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(selected.legs, key = { it.hashCode() }, contentType = { "leg" }) { leg ->
                        LegRow(leg = leg)
                    }
                }
            }
        }
    }
}

@Composable
private fun TripSummaryHeader(itinerary: TripItinerary, modifier: Modifier = Modifier) {
    val timePattern = DateTimeFormat.forPattern("h:mm a")
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = timePattern.print(itinerary.departure),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = timePattern.print(itinerary.arrival),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = formatDurationMillis(itinerary.durationMillis),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (itinerary.numberOfTransfers > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = pluralStringResource(R.plurals.transfers_count, itinerary.numberOfTransfers, itinerary.numberOfTransfers),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            itinerary.legs.filter { !it.isWalk }
                .distinctBy { it.routeNumber to it.routeName }
                .forEach { leg ->
                    RouteBadge(leg = leg)
                    Spacer(modifier = Modifier.width(6.dp))
                }
            Text(
                text = contextWalkTransitSummary(itinerary),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
        itinerary.fare?.let { fare ->
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.fare_label, fare),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RouteBadge(leg: TripLeg) {
    val scheme = MaterialTheme.colorScheme
    val letter = leg.mode.transitTypeLetter()
    Surface(
        shape = CircleShape,
        color = transitColor(letter, scheme)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp)
        ) {
            Text(
                text = (leg.routeNumber?.takeIf { it.isNotEmpty() && letter == "B" }) ?: letter,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = transitOnColor(letter, scheme)
            )
        }
    }
}

@Composable
private fun LegRow(leg: TripLeg) {
    val scheme = MaterialTheme.colorScheme
    val timePattern = DateTimeFormat.forPattern("h:mm a")
    Row(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        if (leg.isWalk) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.DirectionsWalk,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.walk),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (leg.direction.isNotBlank()) {
                    Text(
                        text = leg.direction,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (leg.from.description.isNotBlank() && leg.to.description.isNotBlank()) {
                    Text(
                        text = stringResource(R.string.walk_between_fmt, leg.from.description, leg.to.description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            val letter = leg.mode.transitTypeLetter()
            RouteBadge(leg = leg)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = leg.routeName?.takeIf { it.isNotBlank() }
                        ?: leg.direction.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.route_label),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.leg_transit_fmt,
                        timePattern.print(leg.departure),
                        leg.from.description,
                        timePattern.print(leg.arrival),
                        leg.to.description
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (leg.stayOnBoard) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = stringResource(R.string.stay_on_board),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun contextWalkTransitSummary(itinerary: TripItinerary): String {
    val walk = formatDurationMillis(itinerary.walkTimeMillis)
    val transit = formatDurationMillis(itinerary.transitTimeMillis)
    return stringResource(R.string.walk_transit_fmt, walk, transit)
}

private fun tripPlannerErrorString(context: Context, error: TripPlannerError): String {
    return when (error) {
        TripPlannerError.NO_STOPS_NEAR_ORIGIN,
        TripPlannerError.NO_STOPS_NEAR_DESTINATION -> context.getString(R.string.trip_planner_error_no_stops)
        TripPlannerError.NO_SERVICE_AT_ORIGIN,
        TripPlannerError.NO_SERVICE_AT_DESTINATION -> context.getString(R.string.trip_planner_error_no_service)
        TripPlannerError.TRIP_NOT_POSSIBLE -> context.getString(R.string.trip_planner_error_not_possible)
        TripPlannerError.TRIVIAL_DISTANCE -> context.getString(R.string.trip_planner_error_trivial)
        TripPlannerError.AMBIGUOUS_ORIGIN -> context.getString(R.string.trip_planner_error_ambiguous_origin)
        TripPlannerError.AMBIGUOUS_DESTINATION -> context.getString(R.string.trip_planner_error_ambiguous_destination)
        TripPlannerError.ORIGIN_NOT_FOUND,
        TripPlannerError.DESTINATION_NOT_FOUND -> context.getString(R.string.trip_planner_error_not_found)
        TripPlannerError.OUTSIDE_DISTRICT -> context.getString(R.string.trip_planner_error_outside_district)
        TripPlannerError.SYSTEM_OUTAGE -> context.getString(R.string.trip_planner_error_outage)
        TripPlannerError.UNKNOWN -> context.getString(R.string.trip_planner_error_unknown)
    }
}

private fun formatDurationMillis(ms: Long): String {
    val totalMin = (ms / 60_000L).coerceAtLeast(0)
    val h = totalMin / 60
    val m = totalMin % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

/**
 * MapLibre view for the trip planner: origin/destination markers, route "stick" lines
 * (solid transit, dashed walk), boarding badges, the me-dot, and map-tap pin dropping.
 */
@Composable
private fun TripMap(
    origin: TripPoint?,
    dest: TripPoint?,
    itinerary: TripItinerary?,
    myLocation: LatLng?,
    pickingActive: Boolean,
    onMapTap: (LatLng) -> Unit,
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    pageVisible: Boolean = true
) {
    val currentOnMapTap by rememberUpdatedState(onMapTap)
    val currentPickingActive by rememberUpdatedState(pickingActive)
    val mapState = remember { TripMapState() }
    val fitSize = remember { intArrayOf(-1, -1) }
    val density = LocalDensity.current.density
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val mapStyleUrl = if (isDark) TRIP_MAP_STYLE_URL_DARK else TRIP_MAP_STYLE_URL
    var appliedStyleUrl by remember { mutableStateOf<String?>(null) }

    fun applyTripStyle(style: Style) {
        val letters = transitBadgeLetters()
        letters.forEach { letter ->
            style.addImage(
                "badge-$letter",
                badgeBitmap(
                    context,
                    transitColor(letter, scheme).toArgb(),
                    transitIconResource(letter),
                    density,
                    transitOnColor(letter, scheme).toArgb()
                )
            )
        }
        mapState.letterColors = letters.associateWith {
            String.format(Locale.US, "#%06X", 0xFFFFFF and transitColor(it, scheme).toArgb())
        }
        style.addImage(
            "origin-dot",
            originDotBitmap(transitColor("B", scheme).toArgb(), density)
        )
        style.addImage(
            "dest-dot",
            destDotBitmap(transitColor("R", scheme).toArgb(), density)
        )
        style.addImage("stop-dot", stopDotBitmap(scheme.secondary.toArgb(), scheme.onSecondary.toArgb(), density))
        style.addImage("me-dot", meDotBitmap(scheme.primary.toArgb(), density))

        fun addSource(name: String): GeoJsonSource {
            val source = GeoJsonSource(name)
            style.addSource(source)
            return source
        }

        // Transit stick lines: color driven per-feature from the badge-letter color.
        mapState.transitSource = addSource("transit-source")
        style.addLayer(
            LineLayer("transit-layer", "transit-source").withProperties(
                PropertyFactory.lineColor(Expression.get("color")),
                PropertyFactory.lineWidth(4f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND)
            )
        )
        // Walk segments: dashed outline-colored line.
        mapState.walkSource = addSource("walk-source")
        style.addLayer(
            LineLayer("walk-layer", "walk-source").withProperties(
                PropertyFactory.lineColor(scheme.outline.toArgb()),
                PropertyFactory.lineWidth(3f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineDasharray(arrayOf(2f, 2f))
            )
        )
        // Boarding/alighting dots and route badges.
        mapState.stopSource = addSource("stop-source")
        style.addLayer(
            SymbolLayer("stop-layer", "stop-source").withProperties(
                iconImage("stop-dot"),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
        mapState.boardSource = addSource("board-source")
        style.addLayer(
            SymbolLayer("board-layer", "board-source").withProperties(
                iconImage(Expression.get("icon")),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
        mapState.originSource = addSource("origin-source")
        style.addLayer(
            SymbolLayer("origin-layer", "origin-source").withProperties(
                iconImage("origin-dot"),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
        mapState.destSource = addSource("dest-source")
        style.addLayer(
            SymbolLayer("dest-layer", "dest-source").withProperties(
                iconImage("dest-dot"),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
        mapState.meSource = addSource("me-source")
        style.addLayer(
            SymbolLayer("me-layer", "me-source").withProperties(
                iconImage("me-dot"),
                iconAnchor(Property.ICON_ANCHOR_CENTER),
                iconAllowOverlap(true),
                iconIgnorePlacement(true)
            )
        )
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                getMapAsync { map ->
                    mapState.map = map
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isAttributionEnabled = true
                    map.setMaxZoomPreference(18.0)
                    map.setStyle(mapStyleUrl) { style ->
                        applyTripStyle(style)
                        appliedStyleUrl = mapStyleUrl
                        mapState.push(origin, dest, itinerary)
                        map.moveCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(45.5189, -122.6795), 12.0
                            )
                        )
                    }
                    map.addOnMapClickListener { latLng ->
                        if (currentPickingActive) {
                            currentOnMapTap(latLng)
                            true
                        } else {
                            false
                        }
                    }
                }
                setOnTouchListener { v, event ->
                    if (event.pointerCount < 2) {
                        v.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                    if (event.actionMasked == MotionEvent.ACTION_UP) {
                        v.performClick()
                    }
                    false
                }
                post { onStart() }
                mapState.mapView = this
            }
        },
        update = { view ->
            view.onStart()
            view.onResume()
            val vmap = mapState.map
            if (vmap != null && appliedStyleUrl != mapStyleUrl) {
                appliedStyleUrl = mapStyleUrl
                vmap.setStyle(mapStyleUrl) { style ->
                    applyTripStyle(style)
                    mapState.push(origin, dest, itinerary)
                }
            }
            mapState.push(origin, dest, itinerary)
            val location = myLocation
            if (location != null) {
                mapState.applyMe(location.latitude, location.longitude)
            }
            fitPlanCameraIfReady(view, mapState, origin, dest, itinerary, fitSize)
        },
        modifier = modifier
    )

    DisposableEffect(Unit) {
        onDispose {
            mapState.mapView?.onStop()
            mapState.mapView?.onPause()
            mapState.mapView?.onDestroy()
            mapState.map = null
            mapState.mapView = null
        }
    }
}

/** Fits the camera to the current plan once the viewport size has settled. */
private fun fitPlanCameraIfReady(
    view: MapView,
    state: TripMapState,
    origin: TripPoint?,
    dest: TripPoint?,
    itinerary: TripItinerary?,
    fitSize: IntArray,
    attempts: Int = 0
) {
    val map = state.map ?: return
    val points = buildList {
        origin?.let { add(LatLng(it.latitude, it.longitude)) }
        dest?.let { add(LatLng(it.latitude, it.longitude)) }
        itinerary?.legs?.forEach { leg ->
            if (leg.from.latitude != 0.0 || leg.from.longitude != 0.0) {
                add(LatLng(leg.from.latitude, leg.from.longitude))
            }
            if (leg.to.latitude != 0.0 || leg.to.longitude != 0.0) {
                add(LatLng(leg.to.latitude, leg.to.longitude))
            }
        }
    }
    if (points.isEmpty()) return

    val settled = view.width > 0 && view.height > 0 &&
        view.width == fitSize[0] && view.height == fitSize[1]
    if (!settled) {
        fitSize[0] = view.width
        fitSize[1] = view.height
        if (attempts >= MAX_CAMERA_FIT_ATTEMPTS) return
        view.postDelayed({
            if (view.isAttachedToWindow) {
                fitPlanCameraIfReady(view, state, origin, dest, itinerary, fitSize, attempts + 1)
            }
        }, 150)
        return
    }

    if (points.size == 1) {
        map.easeCamera(
            CameraUpdateFactory.newLatLngZoom(points.first(), PLAN_CAMERA_ZOOM), 400
        )
        return
    }
    val bounds = LatLngBounds.from(
        points.maxOf { it.latitude }, points.maxOf { it.longitude },
        points.minOf { it.latitude }, points.minOf { it.longitude }
    )
    val cam = map.getCameraForLatLngBounds(bounds, intArrayOf(96, 180, 96, 96))
    if (cam == null) {
        map.easeCamera(
            CameraUpdateFactory.newLatLngZoom(points.first(), PLAN_CAMERA_ZOOM), 400
        )
    } else {
        map.easeCamera(CameraUpdateFactory.newCameraPosition(cam), 400)
    }
}