package com.trimettransit.tracker.feature.trips

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripPlannerError
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripRequestTime
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.util.SingleJobRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.maplibre.android.geometry.LatLng
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.util.Calendar
private const val DEFAULT_ARRIVE_BY_ADVANCE_MS = 60L * 60_000L
/** Saves a trip endpoint across configuration changes (rotation/process death). */
private val tripPointSaver = listSaver<TripPoint?, Any>(
    save = {
        it?.let { point -> listOf(point.latitude, point.longitude, point.description) }
            ?: emptyList()
    },
    restore = { saved ->
        if (saved.isEmpty()) null
        else TripPoint(
            latitude = saved[0] as Double,
            longitude = saved[1] as Double,
            description = saved[2] as String
        )
    }
)
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

    var origin by rememberSaveable(stateSaver = tripPointSaver) { mutableStateOf<TripPoint?>(null) }
    var dest by rememberSaveable(stateSaver = tripPointSaver) { mutableStateOf<TripPoint?>(null) }
    var picking by remember { mutableStateOf(PickSlot.NONE) }
    var pickerSlot by remember { mutableStateOf<PickSlot?>(null) }
    var showResults by remember { mutableStateOf(false) }
    var plannerExpanded by rememberSaveable { mutableStateOf(true) }

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

    var arriveBy by rememberSaveable { mutableStateOf(false) }
    var arriveByTimeMillis by rememberSaveable { mutableStateOf<Long?>(null) }
    var showTimePicker by remember { mutableStateOf(false) }

    var planResult by remember { mutableStateOf<TripPlanResult?>(null) }
    var selectedIndex by remember { mutableIntStateOf(0) }
    var isPlanning by remember { mutableStateOf(false) }
    val planRunner = remember { SingleJobRunner(coroutineScope) }
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

    /** Cancels any in-flight plan request and drops the current results. */
    fun invalidatePlan() {
        planRunner.current.value?.cancel()
        planRunner.current.value = null
        planResult = null
        showResults = false
        isPlanning = false
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
                invalidatePlan()
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
        invalidatePlan()
    }

    fun planIt(refresh: Boolean = false) {
        val from = origin ?: return
        val to = dest ?: return
        // "Find trips" with an already-matching plan just reopens the results sheet; the
        // resume path forces a fresh request to keep the map current.
        val current = planResult
        if (!refresh && current is TripPlanResult.Success) {
            val existingPlan = current.plan
            if (existingPlan != null && existingPlan.from == from && existingPlan.to == to) {
                showResults = true
                return
            }
        }
        invalidatePlan()
        isPlanning = true
        planRunner.launch {
            try {
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
            } catch (e: CancellationException) {
                throw e
            } finally {
                // Only the current request may clear the planning state; a superseded
                // request's cleanup must not clobber the newer request's state.
                if (planRunner.isCurrent(coroutineContext[Job]!!)) {
                    isPlanning = false
                }
            }
        }
    }

    // Re-plan on app re-entry only if a plan already exists (keeps the map fresh without
    // surprising the user with a new request before they've picked anything).
    RememberOnResume {
        if (planResult != null && origin != null && dest != null) {
            planIt(refresh = true)
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
            picking = picking,
            onMapTap = { onMapTap(it) },
            isDark = isDark,
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.trip_planner_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        val collapseSource = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = { plannerExpanded = !plannerExpanded },
                            interactionSource = collapseSource,
                            modifier = Modifier.pressScale(collapseSource)
                        ) {
                            Icon(
                                imageVector = if (plannerExpanded) {
                                    Icons.Default.ExpandLess
                                } else {
                                    Icons.Default.ExpandMore
                                },
                                contentDescription = stringResource(
                                    if (plannerExpanded) R.string.collapse_planner else R.string.expand_planner
                                ),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    AnimatedVisibility(visible = plannerExpanded) {
                        Column(
                            modifier = Modifier
                                .heightIn(max = 320.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Spacer(modifier = Modifier.height(8.dp))

                            EndpointRow(
                                label = stringResource(R.string.origin_field_hint),
                                point = origin,
                                accentColor = MaterialTheme.colorScheme.primary,
                                onClick = {
                                    pickerSlot = PickSlot.ORIGIN
                                    picking = PickSlot.NONE
                                },
                                onClear = { origin = null; invalidatePlan() }
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
                                        invalidatePlan()
                                    },
                                    enabled = origin != null || dest != null,
                                    interactionSource = swapSource,
                                    modifier = Modifier
                                        .size(48.dp)
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
                                onClear = { dest = null; invalidatePlan() }
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = !arriveBy,
                                    onClick = {
                                        arriveBy = false
                                        invalidatePlan()
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
                                        invalidatePlan()
                                    },
                                    label = { Text(stringResource(R.string.arrive_by)) }
                                )
                                if (arriveBy) {
                                    TextButton(onClick = { showTimePicker = true }) {
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
                            modifier = Modifier.size(48.dp).pressScale(remember { MutableInteractionSource() })
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

    // Arrive-by time picker (Material 3).
    if (showTimePicker) {
        val initial = arriveByTimeMillis ?: (System.currentTimeMillis() + DEFAULT_ARRIVE_BY_ADVANCE_MS)
        val cal = Calendar.getInstance().apply { timeInMillis = initial }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.arrive_by_time_title)) },
            text = {
                TimePicker(
                    state = timeState,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showTimePicker = false
                    cal.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    cal.set(Calendar.MINUTE, timeState.minute)
                    arriveByTimeMillis = cal.timeInMillis
                    invalidatePlan()
                }) { Text(stringResource(R.string.done)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Endpoint picker sheet
    pickerSlot?.let { slot ->
        EndpointPickerSheet(
            slot = slot,
            transitRepository = transitRepository,
            onStopPicked = { stop ->
                val point = TripPoint(stop.latitude, stop.longitude, stop.desc)
                if (slot == PickSlot.ORIGIN) origin = point else dest = point
                invalidatePlan()
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
            IconButton(onClick = onClear, modifier = Modifier.size(48.dp)) {
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
