package com.trimettransit.tracker.feature.trips

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberSliderState
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.TripPlannerMin
import com.trimettransit.tracker.model.TripPlannerMode
import com.trimettransit.tracker.model.TripPoint
import com.trimettransit.tracker.model.TripRequestOptions
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.RecentStopsRepository
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.searchStops
import com.trimettransit.tracker.ui.components.StopSearchItem
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun EndpointPickerSheet(
    slot: PickSlot,
    transitRepository: TransitRepository,
    favoritesRepository: FavoritesRepository,
    recentStopsRepository: RecentStopsRepository,
    recentPoints: List<TripPoint>,
    onStopPicked: (Stop) -> Unit,
    onTripPointPicked: (TripPoint) -> Unit,
    onMyLocationPicked: () -> Unit,
    onMapPinPicked: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    var tab by remember { mutableIntStateOf(0) }
    var favorites by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var recentStops by remember { mutableStateOf<List<Stop>>(emptyList()) }

    // Local SQLite reads: the quick picks appear as soon as they resolve, and a failure just
    // leaves the sections out (search still works).
    LaunchedEffect(Unit) {
        favorites = runCatching { favoritesRepository.getFavorites() }.getOrDefault(emptyList())
        recentStops = runCatching { recentStopsRepository.getRecentStops() }.getOrDefault(emptyList())
    }

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

        PrimaryTabRow(selectedTabIndex = tab) {
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
                    favorites = favorites,
                    recentStops = recentStops,
                    recentPoints = recentPoints,
                    recentLabel = stringResource(
                        if (slot == PickSlot.ORIGIN) {
                            R.string.recent_origins_label
                        } else {
                            R.string.recent_destinations_label
                        }
                    ),
                    onTripPointClick = onTripPointPicked,
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
                    val pickOnMapSource = remember { MutableInteractionSource() }
                    FilledTonalButton(
                        onClick = onMapPinPicked,
                        interactionSource = pickOnMapSource,
                        modifier = Modifier.pressScale(pickOnMapSource)
                    ) {
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

@OptIn(FlowPreview::class)
@Composable
internal fun StopSearchPanel(
    transitRepository: TransitRepository,
    onStopClick: (Stop) -> Unit,
    favorites: List<Stop>,
    recentStops: List<Stop>,
    recentPoints: List<TripPoint>,
    recentLabel: String,
    onTripPointClick: (TripPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    // The sheet exists to type a stop, so take focus (and the keyboard) as soon as it settles.
    val searchFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        // Requesting during the expand animation loses the IME; a short wait after mount wins.
        delay(250)
        try {
            searchFocusRequester.requestFocus()
        } catch (e: IllegalStateException) {
            // The sheet was dismissed inside the settle delay, so there is nothing to focus.
        }
    }
    var allStops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Stop>>(emptyList()) }
    val searchHint = stringResource(R.string.search_stops_hint)

    LaunchedEffect(allStops == null, query.isNotBlank()) {
        if (allStops == null && query.isNotBlank()) {
            isLoading = true
            allStops = withContext(Dispatchers.IO) { transitRepository.searchStops() }
            isLoading = false
        }
    }

    // Debounced search (see HomeSearchBar): rapid keystrokes share one filter pass
    // over the multi-MB list; collectLatest cancels superseded filters. Filtering
    // runs on Default so typing never janks the sheet.
    LaunchedEffect(allStops) {
        snapshotFlow { query }
            .debounce(200.milliseconds)
            .collectLatest { q ->
                val stops = allStops
                results = if (q.isBlank() || stops == null) {
                    emptyList()
                } else {
                    withContext(Dispatchers.Default) { searchStops(stops, q) }
                }
            }
    }

    Column(modifier = modifier) {
        Surface(
            shape = appCardShape(),
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
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.clearAndSetSemantics { }
                        )
                    }
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = modalSearchTextStyle(),
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .focusRequester(searchFocusRequester)
                        .matchParentSize()
                        .padding(start = 44.dp, top = 14.dp, bottom = 14.dp, end = 12.dp)
                        .semantics { contentDescription = searchHint }
                )
            }
        }

        val panelState = when {
            isLoading && allStops == null && query.isNotBlank() -> 0
            allStops == null && query.isNotBlank() -> 1
            query.isBlank() -> 2
            results.isEmpty() -> 3
            else -> 4
        }
        Crossfade(
            targetState = panelState,
            animationSpec = m3EffectsDefault(),
            label = "endpointSearchPanel"
        ) { state ->
            when (state) {
                0 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
                1 -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.no_connection),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                2 -> {
                    QuickPicksList(
                        favorites = favorites,
                        recentStops = recentStops,
                        recentPoints = recentPoints,
                        recentLabel = recentLabel,
                        onStopClick = onStopClick,
                        onTripPointClick = onTripPointClick
                    )
                }
                3 -> {
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
                                modifier = if (AppMotion.reduceMotion) Modifier else Modifier.animateItem()
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The empty-query state of the stop search: remembered trips, favorites, then recent stops.
 * These are the endpoints riders pick most often, so they cost one tap instead of a search.
 */
@Composable
private fun QuickPicksList(
    favorites: List<Stop>,
    recentStops: List<Stop>,
    recentPoints: List<TripPoint>,
    recentLabel: String,
    onStopClick: (Stop) -> Unit,
    onTripPointClick: (TripPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteIds = favorites.map { it.locId }.toSet()
    val favoriteStops = favorites.take(QUICK_PICK_LIMIT)
    val recentsOnly = recentStops.filterNot { it.locId in favoriteIds }.take(QUICK_PICK_LIMIT)
    val trips = recentPoints.take(QUICK_PICK_LIMIT)

    if (trips.isEmpty() && favoriteStops.isEmpty() && recentsOnly.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.search_stops_prompt),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (trips.isNotEmpty()) {
            item(key = "recentPointsHeader") {
                QuickPickHeader(recentLabel)
            }
            items(trips, key = { "trip_${it.latitude}_${it.longitude}" }) { trip ->
                TripPointRow(trip = trip, onClick = { onTripPointClick(trip) })
            }
        }
        if (favoriteStops.isNotEmpty()) {
            item(key = "favoritesHeader") {
                QuickPickHeader(stringResource(R.string.favorites_label))
            }
            items(favoriteStops, key = { "favorite_${it.locId}" }) { stop ->
                StopSearchItem(stop = stop, onClick = { onStopClick(stop) })
            }
        }
        if (recentsOnly.isNotEmpty()) {
            item(key = "recentStopsHeader") {
                QuickPickHeader(stringResource(R.string.recent_stops_label))
            }
            items(recentsOnly, key = { "recent_${it.locId}" }) { stop ->
                StopSearchItem(stop = stop, onClick = { onStopClick(stop) })
            }
        }
    }
}

private const val QUICK_PICK_LIMIT = 8

@Composable
private fun QuickPickHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun TripPointRow(trip: TripPoint, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(source)
            .clickable(
                interactionSource = source,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = trip.description.ifBlank { stringResource(R.string.pinned_location) },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Slim text style for the search input field. */
@Composable
internal fun modalSearchTextStyle() =
    MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TripOptionsSheet(
    options: TripRequestOptions,
    onOptionsChanged: (TripRequestOptions) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            Text(
                text = stringResource(R.string.trip_options_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            // Travel mode and priority live here now; the planner card keeps only a one-line
            // summary, which gives the map back two chip rows on compact screens.
            Text(
                text = stringResource(R.string.trip_options_mode),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                PlanModeChip(TripPlannerMode.ALL, R.string.trip_mode_all, options, onOptionsChanged)
                PlanModeChip(TripPlannerMode.BUS, R.string.trip_mode_bus, options, onOptionsChanged)
                PlanModeChip(TripPlannerMode.TRAIN, R.string.trip_mode_train, options, onOptionsChanged)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.trip_options_min),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                PlanPriorityChip(TripPlannerMin.TIME, R.string.trip_min_time, options, onOptionsChanged)
                PlanPriorityChip(TripPlannerMin.TRANSFERS, R.string.trip_min_transfers, options, onOptionsChanged)
                PlanPriorityChip(TripPlannerMin.WALKING, R.string.trip_min_walking, options, onOptionsChanged)
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.trip_options_walk),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(
                        R.string.trip_summary_walk,
                        String.format(Locale.US, "%.1f", options.maxWalkMiles)
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                val walkSliderState = rememberSliderState(options.maxWalkMiles, 7, 0.1f..0.9f)
                LaunchedEffect(options.maxWalkMiles) { walkSliderState.value = options.maxWalkMiles }
                Slider(
                    state = walkSliderState,
                    onValueChange = { value ->
                        val snapped = Math.round(value * 10) / 10f
                        onOptionsChanged(options.copy(maxWalkMiles = snapped))
                        walkSliderState.value = snapped
                    },
                    modifier = Modifier.weight(2f)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.trip_options_count),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val minusSource = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        onOptionsChanged(options.copy(itineraryCount = options.itineraryCount - 1))
                    },
                    enabled = options.itineraryCount > 1,
                    interactionSource = minusSource,
                    modifier = Modifier
                        .size(48.dp)
                        .pressScale(minusSource)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = stringResource(R.string.decrease_option_count),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Crossfade(
                    targetState = options.itineraryCount,
                    animationSpec = m3EffectsDefault(),
                    modifier = Modifier.widthIn(min = 96.dp),
                    label = "itineraryCount"
                ) { count ->
                    Text(
                        text = pluralStringResource(
                            R.plurals.trip_option_count,
                            count,
                            count
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1
                    )
                }
                val plusSource = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        onOptionsChanged(options.copy(itineraryCount = options.itineraryCount + 1))
                    },
                    enabled = options.itineraryCount < 6,
                    interactionSource = plusSource,
                    modifier = Modifier
                        .size(48.dp)
                        .pressScale(plusSource)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = stringResource(R.string.increase_option_count),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/** Travel-mode chip used by the options sheet. */
@Composable
internal fun PlanModeChip(
    mode: TripPlannerMode,
    labelRes: Int,
    options: TripRequestOptions,
    onOptionsChanged: (TripRequestOptions) -> Unit
) {
    FilterChip(
        selected = options.mode == mode,
        onClick = { onOptionsChanged(options.copy(mode = mode)) },
        label = { Text(stringResource(labelRes)) }
    )
}

/** Priority chip used by the options sheet. */
@Composable
internal fun PlanPriorityChip(
    min: TripPlannerMin,
    labelRes: Int,
    options: TripRequestOptions,
    onOptionsChanged: (TripRequestOptions) -> Unit
) {
    FilterChip(
        selected = options.min == min,
        onClick = { onOptionsChanged(options.copy(min = min)) },
        label = { Text(stringResource(labelRes)) }
    )
}
