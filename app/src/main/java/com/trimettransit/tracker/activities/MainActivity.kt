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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.PictureInPictureAlt
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import com.trimettransit.tracker.activities.toggleFavorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.trimettransit.tracker.ui.components.findActivity
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberIsInPipMode
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.FavoritesRepositoryImpl
import com.trimettransit.tracker.data.local.RecentStopsRepositoryImpl
import com.trimettransit.tracker.transit.TransitRepositoryImpl
import com.trimettransit.tracker.widget.WidgetScheduler
import com.trimettransit.tracker.widget.WidgetLaunch
import com.trimettransit.tracker.widget.settings.WidgetSettingsSection
import kotlinx.coroutines.launch
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
import com.trimettransit.tracker.ui.theme.LocalCardStyle
import com.trimettransit.tracker.ui.theme.TriMetGoTheme
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3EffectsFast
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.ui.theme.m3SpatialFast
import com.trimettransit.tracker.ui.theme.m3SpatialSlow
import androidx.annotation.StringRes
import com.trimettransit.tracker.R
import android.net.Uri

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

private data class BottomNavItem(
    val pageIndex: Int,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val iconSize: Dp = 24.dp
)

private val bottomNavItems = listOf(
    BottomNavItem(0, R.string.nav_favorites, Icons.Filled.Favorite),
    BottomNavItem(1, R.string.nav_recent, Icons.Filled.History),
    BottomNavItem(2, R.string.nav_routes, Icons.Filled.Map),
    BottomNavItem(3, R.string.nav_trips, Icons.Filled.Directions)
)

// Navigation route literals, shared by the NavHost registration and every navigate()/popBackStack().
private const val ROUTE_HOME = "home"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_NEARBY_STOPS = "nearby_stops"
private const val ROUTE_ARRIVALS_PREFIX = "arrivals/"
private const val ROUTE_ARRIVALS =
    "arrivals/{stopId}?stopName={stopName}&routeId={routeId}&lat={lat}&lng={lng}"

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainBottomBar(
    topPage: Int,
    pagePosition: Float,
    onNavigate: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    onContextClick: () -> Unit = {},
    contextLabelRes: Int? = null,
    contextIcon: ImageVector? = null
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val itemHeight = 40.dp
    val shouldHideLabel = fontScale > 1.25f ||
            windowInfo.containerSize.width < with(density) { 360.dp.roundToPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showBack) {
                PillActionButton(
                    onClick = onBackClick,
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    size = itemHeight
                )
            }
            Surface(
                shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                AnimatedContent(
                    targetState = contextLabelRes == null,
                    transitionSpec = {
                        (fadeIn(m3EffectsDefault()) + scaleIn(initialScale = 0.85f, animationSpec = m3SpatialDefault())) togetherWith
                            (fadeOut(m3EffectsFast()) + scaleOut(targetScale = 0.85f, animationSpec = m3SpatialFast()))
                    },
                    label = "nav_collapse"
                ) { isTopLevel ->
                    if (isTopLevel) {
                        MainTabRow(
                            topPage = topPage,
                            items = bottomNavItems,
                            itemHeight = itemHeight,
                            shouldHideLabel = shouldHideLabel,
                            onNavigate = onNavigate,
                            pagePosition = pagePosition
                        )
                    } else {
                        CompactContextPill(
                            contextLabelRes = contextLabelRes ?: R.string.nav_arrivals,
                            contextIcon = contextIcon,
                            onClick = onContextClick,
                            itemHeight = itemHeight
                        )
                    }
                }
            }
            PillActionButton(
                onClick = onSettingsClick,
                icon = Icons.Default.Settings,
                contentDescription = stringResource(R.string.settings),
                shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                size = itemHeight
            )
        }
    }
}

private data class PillBounds(val x: Int, val width: Int)

@Composable
private fun PillActionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    shape: Shape,
    containerColor: Color,
    contentColor: Color,
    size: Dp
) {
    Surface(
        shape = shape,
        color = containerColor,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        val source = remember { MutableInteractionSource() }
        IconButton(
            onClick = onClick,
            interactionSource = source,
            modifier = Modifier
                .size(size)
                .pressScale(source, 0.92f)
        ) {
            Icon(icon, contentDescription = contentDescription, tint = contentColor)
        }
    }
}

