package com.trimettransit.tracker.feature.trips

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.TripItinerary
import com.trimettransit.tracker.model.TripLeg
import com.trimettransit.tracker.model.TripPlannerError
import com.trimettransit.tracker.model.TripPlannerMin
import com.trimettransit.tracker.model.TripPlannerMode
import com.trimettransit.tracker.model.TripPlanResult
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripRequestOptions
import com.trimettransit.tracker.map.MapCoordinate
import com.trimettransit.tracker.model.domain.itinerarySelectionAfterPlan
import com.trimettransit.tracker.model.domain.TripDraft
import com.trimettransit.tracker.model.domain.keepPlanVisible
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.RecentStopsRepository
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.appCardBorder
import com.trimettransit.tracker.ui.theme.m3ContentExpand
import com.trimettransit.tracker.ui.theme.m3ContentShrink
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3EffectsFast
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.ui.theme.m3SpatialFast
import com.trimettransit.tracker.util.SingleJobRunner
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Date
import java.util.Locale
private const val DEFAULT_ARRIVE_BY_ADVANCE_MS = 60L * 60_000L
private const val DEFAULT_DEPART_AT_ADVANCE_MS = 15L * 60_000L
/** Saves the whole planner draft (endpoints, scheduling choice, options) across rotation/process death. */
private val tripDraftSaver: Saver<TripDraft, Bundle> = Saver(
    save = { draft ->
        Bundle().apply {
            putBoolean("hasOrigin", draft.origin != null)
            draft.origin?.let {
                putDouble("oLat", it.latitude)
                putDouble("oLon", it.longitude)
                putString("oDesc", it.description)
                it.stopId?.let { id -> putString("oStop", id) }
            }
            putBoolean("hasDest", draft.destination != null)
            draft.destination?.let {
                putDouble("dLat", it.latitude)
                putDouble("dLon", it.longitude)
                putString("dDesc", it.description)
                it.stopId?.let { id -> putString("dStop", id) }
            }
            draft.departAtMillis?.let { putLong("departAt", it) }
            draft.arriveByMillis?.let { putLong("arriveBy", it) }
            draft.plannedAtMillis?.let { putLong("plannedAt", it) }
            putString("mode", draft.options.mode.name)
            putString("min", draft.options.min.name)
            putFloat("maxWalk", draft.options.maxWalkMiles)
            putInt("itineraryCount", draft.options.itineraryCount)
        }
    },
    restore = { saved ->
        val origin = if (saved.getBoolean("hasOrigin")) {
            TripPoint(
                latitude = saved.getDouble("oLat"),
                longitude = saved.getDouble("oLon"),
                description = saved.getString("oDesc") ?: "",
                stopId = saved.getString("oStop")
            )
        } else {
            null
        }
        val destination = if (saved.getBoolean("hasDest")) {
            TripPoint(
                latitude = saved.getDouble("dLat"),
                longitude = saved.getDouble("dLon"),
                description = saved.getString("dDesc") ?: "",
                stopId = saved.getString("dStop")
            )
        } else {
            null
        }
        TripDraft(
            origin = origin,
            destination = destination,
            departAtMillis = saved.takeIf { it.containsKey("departAt") }?.getLong("departAt"),
            arriveByMillis = saved.takeIf { it.containsKey("arriveBy") }?.getLong("arriveBy"),
            plannedAtMillis = saved.takeIf { it.containsKey("plannedAt") }?.getLong("plannedAt"),
            options = TripRequestOptions(
                mode = saved.getString("mode")?.let {
                    runCatching { TripPlannerMode.valueOf(it) }.getOrNull()
                } ?: TripPlannerMode.ALL,
                min = saved.getString("min")?.let {
                    runCatching { TripPlannerMin.valueOf(it) }.getOrNull()
                } ?: TripPlannerMin.TIME,
                maxWalkMiles = saved.getFloat("maxWalk", 0.5f).coerceIn(0.01f, 0.999f),
                itineraryCount = saved.getInt("itineraryCount", 3).coerceIn(1, 6)
            )
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
    favoritesRepository: FavoritesRepository,
    recentStopsRepository: RecentStopsRepository,
    onNavigateToArrivals: (Stop, Int) -> Unit,
    pageVisible: Boolean,
    isDark: Boolean = false
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    // Resource labels captured at composition (they must not be read via LocalContext in
    // non-composable lambdas below, which wouldn't track configuration changes).
    val myLocationLabel = stringResource(R.string.my_location)
    val pinnedLocationLabel = stringResource(R.string.pinned_location)

    // The in-progress request: endpoints, scheduling choice, and options in one value.
    // Arrive-by mode is derived (arriveByMillis != null means arrive-by); depart-now leaves
    // both times null.
    var draft by rememberSaveable(stateSaver = tripDraftSaver) {
        mutableStateOf(TripDraft(options = TripPlannerPrefs.load(context)))
    }
    var picking by remember { mutableStateOf(PickSlot.NONE) }
    var pickerSlot by remember { mutableStateOf<PickSlot?>(null) }
    var showResults by rememberSaveable { mutableStateOf(false) }
    // Tall screens can afford the expanded card; compact phones start collapsed so the map
    // keeps a usable area. Remembered once the rider decides either way.
    val startsExpanded = !plannerStartsCollapsed(LocalConfiguration.current.screenHeightDp)
    var plannerExpanded by rememberSaveable { mutableStateOf(startsExpanded) }
    // Endpoints of the last *requested* plan, as a saveable key. The result itself is not
    // saveable (DateTime-heavy), so restore re-requests it when this matches the current
    // endpoints and no result is present.
    var plannedEndpointKey by rememberSaveable { mutableStateOf<String?>(null) }

    var myLocation by remember { mutableStateOf<MapCoordinate?>(null) }
    var locationPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var hasAskedPermission by remember { mutableStateOf(false) }
    var showLocationExplainer by remember { mutableStateOf(false) }
    var pendingMyLocationOrigin by remember { mutableStateOf(false) }

    // The scheduling choice lives in the draft: arriveByMillis set means arrive-by,
    // departAtMillis set means depart-at, both null means depart-now. This holds the time
    // sheet's target slot (null = closed), not the choice itself.
    var scheduleSheetArriveBy by remember { mutableStateOf<Boolean?>(null) }
    var recentTrips by remember { mutableStateOf(TripPlannerPrefs.loadRecentTrips(context)) }
    var recentOrigins by remember { mutableStateOf(TripPlannerPrefs.loadRecentOrigins(context)) }

    // Options ride along in the draft (and stay prefs-persisted via TripPlannerPrefs).
    // Read aliases below re-evaluate on every recomposition; every write goes through
    // draft.copy so save/restore sees a single value.
    val origin = draft.origin
    val dest = draft.destination
    val arriveBy = draft.arriveByMillis != null
    val options = draft.options
    var showOptionsSheet by remember { mutableStateOf(false) }

    var planResult by remember { mutableStateOf<TripPlanResult?>(null) }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }
    var isPlanning by remember { mutableStateOf(false) }
    val planRunner = remember { SingleJobRunner(coroutineScope) }
    var locationJob by remember { mutableStateOf<Job?>(null) }
    var legGeometries by remember { mutableStateOf<Map<Int, List<GeoPoint>>>(emptyMap()) }
    val geometryDirectionsCache = remember {
        mutableStateOf<Map<Int, List<Direction>>>(emptyMap())
    }
    val geometryStopsCache = remember {
        mutableStateOf<Map<Pair<Int, Int>, List<Stop>>>(emptyMap())
    }

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

    // Location is asked for only from an explicit action ("Use my location" in the picker
    // sheet), never merely because this page became visible. The granted-path fetch below is a
    // read, not a prompt, and stays.

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
                draft = draft.copy(origin = TripPoint(location.latitude, location.longitude, myLocationLabel))
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
            // Now the only thing that records the permission as asked-for, which is what shows
            // the "Location permission is off" chip once the request is refused.
            hasAskedPermission = true
            showLocationExplainer = true
        }
    }

    fun onMapTap(value: MapCoordinate) {
        if (!value.latitude.isFinite() || !value.longitude.isFinite() ||
            value.latitude !in -90.0..90.0 || value.longitude !in -180.0..180.0
        ) {
            return
        }
        when (picking) {
            PickSlot.ORIGIN -> {
                pendingMyLocationOrigin = false
                draft = draft.copy(origin = TripPoint(value.latitude, value.longitude, pinnedLocationLabel))
            }
            PickSlot.DEST -> draft = draft.copy(
                destination = TripPoint(value.latitude, value.longitude, pinnedLocationLabel)
            )
            PickSlot.NONE -> return
        }
        picking = PickSlot.NONE
        invalidatePlan()
    }

    /**
     * Requests a plan. [from]/[to] default to the endpoints in composition; a caller that has
     * just swapped the draft must pass the new pair explicitly, because the composition-level
     * aliases still hold the previous values until the next recomposition.
     */
    fun planIt(
        refresh: Boolean = false,
        resetSelection: Boolean = true,
        from: TripPoint? = null,
        to: TripPoint? = null
    ) {
        val start = from ?: origin ?: return
        val end = to ?: dest ?: return
        if (!start.isValid || !end.isValid) return
        // "Find trips" with an already-matching plan just reopens the results sheet; the
        // resume path forces a fresh request to keep the map current.
        val current = planResult
        if (!refresh && current is TripPlanResult.Success) {
            val existingPlan = current.plan
            if (existingPlan != null && existingPlan.from == start && existingPlan.to == end) {
                showResults = true
                return
            }
        }
        // A plan for the same endpoints stays visible while the new request runs: refresh,
        // option changes, and the earlier/later controls must not blank the map or close the
        // results. A different endpoint pairing still clears, so stale results are never
        // mistaken for the new ones.
        val currentPlan = (current as? TripPlanResult.Success)?.plan
        val keepVisible = keepPlanVisible(currentPlan, start, end)
        planRunner.current.value?.cancel()
        planRunner.current.value = null
        if (!keepVisible) {
            planResult = null
            showResults = false
        }
        plannedEndpointKey = endpointKey(start, end)
        isPlanning = true
        planRunner.launchWithJob { job ->
            try {
                // A time that has passed while the user was deciding -- or that survived process
                // death in a stale saved state -- is refused, never sent to the API: arrive-by
                // falls back to the picker's default (0ab3386 behavior) and depart-at falls
                // back to depart-now.
                val now = System.currentTimeMillis()
                if (draft.isArriveByStale(now)) {
                    draft = draft.copy(arriveByMillis = now + DEFAULT_ARRIVE_BY_ADVANCE_MS)
                } else if (draft.isDepartAtStale(now)) {
                    // A departure that already left is planned as depart-now, the closest
                    // request that still answers the rider.
                    draft = draft.copy(departAtMillis = null)
                }
                val result = transitRepository.planTrip(start, end, draft.requestTime(), options)
                val successPlan = (result as? TripPlanResult.Success)?.plan
                if (successPlan?.itineraries?.isNotEmpty() == true) {
                    selectedIndex = itinerarySelectionAfterPlan(
                        currentIndex = selectedIndex,
                        itineraryCount = successPlan.itineraries.size,
                        reset = resetSelection
                    )
                    draft = draft.copy(plannedAtMillis = System.currentTimeMillis())
                    draft.destination?.let { destination ->
                        recentTrips = TripPlannerPrefs.addRecentTrip(context, destination)
                    }
                    // A located origin is dynamic, so remembering it would only replay stale
                    // coordinates; everything else makes a useful one-tap origin next time.
                    draft.origin
                        ?.takeIf { it.description.isNotBlank() && it.description != myLocationLabel }
                        ?.let { plannedOrigin ->
                            recentOrigins = TripPlannerPrefs.addRecentOrigin(context, plannedOrigin)
                        }
                }
                planResult = result
                showResults = result is TripPlanResult.Success &&
                    successPlan?.itineraries?.isNotEmpty() == true
            } catch (e: CancellationException) {
                throw e
            } finally {
                // Only the current request may clear the planning state; a superseded
                // request's cleanup must not clobber the newer request's state.
                if (planRunner.isCurrent(job)) {
                    isPlanning = false
                }
            }
        }
    }

    /** Persists planner options and refreshes an existing plan with the new request. */
    fun updateOptions(newOptions: TripRequestOptions) {
        if (newOptions == draft.options) return
        val shouldReplan = planResult != null && draft.origin != null && draft.destination != null
        draft = draft.copy(options = newOptions)
        TripPlannerPrefs.save(context, newOptions)
        // planIt keeps the current plan on screen while the new request runs.
        if (shouldReplan) {
            planIt(refresh = true, resetSelection = false)
        } else {
            invalidatePlan()
        }
    }

    /**
     * Re-plans the same endpoints shifted by [deltaMillis] for the results' earlier/later
     * controls, keeping the results view and option selection in place.
     */
    fun browseSchedule(deltaMillis: Long) {
        if (draft.origin == null || draft.destination == null) return
        draft = draft.shiftedSchedule(deltaMillis, System.currentTimeMillis())
        // Same endpoints, so planIt keeps the results open while the shifted request runs.
        planIt(refresh = true, resetSelection = false)
    }

    /** The one-tap return trip: swap the endpoints and re-plan in place. */
    fun planReturnTrip() {
        val previousOrigin = draft.origin ?: return
        val previousDestination = draft.destination ?: return
        draft = draft.copy(origin = previousDestination, destination = previousOrigin)
        pendingMyLocationOrigin = false
        // Pass the swapped pair directly: the composition aliases still hold the old
        // direction, and planIt must not re-request the trip the rider just reversed.
        planIt(
            refresh = true,
            resetSelection = false,
            from = previousDestination,
            to = previousOrigin
        )
    }

    /** Opens arrivals for the stop a transit leg boards at. */
    fun openLegArrivals(leg: TripLeg) {
        val stopId = leg.from.stopId?.toIntOrNull() ?: return
        val routeNum = leg.routeNumber?.toIntOrNull() ?: -1
        onNavigateToArrivals(
            Stop(
                desc = leg.from.description,
                latitude = leg.from.latitude,
                longitude = leg.from.longitude,
                transitType = leg.mode.transitTypeLetter(),
                locId = stopId,
                routeNum = routeNum
            ),
            routeNum
        )
    }

    // Re-plan on app re-entry only if a plan already exists (keeps the map fresh without
    // surprising the user with a new request before they've picked anything).
    RememberOnResume {
        if (planResult != null && origin != null && dest != null) {
            planIt(refresh = true, resetSelection = false)
        }
    }

    val originNow = origin
    val destNow = dest
    val currentEndpointKey =
        if (originNow != null && destNow != null) endpointKey(originNow, destNow) else null

    // The pager disposes pages more than one away, so switching tabs and returning restores the
    // endpoints but not the result. If the same endpoints were planned and no result is present,
    // fetch it again so the results sheet and the drawn route come back with it. The !isPlanning
    // guard is load-bearing: without it this effect would cancel the request it just launched,
    // because planResult is still null while that request is in flight.
    LaunchedEffect(pageVisible, plannedEndpointKey) {
        if (pageVisible && !isPlanning && planResult == null &&
            plannedEndpointKey != null && plannedEndpointKey == currentEndpointKey &&
            origin != null && dest != null
        ) {
            planIt(refresh = true, resetSelection = false)
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

    val walkSummary = stringResource(
        R.string.trip_summary_walk,
        String.format(Locale.US, "%.1f", options.maxWalkMiles)
    )
    val countSummary = pluralStringResource(
        R.plurals.trip_option_count,
        options.itineraryCount,
        options.itineraryCount
    )
    // The chips for mode and priority moved into the options sheet; this summary keeps both
    // choices visible on the card, where they used to cost two full chip rows.
    val modeSummary = stringResource(
        when (options.mode) {
            TripPlannerMode.BUS -> R.string.trip_mode_bus
            TripPlannerMode.TRAIN -> R.string.trip_mode_train
            TripPlannerMode.ALL -> R.string.trip_mode_all
        }
    )
    val prioritySummary = stringResource(
        when (options.min) {
            TripPlannerMin.TRANSFERS -> R.string.trip_min_transfers
            TripPlannerMin.WALKING -> R.string.trip_min_walking
            TripPlannerMin.TIME -> R.string.trip_min_time
        }
    )
    val optionsSummary = "$modeSummary · $prioritySummary · $walkSummary · $countSummary"

    // Back from the results view returns to the planner with the plan (and draft) intact.
    BackHandler(enabled = showResults) { showResults = false }

    // Resolve the selected itinerary's transit legs into real route geometry once the plan
    // (or its selected option) changes; walks and legs whose route can't be matched to the
    // route config keep the map's default stick rendering.
    LaunchedEffect(selectedItinerary, transitRepository) {
        legGeometries = fetchLegGeometries(
            transitRepository = transitRepository,
            itinerary = selectedItinerary,
            directionsCache = geometryDirectionsCache,
            stopsCache = geometryStopsCache
        )
    }

    val resultPlan = (planResult as? TripPlanResult.Success)?.plan
        ?.takeIf { it.itineraries.isNotEmpty() }
    // The minute tick is scoped to this label, and only while the page is visible.
    val ageMinutesFmt = stringResource(R.string.trip_updated_minutes)
    val ageJustNow = stringResource(R.string.trip_updated_now)
    val resultsAgeText = rememberAgeLabel(active = pageVisible) { now ->
        draft.ageText(now, ageMinutesFmt, ageJustNow)
    }
    val originLabel = origin?.description?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.origin_field_hint)
    val destinationLabel = dest?.description?.takeIf { it.isNotBlank() }
        ?: stringResource(R.string.destination_field_hint)

    val resultsPlan = resultPlan
    if (showResults && resultsPlan != null) {
        // Selected option, resolved once for both the share text and the map preview.
        val shareItinerary = resultsPlan.itineraries.getOrNull(selectedIndex)
        Box(modifier = Modifier.fillMaxSize()) {
            ContentEntrance {
                ItineraryResultsScreen(
                    plan = resultsPlan,
                    selectedIndex = selectedIndex,
                    originLabel = originLabel,
                    destinationLabel = destinationLabel,
                    onSelect = {
                        selectedIndex = it
                        picking = PickSlot.NONE
                    },
                    onBack = { showResults = false },
                    onRefresh = { planIt(refresh = true, resetSelection = false) },
                    isRefreshing = isPlanning,
                    ageText = resultsAgeText,
                    canBrowseEarlier = draft.canShiftEarlier,
                    onEarlier = { browseSchedule(-RESULT_BROWSE_STEP_MS) },
                    onLater = { browseSchedule(RESULT_BROWSE_STEP_MS) },
                    onSwapEndpoints = { planReturnTrip() },
                    onShare = {
                        shareItinerary?.let { itinerary ->
                            shareTrip(context, originLabel, destinationLabel, itinerary)
                        }
                    },
                    onLegClick = { leg -> openLegArrivals(leg) },
                    mapPreview = {
                        TripMap(
                            origin = origin,
                            dest = dest,
                            itinerary = resultsPlan.itineraries.getOrNull(selectedIndex),
                            legGeometries = legGeometries,
                            myLocation = myLocation,
                            picking = PickSlot.NONE,
                            onMapTap = {},
                            isDark = isDark,
                            fitPadding = intArrayOf(32, 32, 32, 32),
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            TripMap(
                origin = origin,
                dest = dest,
                itinerary = selectedItinerary,
                legGeometries = legGeometries,
                myLocation = myLocation,
                picking = picking,
                onMapTap = { onMapTap(it) },
                isDark = isDark,
                showMyLocationButton = true,
                modifier = Modifier.fillMaxSize()
            )

            // "Location permission is off" chip (mirrors the other location screens).
            AnimatedVisibility(
                visible = pageVisible && !locationPermissionGranted && hasAskedPermission,
                enter = if (AppMotion.reduceMotion) {
                    fadeIn(m3EffectsDefault())
                } else {
                    fadeIn(m3EffectsDefault()) + slideInVertically(m3SpatialDefault()) { -it }
                },
                exit = if (AppMotion.reduceMotion) {
                    fadeOut(m3EffectsFast())
                } else {
                    fadeOut(m3EffectsFast()) + slideOutVertically(m3SpatialFast()) { -it / 3 }
                }
            ) {
                Surface(
                    onClick = { showLocationExplainer = true },
                    shape = appCardShape(),
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
                    shape = appCardShape(),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = appCardBorder(),
                    shadowElevation = 1.dp,
                    tonalElevation = 0.dp,
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
                                modifier = Modifier.size(48.dp).pressScale(collapseSource)
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

                        AnimatedVisibility(
                            visible = plannerExpanded,
                            enter = m3ContentExpand(),
                            exit = m3ContentShrink()
                        ) {
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
                                    onClear = { pendingMyLocationOrigin = false; draft = draft.copy(origin = null); invalidatePlan() },
                                    onPickOnMap = { picking = PickSlot.ORIGIN; pickerSlot = null }
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
                                            val from = draft.origin
                                            pendingMyLocationOrigin = false
                                            draft = draft.copy(origin = draft.destination, destination = from)
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
                                    onClear = { draft = draft.copy(destination = null); invalidatePlan() },
                                    onPickOnMap = { picking = PickSlot.DEST; pickerSlot = null }
                                )

                                // Depart-now / depart-at / arrive-by. A timed chip shows its
                                // chosen time in the label, so the chip is the only control (no
                                // duplicate time button below). The spoken label keeps the
                                // direction ("Depart at ...") that the visible label drops.
                                val departAtMillis = draft.departAtMillis
                                val arriveByMillis = draft.arriveByMillis
                                val departChipDescription = departAtMillis?.let {
                                    stringResource(R.string.trip_depart_at_fmt, scheduleTimeLabel(it))
                                }
                                val arriveChipDescription = arriveByMillis?.let {
                                    stringResource(R.string.trip_arrive_by_fmt, scheduleTimeLabel(it))
                                }
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    FilterChip(
                                        selected = !arriveBy && departAtMillis == null,
                                        onClick = {
                                            draft = draft.copy(arriveByMillis = null, departAtMillis = null)
                                            invalidatePlan()
                                        },
                                        label = { Text(stringResource(R.string.depart_now)) }
                                    )
                                    FilterChip(
                                        selected = departAtMillis != null,
                                        onClick = {
                                            if (departAtMillis == null) {
                                                draft = draft.copy(
                                                    departAtMillis = System.currentTimeMillis() + DEFAULT_DEPART_AT_ADVANCE_MS,
                                                    arriveByMillis = null
                                                )
                                                invalidatePlan()
                                            }
                                            scheduleSheetArriveBy = false
                                        },
                                        label = {
                                            Text(
                                                text = departAtMillis?.let { scheduleTimeLabel(it) }
                                                    ?: stringResource(R.string.depart_at),
                                                modifier = departChipDescription?.let { description ->
                                                    Modifier.semantics { contentDescription = description }
                                                } ?: Modifier
                                            )
                                        }
                                    )
                                    FilterChip(
                                        selected = arriveBy,
                                        onClick = {
                                            if (arriveByMillis == null) {
                                                draft = draft.copy(
                                                    arriveByMillis = System.currentTimeMillis() + DEFAULT_ARRIVE_BY_ADVANCE_MS,
                                                    departAtMillis = null
                                                )
                                                invalidatePlan()
                                            }
                                            scheduleSheetArriveBy = true
                                        },
                                        label = {
                                            Text(
                                                text = arriveByMillis?.let { scheduleTimeLabel(it) }
                                                    ?: stringResource(R.string.arrive_by),
                                                modifier = arriveChipDescription?.let { description ->
                                                    Modifier.semantics { contentDescription = description }
                                                } ?: Modifier
                                            )
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val optionsSource = remember { MutableInteractionSource() }
                                Surface(
                                    onClick = { showOptionsSheet = true },
                                    interactionSource = optionsSource,
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .pressScale(optionsSource)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = stringResource(R.string.trip_options_open),
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = optionsSummary,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        // Kept outside the collapsible, scrolling section so the main action
                        // never scrolls out of view.
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
                                animationSpec = m3EffectsDefault(),
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
                    enter = if (AppMotion.reduceMotion) {
                        fadeIn(m3EffectsDefault())
                    } else {
                        fadeIn(m3EffectsDefault()) + slideInVertically(m3SpatialDefault()) { -it }
                    },
                    exit = if (AppMotion.reduceMotion) {
                        fadeOut(m3EffectsFast())
                    } else {
                        fadeOut(m3EffectsFast()) + slideOutVertically(m3SpatialFast()) { -it / 3 }
                    }
                ) {
                    Surface(
                        shape = appCardShape(),
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
                            val cancelPickSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = { picking = PickSlot.NONE },
                                interactionSource = cancelPickSource,
                                modifier = Modifier.size(48.dp).pressScale(cancelPickSource)
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
                val statusIsError = planResult is TripPlanResult.Error
                var lastStatus by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
                if (statusText != null) lastStatus = statusText to statusIsError
                AnimatedVisibility(
                    visible = statusText != null,
                    enter = m3ContentExpand(),
                    exit = m3ContentShrink(),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                ) {
                    val shown = lastStatus
                    if (shown != null) {
                        Surface(
                            shape = appCardShape(),
                            color = if (shown.second) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            shadowElevation = 4.dp
                        ) {
                            Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 4.dp)) {
                                Text(
                                    text = shown.first,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (shown.second) {
                                        MaterialTheme.colorScheme.onErrorContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                // Dead-end states get a way out: retry a failed request, or
                                // loosen the search that found no trips.
                                if (origin != null && dest != null) {
                                    Row {
                                        if (shown.second) {
                                            TextButton(onClick = { planIt(refresh = true) }) {
                                                Text(stringResource(R.string.trip_retry))
                                            }
                                        } else {
                                            TextButton(
                                                onClick = {
                                                    updateOptions(
                                                        options.copy(
                                                            maxWalkMiles = (options.maxWalkMiles + 0.2f)
                                                                .coerceAtMost(0.9f)
                                                        )
                                                    )
                                                }
                                            ) {
                                                Text(stringResource(R.string.trip_allow_more_walking))
                                            }
                                            if (draft.departAtMillis != null || draft.arriveByMillis != null) {
                                                TextButton(
                                                    onClick = {
                                                        draft = draft.copy(departAtMillis = null, arriveByMillis = null)
                                                        planIt(refresh = true)
                                                    }
                                                ) {
                                                    Text(stringResource(R.string.depart_now))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Schedule picker: depart-at or arrive-by, date + time in one sheet.
    scheduleSheetArriveBy?.let { sheetArriveBy ->
        val current = if (sheetArriveBy) draft.arriveByMillis else draft.departAtMillis
        val initial = current ?: (System.currentTimeMillis() +
            if (sheetArriveBy) DEFAULT_ARRIVE_BY_ADVANCE_MS else DEFAULT_DEPART_AT_ADVANCE_MS)
        TripScheduleSheet(
            arriveBy = sheetArriveBy,
            initialMillis = initial,
            onConfirm = { millis ->
                val unchanged = if (sheetArriveBy) {
                    draft.arriveByMillis == millis
                } else {
                    draft.departAtMillis == millis
                }
                draft = if (sheetArriveBy) {
                    draft.copy(arriveByMillis = millis, departAtMillis = null)
                } else {
                    draft.copy(departAtMillis = millis, arriveByMillis = null)
                }
                // Confirming the same time should not throw away the results on screen.
                if (!unchanged) invalidatePlan()
            },
            onDismiss = { scheduleSheetArriveBy = null }
        )
    }

    // Endpoint picker sheet
    pickerSlot?.let { slot ->
        EndpointPickerSheet(
            slot = slot,
            transitRepository = transitRepository,
            favoritesRepository = favoritesRepository,
            recentStopsRepository = recentStopsRepository,
            recentPoints = if (slot == PickSlot.ORIGIN) recentOrigins else recentTrips,
            onStopPicked = { stop ->
                val point = TripPoint(stop.latitude, stop.longitude, stop.desc)
                if (slot == PickSlot.ORIGIN) {
                    pendingMyLocationOrigin = false
                    draft = draft.copy(origin = point)
                } else {
                    draft = draft.copy(destination = point)
                }
                invalidatePlan()
                pickerSlot = null
            },
            onTripPointPicked = { trip ->
                if (slot == PickSlot.ORIGIN) {
                    pendingMyLocationOrigin = false
                    draft = draft.copy(origin = trip)
                } else {
                    draft = draft.copy(destination = trip)
                }
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

    // Trip options sheet
    if (showOptionsSheet) {
        TripOptionsSheet(
            options = options,
            onOptionsChanged = { updateOptions(it) },
            onDismiss = { showOptionsSheet = false }
        )
    }

}

/** Step for the results' earlier/later controls. */
private const val RESULT_BROWSE_STEP_MS = 30L * 60_000L

/** Shares the selected option as plain text so a rider can forward a plan. */
private fun shareTrip(
    context: Context,
    originLabel: String,
    destinationLabel: String,
    itinerary: TripItinerary
) {
    val dateLabel = itinerary.departure?.let { departure ->
        DateFormat.getMediumDateFormat(context).format(Date(departure.millis))
    }.orEmpty()
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_SUBJECT,
            context.getString(R.string.share_trip_subject_fmt, originLabel, destinationLabel)
        )
        putExtra(
            Intent.EXTRA_TEXT,
            buildTripShareText(
                originLabel = originLabel,
                destinationLabel = destinationLabel,
                dateLabel = dateLabel,
                itinerary = itinerary
            )
        )
    }
    context.startActivity(Intent.createChooser(send, context.getString(R.string.share_trip)))
}

/** Stable identity for a from→to pair, used to decide whether a restored plan is still relevant. */
private fun endpointKey(from: TripPoint, to: TripPoint): String =
    "${from.latitude},${from.longitude}->${to.latitude},${to.longitude}"
