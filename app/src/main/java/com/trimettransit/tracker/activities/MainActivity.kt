package com.trimettransit.tracker.activities

import android.app.PictureInPictureParams
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
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
import com.trimettransit.tracker.ui.NavState
import com.trimettransit.tracker.feature.arrivals.toggleFavorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
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
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.data.local.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.feature.arrivals.ArrivalsScreen
import com.trimettransit.tracker.feature.home.HomeScreen
import com.trimettransit.tracker.feature.search.SearchStopsScreen
import com.trimettransit.tracker.feature.settings.SettingsScreen
import com.trimettransit.tracker.feature.stops.NearbyStopsScreen
import com.trimettransit.tracker.feature.stops.StopsScreen
import com.trimettransit.tracker.feature.vehicles.WhatsNearbyScreen
import com.trimettransit.tracker.ui.theme.TriMetGoTheme
import java.net.URLEncoder

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
    val tabIndex: Int = -1
)

private val bottomNavItems = listOf(
    BottomNavItem(0, "Home", Icons.Filled.Home),
    BottomNavItem(1, "Routes", Icons.Filled.Map),
    BottomNavItem(2, "What's Nearby", Icons.Filled.DirectionsBus),
    BottomNavItem(3, "Search", Icons.Filled.Search),
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MainBottomBar(
    topPage: Int,
    onNavigate: (Int) -> Unit,
    homePagerState: PagerState,
    onSettingsClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val fontScale = LocalDensity.current.fontScale
    val scope = rememberCoroutineScope()
    val items = if (topPage == 0) {
        listOf(
            BottomNavItem(0, "Favorites", Icons.Filled.Favorite, tabIndex = 0),
            BottomNavItem(0, "Recent", Icons.Filled.History, tabIndex = 1)
        ) + bottomNavItems.filter { it.pageIndex != 0 }
    } else {
        bottomNavItems
    }
    val shouldHideLabel = fontScale > 1.25f || (configuration.screenWidthDp < 400 && items.size > 3)

    HorizontalFloatingToolbar(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            expanded = true,
            floatingActionButton = {
                val fabSource = remember { MutableInteractionSource() }
                FloatingToolbarDefaults.VibrantFloatingActionButton(
                    onClick = onSettingsClick,
                    interactionSource = fabSource,
                    modifier = Modifier.pressScale(fabSource, 0.92f)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            },
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
            content = {
                items.forEachIndexed { index, item ->
                    val isSelected = if (item.tabIndex >= 0) {
                        topPage == 0 && homePagerState.currentPage == item.tabIndex
                    } else {
                        topPage == item.pageIndex
                    }

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
                            if (item.tabIndex >= 0) {
                                if (homePagerState.currentPage != item.tabIndex) {
                                    scope.launch { homePagerState.animateScrollToPage(item.tabIndex) }
                                }
                            } else if (item.pageIndex != topPage) {
                                onNavigate(item.pageIndex)
                            }
                        },
                        modifier = Modifier
                            .width(48.dp + labelWidth)
                            .height(48.dp),
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
                                modifier = Modifier.size(24.dp)
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
        )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                val activity = LocalContext.current as ComponentActivity
                SideEffect {
                    WindowInsetsControllerCompat(
                        activity.window, activity.window.decorView
                    ).isAppearanceLightStatusBars = !isDark
                    WindowInsetsControllerCompat(
                        activity.window, activity.window.decorView
                    ).isAppearanceLightNavigationBars = !isDark
                }
                MainAppContent(isDark = isDark)
            }
        }
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
    val outerSnackbarHostState = remember { SnackbarHostState() }
    val homePagerState = rememberPagerState(pageCount = { 2 })
    val topPagerState = rememberPagerState(pageCount = { bottomNavItems.size })
    var selectedStopsRoute by remember { mutableStateOf<Route?>(null) }
    var selectedStopsDirection by remember { mutableStateOf<Direction?>(null) }

    fun navigateToArrivals(stop: Stop, routeId: Int) {
        if (routeId > 0) stop.routeNum = routeId
        scope.launch(Dispatchers.IO) {
            DatabaseHelper(context.applicationContext).addRecentStop(stop)
        }
        navController.navigate(
            "arrivals/${stop.locId}?stopName=${URLEncoder.encode(stop.desc, "UTF-8")}&routeId=$routeId&lat=${stop.latitude}&lng=${stop.longitude}"
        )
    }

    fun onTopPageSelected(page: Int) {
        scope.launch {
            if (page == 0 && homePagerState.currentPage != 0) {
                homePagerState.animateScrollToPage(0)
            }
            topPagerState.animateScrollToPage(page)
        }
    }

    // System back walks the top-level pager back to Home before leaving the app
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
                        route == "nearby_stops" || route == "settings" -> TopAppBar(
                        title = {
                            Text(
                                if (route == "nearby_stops") "Nearby Stops" else "Settings"
                            )
                        },
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
                            IconButton(
                                onClick = {
                                    val params = PictureInPictureParams.Builder()
                                        .setAspectRatio(Rational(2, 3))
                                        .build()
                                    pipActivity.enterPictureInPictureMode(params)
                                },
                                interactionSource = pipSource,
                                modifier = Modifier.pressScale(pipSource)
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
                                        val result = toggleFavorite(context, locId, stopName, NavState.arrivalsIsFavorite, routeId, NavState.arrivalsLat, NavState.arrivalsLng)
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
                    visible = isTopLevel,
                    enter = slideInVertically(tween(durationMillis = 250, easing = FastOutSlowInEasing)) { it } +
                        fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(tween(durationMillis = 180, easing = FastOutSlowInEasing)) { it } +
                        fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing))
                ) {
                    MainBottomBar(
                        topPage = topPagerState.currentPage,
                        onNavigate = ::onTopPageSelected,
                        homePagerState = homePagerState,
                        onSettingsClick = {
                            navController.navigate("settings")
                        }
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
            },
            floatingActionButton = {
                AnimatedVisibility(
                    visible = !isTopLevel && currentRoute != "settings",
                    enter = fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(durationMillis = 250, easing = FastOutSlowInEasing)) { it },
                    exit = fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                        slideOutVertically(tween(durationMillis = 180, easing = FastOutSlowInEasing)) { it }
                ) {
                    val fabSource = remember { MutableInteractionSource() }
                    FloatingActionButton(
                        onClick = { navController.navigate("settings") },
                        interactionSource = fabSource,
                        modifier = Modifier.pressScale(fabSource, 0.92f)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
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
                            0 -> HomeScreen(
                                pagerState = homePagerState,
                                onNavigateToArrivals = { stop: Stop ->
                                    navigateToArrivals(stop, stop.routeNum)
                                }
                            )
                            1 -> StopsScreen(
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
                            2 -> WhatsNearbyScreen(
                                onNavigateToArrivals = { stop: Stop, _: Int ->
                                    navigateToArrivals(stop, -1)
                                }
                            )
                            3 -> SearchStopsScreen(
                                onNavigateToArrivals = { stop: Stop, _: Int ->
                                    navigateToArrivals(stop, stop.routeNum)
                                }
                            )
                        }
                    }
                }
                composable("settings") {
                    SettingsScreen()
                }
                composable("nearby_stops") {
                    NearbyStopsScreen(
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
                        stopId = backStackEntry.arguments?.getString("stopId") ?: "",
                        stopName = backStackEntry.arguments?.getString("stopName") ?: "",
                        routeId = backStackEntry.arguments?.getInt("routeId") ?: -1,
                        latitude = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0,
                        longitude = backStackEntry.arguments?.getString("lng")?.toDoubleOrNull() ?: 0.0,
                    )
                }
            }
        }

}

