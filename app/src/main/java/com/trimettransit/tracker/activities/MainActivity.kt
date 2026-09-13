package com.trimettransit.tracker.activities

import android.app.PictureInPictureParams
import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.graphics.toColorInt
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import com.trimettransit.tracker.activities.toggleFavorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.trimettransit.tracker.ui.MainBottomBar
import com.trimettransit.tracker.ui.MainNavigationRail
import com.trimettransit.tracker.ui.bottomNavItems
import com.trimettransit.tracker.ui.components.findActivity
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberIsInPipMode
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.FavoritesRepositoryImpl
import com.trimettransit.tracker.data.local.RecentStopsRepositoryImpl
import com.trimettransit.tracker.transit.TransitRepositoryImpl
import com.trimettransit.tracker.widget.WidgetScheduler
import com.trimettransit.tracker.widget.WidgetLaunch
import com.trimettransit.tracker.notifications.DepartureAlertsSection
import com.trimettransit.tracker.widget.settings.WidgetSettingsSection
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.feature.arrivals.ArrivalsScreen
import com.trimettransit.tracker.feature.home.FavoritesScreen
import com.trimettransit.tracker.feature.home.RecentStopsScreen
import com.trimettransit.tracker.feature.settings.SettingsScreen
import com.trimettransit.tracker.feature.stops.NearbyStopsScreen
import com.trimettransit.tracker.feature.stops.StopsScreen
import com.trimettransit.tracker.feature.trips.TripPlannerScreen
import com.trimettransit.tracker.ui.appearance.AppearancePrefs
import com.trimettransit.tracker.ui.appearance.AppearanceStyle
import com.trimettransit.tracker.ui.appearance.FontScale
import com.trimettransit.tracker.ui.appearance.ThemePreference
import com.trimettransit.tracker.ui.appearance.readAppearanceStyle
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.TriMetGoTheme
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3EffectsFast
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.ui.theme.m3SpatialFast
import com.trimettransit.tracker.R

private val AnimatedContentTransitionScope<*>.navEnter: EnterTransition
    get() = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = m3SpatialDefault()
    ) + fadeIn(
        initialAlpha = 0.7f,
        animationSpec = m3EffectsDefault()
    )

private val AnimatedContentTransitionScope<*>.navExit: ExitTransition
    get() = slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = m3SpatialFast()
    ) + fadeOut(
        targetAlpha = 0.7f,
        animationSpec = m3EffectsFast()
    )

private val AnimatedContentTransitionScope<*>.navPopEnter: EnterTransition
    get() = slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = m3SpatialDefault()
    ) + fadeIn(
        initialAlpha = 0.7f,
        animationSpec = m3EffectsDefault()
    )

private val AnimatedContentTransitionScope<*>.navPopExit: ExitTransition
    get() = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = m3SpatialFast()
    ) + fadeOut(
        targetAlpha = 0.7f,
        animationSpec = m3EffectsFast()
    )

/**
 * Enter transition for the Arrivals destination: the fast spatial spring so pushing to
 * Arrivals from Home/Routes reads tighter/snappier than the default [navEnter].
 */
private val AnimatedContentTransitionScope<*>.navEnterArrivals: EnterTransition
    get() = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = m3SpatialFast()
    ) + fadeIn(
        initialAlpha = 0.7f,
        animationSpec = m3EffectsFast()
    )

/**
 * Quick variant of [navExit] (fast effects/spatial springs) for the Home and Routes
 * destinations so the push to Arrivals reads tighter; same slide+fade shape.
 */
private val AnimatedContentTransitionScope<*>.navExitQuick: ExitTransition
    get() = slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = m3SpatialFast()
    ) + fadeOut(
        targetAlpha = 0.7f,
        animationSpec = m3EffectsFast()
    )


// Type-safe navigation destinations, shared by the NavHost registration and every navigate()/popBackStack().
@Serializable
object HomeDestination

@Serializable
object SettingsDestination

@Serializable
object NearbyStopsDestination

