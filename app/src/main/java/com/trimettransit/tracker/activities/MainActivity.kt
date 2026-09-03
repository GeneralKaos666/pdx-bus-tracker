package com.trimettransit.tracker.activities

import android.app.PictureInPictureParams
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Map
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
import com.trimettransit.tracker.ui.NavState
import com.trimettransit.tracker.feature.arrivals.toggleFavorite
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.trimettransit.tracker.ui.components.findActivity
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.rememberIsInPipMode
import androidx.compose.runtime.setValue
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
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
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
import kotlinx.coroutines.Dispatchers
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
import com.trimettransit.tracker.feature.vehicles.WhatsNearbyScreen
import com.trimettransit.tracker.ui.theme.TriMetGoTheme
import android.net.Uri

private val AnimatedContentTransitionScope<*>.navEnter: EnterTransition
    get() = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    ) + fadeIn(
        initialAlpha = 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    )

private val AnimatedContentTransitionScope<*>.navExit: ExitTransition
    get() = slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    ) + fadeOut(
        targetAlpha = 0.7f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )

private val AnimatedContentTransitionScope<*>.navPopEnter: EnterTransition
    get() = slideInHorizontally(
        initialOffsetX = { -it },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    ) + fadeIn(
        initialAlpha = 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
    )

private val AnimatedContentTransitionScope<*>.navPopExit: ExitTransition
    get() = slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    ) + fadeOut(
        targetAlpha = 0.7f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )

/**
 * Enter transition for the Arrivals destination: a stiffer spring than the
 * default [navEnter] so pushing to Arrivals from Home/Routes feels a hair
 * snappier (settles well under the 300ms [navExitQuick] that bounds it).
 */
private val AnimatedContentTransitionScope<*>.navEnterArrivals: EnterTransition
    get() = slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    ) + fadeIn(
        initialAlpha = 0.7f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    )

/**
 * Slightly shorter exit (350 → 300ms) for the Home and Routes destinations so
 * the push to Arrivals reads tighter; same slide+fade shape as [navExit].
 */