@Composable
private fun MainTabRow(
    topPage: Int,
    items: List<BottomNavItem>,
    itemHeight: Dp,
    shouldHideLabel: Boolean,
    onNavigate: (Int) -> Unit,
    pagePosition: Float
) {
    val bounds = remember { mutableStateMapOf<Int, PillBounds>() }
    var boxLeft by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    // Continuous page position (currentPage + drag/fling offset fraction) drives the
    // pill so it tracks finger swipes frame-by-frame instead of snapping on settle.
    val maxIndex = items.lastIndex
    val position = pagePosition.coerceIn(0f, maxIndex.toFloat())
    val fromPage = floor(position).toInt().coerceIn(0, maxIndex)
    val toPage = ceil(position).toInt().coerceIn(0, maxIndex)
    val fraction = position - fromPage
    val fromBounds = bounds[fromPage]
    val toBounds = bounds[toPage]
    val pillTarget = when {
        fromBounds == null -> toBounds
        toBounds == null -> fromBounds
        else -> PillBounds(
            x = fromBounds.x + ((toBounds.x - fromBounds.x) * fraction).roundToInt(),
            width = fromBounds.width + ((toBounds.width - fromBounds.width) * fraction).roundToInt()
        )
    }

    val indicatorOffset = with(density) { (pillTarget?.x ?: 0).toDp() }
    val indicatorWidth = with(density) { (pillTarget?.width ?: 0).toDp() }

    Box(
        modifier = Modifier
            .padding(8.dp)
            .onGloballyPositioned { coords ->
                boxLeft = coords.positionInWindow().x.roundToInt()
            }
    ) {
        if (pillTarget != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset { IntOffset(indicatorOffset.roundToPx(), 0) }
            ) {
                Box(
                    modifier = Modifier
                        .width(indicatorWidth)
                        .height(itemHeight)
                        .clip(RoundedCornerShape(LocalCardStyle.current.cornerRadius))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { index, item ->
                val isSelected = topPage == item.pageIndex
                val icon = item.icon
                val labelRes = item.labelRes

                val labelWidth by animateDpAsState(
                    targetValue = if (isSelected && !shouldHideLabel) 80.dp else 0.dp,
                    animationSpec = m3SpatialSlow(),
                    label = "label_width_$index"
                )

                val itemSource = remember { MutableInteractionSource() }
                IconButton(
                    onClick = {
                        if (item.pageIndex != topPage) onNavigate(item.pageIndex)
                    },
                    interactionSource = itemSource,
                    modifier = Modifier
                        .width(48.dp + labelWidth)
                        .height(itemHeight)
                        .onGloballyPositioned { coords ->
                            val pos = coords.positionInWindow()
                            bounds[index] = PillBounds(
                                x = pos.x.roundToInt() - boxLeft,
                                width = coords.size.width
                            )
                        }
                        .pressScale(itemSource, 0.92f)
                        .semantics {
                            role = Role.Tab
                            selected = isSelected
                        },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = stringResource(labelRes),
                            tint = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.size(item.iconSize)
                        )
                        if (isSelected && !shouldHideLabel) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactContextPill(
    contextLabelRes: Int,
    contextIcon: ImageVector?,
    onClick: () -> Unit,
    itemHeight: Dp
) {
    Surface(
        shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
        color = MaterialTheme.colorScheme.primaryContainer,
        shadowElevation = 8.dp,
        tonalElevation = 4.dp
    ) {
        val source = remember { MutableInteractionSource() }
        IconButton(
            onClick = onClick,
            interactionSource = source,
            modifier = Modifier
                .size(itemHeight)
                .pressScale(source, 0.92f)
        ) {
            Icon(
                imageVector = contextIcon ?: Icons.Default.Settings,
                contentDescription = stringResource(contextLabelRes),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
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
            var themePref by remember { mutableStateOf(prefs.getString("theme", "system") ?: "system") }
            var dynamicColorPref by remember { mutableStateOf(prefs.getBoolean("pref_key_dynamic_color", true)) }
            var cardCornerRadiusPref by remember { mutableIntStateOf(prefs.getInt("pref_key_card_corner_radius", 16)) }
            var cardOutlinesPref by remember { mutableStateOf(prefs.getBoolean("pref_key_card_outlines", true)) }
            var cardOutlineColorPref by remember {
                mutableStateOf(prefs.getString("pref_key_card_outline_color", "auto") ?: "auto")
            }
            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "theme" -> themePref = prefs.getString("theme", "system") ?: "system"
                        "pref_key_dynamic_color" -> dynamicColorPref = prefs.getBoolean("pref_key_dynamic_color", true)
                        "pref_key_card_corner_radius" -> cardCornerRadiusPref = prefs.getInt("pref_key_card_corner_radius", 16)
                        "pref_key_card_outlines" -> cardOutlinesPref = prefs.getBoolean("pref_key_card_outlines", true)
                        "pref_key_card_outline_color" -> cardOutlineColorPref = prefs.getString("pref_key_card_outline_color", "auto") ?: "auto"
                    }
                }
                prefs.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
            }
            val isDark = when (themePref) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            TriMetGoTheme(
                darkTheme = isDark,
                dynamicColor = dynamicColorPref,
                cardCornerRadius = cardCornerRadiusPref.dp,
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
                MainAppContent(isDark = isDark)
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
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val refreshRotation = remember { Animatable(0f) }
    val currentRoute = currentBackStackEntry?.destination?.route ?: ""
    val isTopLevel = currentRoute == ROUTE_HOME

    // Manual dependency wiring: one shared DatabaseHelper drives both local-data repos.
    val appContext = context.applicationContext
    val favoritesRepository = remember { FavoritesRepositoryImpl(DatabaseHelper(appContext)) }
    val recentStopsRepository = remember { RecentStopsRepositoryImpl(DatabaseHelper(appContext)) }
    val transitRepository = remember { TransitRepositoryImpl(appContext) }
    // Sub-screens brand the collapsed pill with their own icon and name.
    var contextLabelRes: Int? = null
    var contextIcon: ImageVector? = null
    when {
        currentRoute.startsWith(ROUTE_ARRIVALS_PREFIX) -> {
            contextLabelRes = R.string.nav_arrivals
            contextIcon = Icons.Filled.Schedule
        }
        currentRoute == ROUTE_NEARBY_STOPS -> {
            contextLabelRes = R.string.nearby_stops_title
            contextIcon = Icons.Filled.NearMe
        }
        currentRoute == ROUTE_SETTINGS -> {
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
        navController.navigate(
            "$ROUTE_ARRIVALS_PREFIX${stop.locId}?stopName=${Uri.encode(stop.desc)}&routeId=$routeId&lat=${stop.latitude}&lng=${stop.longitude}"
        )
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
        if (currentRoute != ROUTE_HOME) {
            navController.popBackStack(ROUTE_HOME, inclusive = false)
        }
        onTopPageSelected(page)
    }

    // System back walks the top-level pager back to Favorites (page 0) before leaving the app
    BackHandler(enabled = isTopLevel && topPagerState.currentPage > 0) {
        scope.launch { topPagerState.animateScrollToPage(0) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
                .only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
            topBar = {
                if (!isTopLevel) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        (fadeIn(m3EffectsDefault()) +
                            slideInVertically(m3SpatialDefault()) { -it })
                            .togetherWith(
                                fadeOut(m3EffectsFast()) +
                                    slideOutVertically(m3SpatialFast()) { -it / 3 }
                            )
                    },
                    label = "topBar"
                ) { route ->
                    when {
                        route == ROUTE_NEARBY_STOPS -> TopAppBar(
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
                    route.startsWith(ROUTE_ARRIVALS_PREFIX) && !inPip -> TopAppBar(
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
                                    val locId = entry?.arguments?.getString("stopId")?.toIntOrNull() ?: 0
                                    val stopName = entry?.arguments?.getString("stopName") ?: ""
                                    scope.launch {
                                        val routeId = entry?.arguments?.getInt("routeId") ?: -1
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
                startDestination = ROUTE_HOME,
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
                enterTransition = { navEnter },
                exitTransition = { navExit },
                popEnterTransition = { navPopEnter },
                popExitTransition = { navPopExit }
            ) {
                composable(ROUTE_HOME, exitTransition = { navExitQuick }) {
                    HorizontalPager(
                        state = topPagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
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
                }
                composable(ROUTE_SETTINGS) {
                    SettingsScreen(
                        widgetSection = { WidgetSettingsSection() },
                        onRegisterScrollToTop = { onScrollToTop = it }
                    )
                }
                composable(ROUTE_NEARBY_STOPS) {
                    NearbyStopsScreen(
                        transitRepository = transitRepository,
                        onNavigateToArrivals = { stop: Stop, routeId: Int ->
                            navigateToArrivals(stop, routeId)
                        },
                        onRegisterScrollToTop = { onScrollToTop = it }
                    )
                }
                composable(
                    route = ROUTE_ARRIVALS,
                    arguments = listOf(
                        navArgument("stopId") { type = NavType.StringType },
                        navArgument("stopName") { type = NavType.StringType; defaultValue = "" },
                        navArgument("routeId") { type = NavType.IntType; defaultValue = -1 },
                        navArgument("lat") { type = NavType.StringType; defaultValue = "" },
                        navArgument("lng") { type = NavType.StringType; defaultValue = "" }
                    ),
                    enterTransition = { navEnterArrivals }
                ) { backStackEntry ->
                    ArrivalsScreen(
                        transitRepository = transitRepository,
                        favoritesRepository = favoritesRepository,
                        stopId = backStackEntry.arguments?.getString("stopId") ?: "",
                        stopName = backStackEntry.arguments?.getString("stopName") ?: "",
                        routeId = backStackEntry.arguments?.getInt("routeId") ?: -1,
                        latitude = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0,
                        longitude = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0,
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
        AnimatedVisibility(
            visible = !inPip,
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
                    navController.navigate(ROUTE_SETTINGS) { launchSingleTop = true }
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