@Serializable
data class ArrivalsDestination(
    val stopId: Int,
    val stopName: String = "",
    val routeId: Int = -1,
    val lat: Double = 0.0,
    val lng: Double = 0.0
)

/** Back arrow used by every non-top-level top app bar. */
@Composable
private fun BackNavigationIcon(onClick: () -> Unit) {
    val backSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = backSource,
        modifier = Modifier.pressScale(backSource)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
    }
}


class MainActivity : ComponentActivity() {

    internal val widgetLaunchIntent = mutableStateOf<Intent?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        widgetLaunchIntent.value = intent
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        widgetLaunchIntent.value = intent
        WidgetScheduler.schedulePeriodic(this)
        WidgetScheduler.refreshNow(this)
        // Play's "deprecated Android 15 edge-to-edge APIs" warning comes from
        // androidx.activity's enableEdgeToEdge() backward-compat internals, not
        // app code. Known/benign — don't reimplement edge-to-edge to "fix" it.
        enableEdgeToEdge()
        setContent {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            var appearance by remember { mutableStateOf(readAppearanceStyle(prefs)) }
            var cardCornerRadiusPref by remember { mutableIntStateOf(prefs.getInt("pref_key_card_corner_radius", 16)) }
            var cardCornerStylePref by remember {
                mutableStateOf(prefs.getString("pref_key_card_corner_style", "rounded") ?: "rounded")
            }
            var cardOutlinesPref by remember { mutableStateOf(prefs.getBoolean("pref_key_card_outlines", true)) }
            var cardOutlineColorPref by remember {
                mutableStateOf(prefs.getString("pref_key_card_outline_color", "auto") ?: "auto")
            }
            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        AppearancePrefs.THEME, AppearancePrefs.COLOR_MODE, AppearancePrefs.ACCENT_COLOR,
                        AppearancePrefs.VIBRANCY, AppearancePrefs.AMOLED_DARK, AppearancePrefs.PILL_ACCENT,
                        AppearancePrefs.TRANSIT_BUS, AppearancePrefs.TRANSIT_RAIL,
                        AppearancePrefs.TRANSIT_STREETCAR, AppearancePrefs.TRANSIT_WES,
                        AppearancePrefs.DENSITY, AppearancePrefs.FONT_SCALE, AppearancePrefs.MOTION,
                        AppearancePrefs.DYNAMIC_COLOR -> appearance = readAppearanceStyle(prefs)
                        "pref_key_card_corner_radius" -> cardCornerRadiusPref = prefs.getInt("pref_key_card_corner_radius", 16)
                        "pref_key_card_corner_style" -> cardCornerStylePref =
                            prefs.getString("pref_key_card_corner_style", "rounded") ?: "rounded"
                        "pref_key_card_outlines" -> cardOutlinesPref = prefs.getBoolean("pref_key_card_outlines", true)
                        "pref_key_card_outline_color" -> cardOutlineColorPref = prefs.getString("pref_key_card_outline_color", "auto") ?: "auto"
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val isDark = when (appearance.theme) {
                ThemePreference.DARK -> true
                ThemePreference.LIGHT -> false
                else -> isSystemInDarkTheme()
            }
            AppMotion.intensity = appearance.motionIntensity
            TriMetGoTheme(
                darkTheme = isDark,
                appearance = appearance,
                cardCornerRadius = cardCornerRadiusPref.dp,
                cardCutCorners = cardCornerStylePref == "cut",
                cardOutlinesEnabled = cardOutlinesPref,
                cardOutlineColor = parseCardOutlineColor(cardOutlineColorPref)
            ) {
                val activity = LocalActivity.current
                SideEffect {
                    if (activity != null) {
                        WindowInsetsControllerCompat(
                            activity.window, activity.window.decorView
                        ).isAppearanceLightStatusBars = !isDark
                        WindowInsetsControllerCompat(
                            activity.window, activity.window.decorView
                        ).isAppearanceLightNavigationBars = !isDark
                    }
                }
                // In-app text-size override on top of the system font scale (sp-based only,
                // so dp paddings stay put).
                val fontScaleFactor = when (appearance.fontScale) {
                    FontScale.SMALLER -> 0.9f
                    FontScale.LARGER -> 1.15f
                    else -> 1.0f
                }
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = LocalDensity.current.fontScale * fontScaleFactor
                    )
                ) {
                    MainAppContent(isDark = isDark)
                }
            }
        }
    }

    // Each foreground serves as a widget-refresh event so a favorite added/removed in the
    // app shows up on the home screen without waiting for the full periodic cadence.
    override fun onStart() {
        super.onStart()
        WidgetScheduler.refreshNow(this)
    }
}