private val AnimatedContentTransitionScope<*>.navExitQuick: ExitTransition
    get() = slideOutHorizontally(
        targetOffsetX = { -it },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    ) + fadeOut(
        targetAlpha = 0.7f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

private data class BottomNavItem(
    val pageIndex: Int,
    val label: String,
    val icon: ImageVector,
    val iconSize: Dp = 24.dp
)

private val bottomNavItems = listOf(
    BottomNavItem(0, "Favorites", Icons.Filled.Favorite),
    BottomNavItem(1, "Recent", Icons.Filled.History),
    BottomNavItem(2, "Routes", Icons.Filled.Map),
    BottomNavItem(3, "What's Nearby", Icons.Filled.DirectionsBus),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainBottomBar(
    topPage: Int,
    onNavigate: (Int) -> Unit,
    onSettingsClick: () -> Unit,
    showBack: Boolean = false,
    onBackClick: () -> Unit = {},
    compact: Boolean = false,
    collapsed: Boolean = false,
    collapsedItem: BottomNavItem? = null
) {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val fontScale = density.fontScale
    val itemHeight by animateDpAsState(
        targetValue = if (compact) 44.dp else 48.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "nav_item_height"
    )

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
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp
                ) {
                    val backSource = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onBackClick,
                        interactionSource = backSource,
                        modifier = Modifier
                            .size(48.dp)
                            .pressScale(backSource, 0.92f)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 8.dp,
                tonalElevation = 4.dp
            ) {
                AnimatedContent(
                    targetState = collapsed,
                    transitionSpec = {
                        (fadeIn(spring()) + scaleIn(initialScale = 0.85f, animationSpec = spring())) togetherWith
                            (fadeOut(spring()) + scaleOut(targetScale = 0.85f, animationSpec = spring()))
                    },
                    label = "nav_collapse"
                ) { collapsedState ->
                    val items = when {
                        !collapsedState -> bottomNavItems
                        collapsedItem != null -> listOf(collapsedItem)
                        else -> listOf(bottomNavItems[topPage.coerceIn(0, bottomNavItems.lastIndex)])
                    }
                    val shouldHideLabel = fontScale > 1.25f ||
                            (windowInfo.containerSize.width < with(density) { 400.dp.roundToPx() } && items.size > 3)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        items.forEachIndexed { index, item ->
                            val isSelected = (collapsedState && items.size == 1) || topPage == item.pageIndex

                            val labelWidth by animateDpAsState(
                                targetValue = if (isSelected && !shouldHideLabel) 80.dp else 0.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "label_width_$index"
                            )

                            val spacerWidth by animateDpAsState(
                                targetValue = if (index < items.size - 1) 8.dp else 0.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "spacer_width_$index"
                            )

                            IconButton(
                                onClick = {
                                    if (item.pageIndex >= 0 && (collapsed || item.pageIndex != topPage)) {
                                        onNavigate(item.pageIndex)
                                    }
                                },
                                modifier = Modifier
                                    .width(48.dp + labelWidth)
                                    .height(itemHeight),
                                colors = if (isSelected) {
                                    IconButtonDefaults.filledIconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onSurface,
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    )
                                } else {
                                    IconButtonDefaults.iconButtonColors(
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.label,
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
                                            text = item.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            if (index < items.size - 1) {
                                Spacer(modifier = Modifier.width(spacerWidth))
                            }
                        }
                    }
                }
            }
            if (!showBack) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shadowElevation = 8.dp,
                    tonalElevation = 4.dp
                ) {
                    val bubbleSource = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onSettingsClick,
                        interactionSource = bubbleSource,
                        modifier = Modifier
                            .size(48.dp)
                            .pressScale(bubbleSource, 0.92f)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            DisposableEffect(prefs) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    when (key) {
                        "theme" -> themePref = prefs.getString("theme", "system") ?: "system"
                        "pref_key_dynamic_color" -> dynamicColorPref = prefs.getBoolean("pref_key_dynamic_color", true)
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
            TriMetGoTheme(darkTheme = isDark, dynamicColor = dynamicColorPref) {
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
    val isTopLevel = currentRoute == "home"

    // Manual dependency wiring: one shared DatabaseHelper drives both local-data repos.
    val appContext = context.applicationContext
    val favoritesRepository = remember { FavoritesRepositoryImpl(DatabaseHelper(appContext)) }
    val recentStopsRepository = remember { RecentStopsRepositoryImpl(DatabaseHelper(appContext)) }
    val transitRepository = remember { TransitRepositoryImpl(appContext) }
    // Sub-screens brand the collapsed pill with their own icon instead of
    // whichever Home page was last open; pageIndex -1 makes it inert (back bubble navigates).
    val collapsedNavItem = when {
        currentRoute.startsWith("arrivals/") -> BottomNavItem(-1, "Arrivals", Icons.Filled.Schedule)
        currentRoute == "nearby_stops" -> BottomNavItem(-1, "Nearby Stops", Icons.Filled.NearMe)
        currentRoute == "settings" -> BottomNavItem(-1, "Settings", Icons.Default.Settings)
        else -> null
    }
    val outerSnackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(currentRoute) { NavState.bottomBarVisible = true }
    val topPagerState = rememberPagerState(pageCount = { bottomNavItems.size })
    var selectedStopsRoute by remember { mutableStateOf<Route?>(null) }
    var selectedStopsDirection by remember { mutableStateOf<Direction?>(null) }

    fun navigateToArrivals(stop: Stop, routeId: Int) {
        val stopToRecord = if (routeId > 0) stop.copy(routeNum = routeId) else stop
        scope.launch(Dispatchers.IO) {
            DatabaseHelper(context.applicationContext).addRecentStop(stopToRecord)
        }
        navController.navigate(
            "arrivals/${stop.locId}?stopName=${Uri.encode(stop.desc)}&routeId=$routeId&lat=${stop.latitude}&lng=${stop.longitude}"
        )
    }

    fun onTopPageSelected(page: Int) {
        scope.launch {
            topPagerState.animateScrollToPage(page)
        }
    }

    fun navigateToTopPage(page: Int) {
        if (currentRoute != "home") {
            navController.popBackStack("home", inclusive = false)
        }
        onTopPageSelected(page)
    }

    // System back walks the top-level pager back to Favorites (page 0) before leaving the app
    BackHandler(enabled = isTopLevel && topPagerState.currentPage > 0) {
        scope.launch { topPagerState.animateScrollToPage(0) }
    }

    Scaffold(
            topBar = {
                if (!isTopLevel) {
                AnimatedContent(
                    targetState = currentRoute,
                    transitionSpec = {
                        (fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(durationMillis = 250, easing = FastOutSlowInEasing)) { -it })
                            .togetherWith(
                                fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                                    slideOutVertically(tween(durationMillis = 180, easing = FastOutSlowInEasing)) { -it / 3 }
                            )
                    },
                    label = "topBar"
                ) { route ->
                    when {
                        route == "nearby_stops" -> TopAppBar(
                        title = { Text("Nearby Stops") },
                        navigationIcon = {
                            val backSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = { navController.popBackStack() },
                                interactionSource = backSource,
                                modifier = Modifier.pressScale(backSource)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    route.startsWith("arrivals/") && !inPip -> TopAppBar(
                        title = { Text(NavState.arrivalsStopName.ifBlank { "Stop" }) },
                        navigationIcon = {
                            val backSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = { navController.popBackStack() },
                                interactionSource = backSource,
                                modifier = Modifier.pressScale(backSource)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        },
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
                                    contentDescription = "Mini window",
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
                                        var lat = NavState.arrivalsLat
                                        var lng = NavState.arrivalsLng
                                        if (!NavState.arrivalsIsFavorite && lat == 0.0 && lng == 0.0) {
                                            // Coords not resolved yet (fetch still in flight or offline):
                                            // resolve them now so the favorite isn't parked at 0,0.
                                            transitRepository.getStopById(locId)?.let {
                                                lat = it.latitude
                                                lng = it.longitude
                                            }
                                        }
                                        val result = toggleFavorite(favoritesRepository, context, locId, stopName, NavState.arrivalsIsFavorite, routeId, lat, lng)
                                        if (result.first) {
                                            NavState.arrivalsIsFavorite = !NavState.arrivalsIsFavorite
                                        }
                                        outerSnackbarHostState.showSnackbar(result.second)
                                    }
                                },
                                interactionSource = favSource,
                                modifier = Modifier.pressScale(favSource)
                            ) {
                                AnimatedContent(
                                    targetState = NavState.arrivalsIsFavorite,
                                    transitionSpec = { fadeIn(tween(durationMillis = 300, easing = FastOutSlowInEasing)) togetherWith fadeOut(tween(durationMillis = 300, easing = FastOutSlowInEasing)) },
                                    label = "favoriteIcon"
                                ) { isFav ->
                                    Icon(
                                        imageVector = if (isFav) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                        contentDescription = if (isFav) "Remove favorite" else "Add favorite",
                                        tint = if (isFav) MaterialTheme.colorScheme.error
                                                else MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            val refreshSource = remember { MutableInteractionSource() }
                            IconButton(
                                onClick = {
                                    scope.launch { refreshRotation.animateTo(refreshRotation.value + 360f, tween(durationMillis = 350, easing = FastOutSlowInEasing)) }
                                    NavState.arrivalsOnRefresh?.invoke()
                                },
                                interactionSource = refreshSource,
                                modifier = Modifier.pressScale(refreshSource)
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Refresh",
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
            bottomBar = {
                AnimatedVisibility(
                    visible = !inPip && NavState.bottomBarVisible,
                    enter = slideInVertically(tween(durationMillis = 250, easing = FastOutSlowInEasing)) { it } +
                        fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(tween(durationMillis = 180, easing = FastOutSlowInEasing)) { it } +
                        fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing))
                ) {
                    MainBottomBar(
                        topPage = topPagerState.currentPage,
                        onNavigate = ::navigateToTopPage,
                        onSettingsClick = {
                            // launchSingleTop: a fast double-tap must not push two settings entries.
                            navController.navigate("settings") { launchSingleTop = true }
                        },
                        showBack = !isTopLevel,
                        onBackClick = { navController.popBackStack() },
                        compact = currentRoute.startsWith("arrivals/"),
                        collapsed = !isTopLevel,
                        collapsedItem = collapsedNavItem
                    )
                }
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = outerSnackbarHostState,
                    modifier = Modifier.padding(bottom = 64.dp),
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
                startDestination = "home",
                modifier = Modifier
                    .padding(padding)
                    .consumeWindowInsets(padding),
                enterTransition = { navEnter },
                exitTransition = { navExit },
                popEnterTransition = { navPopEnter },
                popExitTransition = { navPopExit }
            ) {
                composable("home", exitTransition = { navExitQuick }) {
                    HorizontalPager(
                        state = topPagerState,
                        modifier = Modifier.fillMaxSize(),
                        beyondViewportPageCount = 1
                    ) { page ->
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
                            3 -> WhatsNearbyScreen(
                                transitRepository = transitRepository,
                                pageVisible = topPagerState.currentPage == 3,
                                onNavigateToArrivals = { stop: Stop, _: Int ->
                                    navigateToArrivals(stop, -1)
                                },
                                isDark = isDark
                            )
                        }
                    }
                }
                composable("settings") {
                    SettingsScreen()
                }
                composable("nearby_stops") {
                    NearbyStopsScreen(
                        transitRepository = transitRepository,
                        onNavigateToArrivals = { stop: Stop, routeId: Int ->
                            navigateToArrivals(stop, routeId)
                        }
                    )
                }
                composable(
                    route = "arrivals/{stopId}?stopName={stopName}&routeId={routeId}&lat={lat}&lng={lng}",
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
                    )
                }
            }
        }

}