/** Parses a stored card-outline colour pref ("auto" → null = follow the theme). */
private fun parseCardOutlineColor(value: String): Color? {
    if (value == "auto") return null
    return runCatching { Color(value.toColorInt()) }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppContent(
    isDark: Boolean
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val inPip = rememberIsInPipMode()
    val pipActivity = LocalContext.current.findActivity()
    val context = LocalContext.current

    // WS4 adaptive layout: wide (≥840dp) screens become a master-detail split. The top-level
    // pager sits left and the selected stop's arrivals render in a persistent right pane; the
    // bottom pill bar is replaced by a left-edge rail. Narrow/mid layouts keep the phone UX.
    val windowInfo = LocalWindowInfo.current
    val expandedPane = with(LocalDensity.current) { windowInfo.containerSize.width >= 840.dp.roundToPx() }
    var detailStop by remember { mutableStateOf<ArrivalsDestination?>(null) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val refreshRotation = remember { Animatable(0f) }
    val destination = currentBackStackEntry?.destination
    val isTopLevel = destination?.hasRoute<HomeDestination>() == true
    val isArrivals = destination?.hasRoute<ArrivalsDestination>() == true
    val isNearbyStops = destination?.hasRoute<NearbyStopsDestination>() == true
    val isSettings = destination?.hasRoute<SettingsDestination>() == true

    // Manual dependency wiring: one shared DatabaseHelper drives both local-data repos.
    val appContext = context.applicationContext
    val favoritesRepository = remember { FavoritesRepositoryImpl(DatabaseHelper(appContext)) }
    val recentStopsRepository = remember { RecentStopsRepositoryImpl(DatabaseHelper(appContext)) }
    val transitRepository = remember { TransitRepositoryImpl(appContext) }
    // Sub-screens brand the collapsed pill with their own icon and name.
    var contextLabelRes: Int? = null
    var contextIcon: ImageVector? = null
    when {
        isArrivals -> {
            contextLabelRes = R.string.nav_arrivals
            contextIcon = Icons.Filled.Schedule
        }
        isNearbyStops -> {
            contextLabelRes = R.string.nearby_stops_title
            contextIcon = Icons.Filled.NearMe
        }
        isSettings -> {
            contextLabelRes = R.string.settings
            contextIcon = Icons.Default.Settings
        }
    }
    val outerSnackbarHostState = remember { SnackbarHostState() }

    var arrivalsStopName by remember { mutableStateOf("") }
    var arrivalsIsFavorite by remember { mutableStateOf(false) }
    var arrivalsLat by remember { mutableDoubleStateOf(0.0) }
    var arrivalsLng by remember { mutableDoubleStateOf(0.0) }
    var arrivalsOnRefresh by remember { mutableStateOf<(() -> Unit)?>(null) }
    var onScrollToTop by remember { mutableStateOf<(() -> Unit)?>(null) }

    val topPagerState = rememberPagerState(pageCount = { bottomNavItems.size })
    var selectedStopsRoute by remember { mutableStateOf<Route?>(null) }
    var selectedStopsDirection by remember { mutableStateOf<Direction?>(null) }
    val saveableStateHolder = rememberSaveableStateHolder()

    fun navigateToArrivals(stop: Stop, routeId: Int) {
        val stopToRecord = if (routeId > 0) stop.copy(routeNum = routeId) else stop
        scope.launch {
            recentStopsRepository.addRecentStop(stopToRecord)
        }
        if (expandedPane) {
            detailStop = ArrivalsDestination(stop.locId, stop.desc, routeId, stop.latitude, stop.longitude)
        } else {
            navController.navigate(
                ArrivalsDestination(stop.locId, stop.desc, routeId, stop.latitude, stop.longitude)
            )
        }
    }

    // Widget taps arrive with stop/route/coords as intent extras (see WidgetLaunch).
    val widgetLaunchIntentValue = (LocalActivity.current as? MainActivity)?.widgetLaunchIntent?.value
    LaunchedEffect(widgetLaunchIntentValue) {
        val intent = widgetLaunchIntentValue ?: return@LaunchedEffect
        val stopId = intent.getLongExtra(WidgetLaunch.EXTRA_STOP_ID, -1L)
        if (stopId <= 0L) return@LaunchedEffect
        val stop = Stop(
            desc = intent.getStringExtra(WidgetLaunch.EXTRA_STOP_NAME).orEmpty(),
            latitude = intent.getDoubleExtra(WidgetLaunch.EXTRA_LAT, 0.0),
            longitude = intent.getDoubleExtra(WidgetLaunch.EXTRA_LNG, 0.0),
            transitType = "bus",
            locId = stopId.toInt(),
            routeNum = intent.getIntExtra(WidgetLaunch.EXTRA_ROUTE_ID, 0)
        )
        navigateToArrivals(stop, stop.routeNum)
    }

    fun onTopPageSelected(page: Int) {
        scope.launch {
            topPagerState.animateScrollToPage(page)
        }
    }

    fun navigateToTopPage(page: Int) {
        if (!isTopLevel) {
            navController.popBackStack(HomeDestination, inclusive = false)
        }
        onTopPageSelected(page)
    }

    // System back walks the top-level pager back to Favorites (page 0) before leaving the app
    BackHandler(enabled = isTopLevel && topPagerState.currentPage > 0) {
        scope.launch { topPagerState.animateScrollToPage(0) }
    }

    // Two-pane: system back first closes the arrivals detail pane, then falls back to pager/exit.
    BackHandler(enabled = expandedPane && isTopLevel && detailStop != null) {
        detailStop = null
    }

    /**
     * Toggles the favorite state for the stop currently shown, resolving its coordinates first if
     * the arrivals fetch is still in flight or failed. Shared by the phone top bar and the
     * two-pane detail header so both surfaces stay in sync.
     */
    fun toggleFavoriteFlow(locId: Int, stopName: String, routeId: Int) {
        scope.launch {
            var lat = arrivalsLat
            var lng = arrivalsLng
            if (!arrivalsIsFavorite && lat == 0.0 && lng == 0.0) {
                // Coords not resolved yet (fetch still in flight or offline):
                // resolve them now so the favorite isn't parked at 0,0.
                transitRepository.getStopById(locId)?.let {
                    lat = it.latitude
                    lng = it.longitude
                }
            }
            val result = toggleFavorite(favoritesRepository, context, locId, stopName, arrivalsIsFavorite, routeId, lat, lng)
            if (result.first) {
                arrivalsIsFavorite = !arrivalsIsFavorite
            }
            outerSnackbarHostState.showSnackbar(result.second)
        }
    }

    /**
     * Arrivals detail pane for the Expanded two-pane layout: a compact header (stop name, favorite,
     * refresh, close) above the shared ArrivalsScreen so the pane mirrors the phone top bar's actions.
     * Keyed by the selected stop so switching selections rebuilds the screen's local state.
     */
    val arrivalsDetailPane: @Composable (ArrivalsDestination) -> Unit = { dest ->
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = arrivalsStopName.ifBlank {
                        dest.stopName.ifBlank { stringResource(R.string.stop) }
                    },
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                val paneFavSource = remember(dest.stopId) { MutableInteractionSource() }
                IconButton(
                    onClick = { toggleFavoriteFlow(dest.stopId, dest.stopName, dest.routeId) },
                    interactionSource = paneFavSource,
                    modifier = Modifier.pressScale(paneFavSource)
                ) {
                    AnimatedContent(
                        targetState = arrivalsIsFavorite,
                        transitionSpec = { fadeIn(m3EffectsDefault()) togetherWith fadeOut(m3EffectsFast()) },
                        label = "paneFavoriteIcon"
                    ) { isFav ->
                        Icon(
                            imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (isFav) stringResource(R.string.remove_favorite) else stringResource(R.string.add_favorite),
                            tint = if (isFav) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                val paneRefreshSource = remember(dest.stopId) { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        scope.launch { refreshRotation.animateTo(refreshRotation.value + 360f, m3EffectsFast()) }
                        arrivalsOnRefresh?.invoke()
                    },
                    interactionSource = paneRefreshSource,
                    modifier = Modifier.pressScale(paneRefreshSource)
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.refresh),
                        modifier = Modifier.rotate(refreshRotation.value)
                    )
                }
                val paneCloseSource = remember(dest.stopId) { MutableInteractionSource() }
                IconButton(
                    onClick = { detailStop = null },
                    interactionSource = paneCloseSource,
                    modifier = Modifier.pressScale(paneCloseSource)
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = stringResource(R.string.close_detail),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            key(dest.stopId, dest.routeId) {
                ArrivalsScreen(
                    transitRepository = transitRepository,
                    favoritesRepository = favoritesRepository,
                    stopId = dest.stopId,
                    stopName = dest.stopName,
                    routeId = dest.routeId,
                    latitude = dest.lat,
                    longitude = dest.lng,
                    isDark = isDark,
                    onArrivalsStateChange = { name, fav, lat, lng ->
                        arrivalsStopName = name
                        arrivalsIsFavorite = fav
                        arrivalsLat = lat
                        arrivalsLng = lng
                    },
                    onRegisterRefresh = { arrivalsOnRefresh = it },
                    onRegisterScrollToTop = { onScrollToTop = it }
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (expandedPane) {
                MainNavigationRail(
                    topPage = topPagerState.currentPage,
                    onNavigate = ::navigateToTopPage,
                    onSettingsClick = {
                        navController.navigate(SettingsDestination) { launchSingleTop = true }
                    }
                )
            }
            Scaffold(
                modifier = if (expandedPane) Modifier.weight(1f) else Modifier.fillMaxSize(),
                contentWindowInsets = WindowInsets.safeDrawing
                    .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            topBar = {
                if (!isTopLevel) {
                AnimatedContent(
                    targetState = destination,
                    transitionSpec = {
                        (fadeIn(m3EffectsDefault()) +
                            slideInVertically(m3SpatialDefault()) { -it })
                            .togetherWith(
                                fadeOut(m3EffectsFast()) +
                                    slideOutVertically(m3SpatialFast()) { -it / 3 }
                            )
                    },
                    label = "topBar"
                ) { dest ->
                    when {
                        dest?.hasRoute<NearbyStopsDestination>() == true -> TopAppBar(
                        title = { Text(stringResource(R.string.nearby_stops_title)) },
                        navigationIcon = { BackNavigationIcon(onClick = { navController.popBackStack() }) },
                        contentPadding = PaddingValues(0.dp),
                        windowInsets = TopAppBarDefaults.windowInsets,
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    dest?.hasRoute<ArrivalsDestination>() == true && !inPip -> TopAppBar(
                        title = { Text(arrivalsStopName.ifBlank { stringResource(R.string.stop) }) },
                        navigationIcon = { BackNavigationIcon(onClick = { navController.popBackStack() }) },
                        contentPadding = PaddingValues(0.dp),
                        windowInsets = TopAppBarDefaults.windowInsets,
                        actions = {
                            val pipSource = remember { MutableInteractionSource() }
                            var pipButtonRect by remember { mutableStateOf<android.graphics.Rect?>(null) }
                            IconButton(
                                onClick = {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(2, 3))
                                        .setAutoEnterEnabled(true)
                                        .apply { pipButtonRect?.let { setSourceRectHint(it) } }
                                        .build()
                                    pipActivity.setPictureInPictureParams(params)
                                    pipActivity.enterPictureInPictureMode(params)
                                },
                                interactionSource = pipSource,
                                modifier = Modifier
                                    .pressScale(pipSource)
                                    .onGloballyPositioned { coords ->
                                        val pos = coords.positionInWindow()
                                        pipButtonRect = android.graphics.Rect(
                                            pos.x.roundToInt(),
                                            pos.y.roundToInt(),
                                            (pos.x + coords.size.width).roundToInt(),
                                            (pos.y + coords.size.height).roundToInt()
                                        )
                                    }
                            ) {
                                Icon(
                                    Icons.Outlined.PictureInPictureAlt,
                                    contentDescription = stringResource(R.string.mini_window),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            val favSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = {
                                    val entry = currentBackStackEntry
                                    toggleFavoriteFlow(
                                        entry?.arguments?.getInt("stopId") ?: 0,
                                        entry?.arguments?.getString("stopName") ?: "",
                                        entry?.arguments?.getInt("routeId") ?: -1
                                    )
                                },
                                interactionSource = favSource,
                                modifier = Modifier.pressScale(favSource)
                            ) {
                                AnimatedContent(
                                    targetState = arrivalsIsFavorite,
                                    transitionSpec = { fadeIn(m3EffectsDefault()) togetherWith fadeOut(m3EffectsFast()) },
                                    label = "favoriteIcon"
                                ) { isFav ->
                                    Icon(
                                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = if (isFav) stringResource(R.string.remove_favorite) else stringResource(R.string.add_favorite),
                                        tint = if (isFav) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            val refreshSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = {
                                    scope.launch { refreshRotation.animateTo(refreshRotation.value + 360f, m3EffectsFast()) }
                                    arrivalsOnRefresh?.invoke()
                                },
                                interactionSource = refreshSource,
                                modifier = Modifier.pressScale(refreshSource)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = stringResource(R.string.refresh),
                                    modifier = Modifier.rotate(refreshRotation.value)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                            actionIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    else -> {}
                }
                }
            }
            },
            bottomBar = {},
            snackbarHost = {
                SnackbarHost(
                    hostState = outerSnackbarHostState,
                    modifier = Modifier
                        .windowInsetsPadding(WindowInsets.navigationBars)
                        .padding(bottom = 56.dp),
                    snackbar = { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = if (isDark) {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            } else {
                                SnackbarDefaults.color
                            },
                            contentColor = if (isDark) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                SnackbarDefaults.contentColor
                            }
                        )
                    }
                )
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = HomeDestination,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
                enterTransition = { navEnter },
                exitTransition = { navExit },
                popEnterTransition = { navPopEnter },
                popExitTransition = { navPopExit }
            ) {
                composable<HomeDestination>(exitTransition = { navExitQuick }) {
                    val topLevelPage: @Composable (Int) -> Unit = { page ->
                        saveableStateHolder.SaveableStateProvider(page) {
                            when (page) {
                                0 -> FavoritesScreen(
                                    favoritesRepository = favoritesRepository,
                                    transitRepository = transitRepository,
                                    onNavigateToArrivals = { stop: Stop ->
                                        navigateToArrivals(stop, stop.routeNum)
                                    }
                                )
                                1 -> RecentStopsScreen(
                                    recentStopsRepository = recentStopsRepository,
                                    onNavigateToArrivals = { stop: Stop ->
                                        navigateToArrivals(stop, stop.routeNum)
                                    }
                                )
                                2 -> StopsScreen(
                                    transitRepository = transitRepository,
                                    selectedRoute = selectedStopsRoute,
                                    selectedDirection = selectedStopsDirection,
                                    onRouteToggle = { route ->
                                        selectedStopsRoute = if (selectedStopsRoute?.routeId == route.routeId) null else route
                                        selectedStopsDirection = null
                                    },
                                    onDirectionToggle = { direction ->
                                        selectedStopsDirection = if (selectedStopsDirection?.dir == direction.dir) null else direction
                                    },
                                    onNavigateToArrivals = { stop: Stop, routeId: Int ->
                                        navigateToArrivals(stop, routeId)
                                    }
                                )
                                3 -> TripPlannerScreen(
                                    transitRepository = transitRepository,
                                    pageVisible = topPagerState.currentPage == page,
                                    isDark = isDark
                                )
                            }
                        }
                    }
                    if (expandedPane) {
                        // Master-detail split: browsing list on the left, chosen stop's arrivals on the right.
                        Row(modifier = Modifier.fillMaxSize()) {
                            HorizontalPager(
                                state = topPagerState,
                                modifier = Modifier
                                    .weight(1.15f)
                                    .fillMaxHeight(),
                                beyondViewportPageCount = 1
                            ) { page ->
                                topLevelPage(page)
                            }
                            VerticalDivider(modifier = Modifier.fillMaxHeight())
                            val dest = detailStop
                            if (dest == null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.expanded_arrivals_placeholder),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .padding(start = 8.dp, end = 16.dp)
                                ) {
                                    arrivalsDetailPane(dest)
                                }
                            }
                        }
                    } else {
                        HorizontalPager(
                            state = topPagerState,
                            modifier = Modifier.fillMaxSize(),
                            beyondViewportPageCount = 1
                        ) { page ->
                            topLevelPage(page)
                        }
                    }
                }
                composable<SettingsDestination> {
                    SettingsScreen(
                        widgetSection = { WidgetSettingsSection() },
                        notificationsSection = { DepartureAlertsSection() },
                        onRegisterScrollToTop = { onScrollToTop = it }
                    )
                }
                composable<NearbyStopsDestination> {
                    NearbyStopsScreen(
                        transitRepository = transitRepository,
                        onNavigateToArrivals = { stop: Stop, routeId: Int ->
                            navigateToArrivals(stop, routeId)
                        },
                        onRegisterScrollToTop = { onScrollToTop = it }
                    )
                }
                composable<ArrivalsDestination>(enterTransition = { navEnterArrivals }) { backStackEntry ->
                    val dest: ArrivalsDestination = backStackEntry.toRoute()
                    ArrivalsScreen(
                        transitRepository = transitRepository,
                        favoritesRepository = favoritesRepository,
                        stopId = dest.stopId,
                        stopName = dest.stopName,
                        routeId = dest.routeId,
                        latitude = dest.lat,
                        longitude = dest.lng,
                        isDark = isDark,
                        onArrivalsStateChange = { name, fav, lat, lng ->
                            arrivalsStopName = name
                            arrivalsIsFavorite = fav
                            arrivalsLat = lat
                            arrivalsLng = lng
                        },
                        onRegisterRefresh = { arrivalsOnRefresh = it },
                        onRegisterScrollToTop = { onScrollToTop = it }
                    )
                }
            }
        }
        }
    AnimatedVisibility(
        visible = !inPip && !expandedPane,
        enter = slideInVertically(m3SpatialDefault()) { it } +
            fadeIn(m3EffectsDefault()),
        exit = slideOutVertically(m3SpatialFast()) { it } +
            fadeOut(m3EffectsFast()),
        modifier = Modifier.align(Alignment.BottomCenter)
    ) {
        @SuppressLint("FrequentlyChangingValue")
        val pagePosition = topPagerState.currentPage +
            topPagerState.currentPageOffsetFraction
        MainBottomBar(
            topPage = topPagerState.currentPage,
            pagePosition = pagePosition,
            onNavigate = ::navigateToTopPage,
            onSettingsClick = {
                navController.navigate(SettingsDestination) { launchSingleTop = true }
            },
            showBack = !isTopLevel,
            onBackClick = { navController.popBackStack() },
            onContextClick = { onScrollToTop?.invoke() },
            contextLabelRes = contextLabelRes,
            contextIcon = contextIcon
        )
    }
}

}

