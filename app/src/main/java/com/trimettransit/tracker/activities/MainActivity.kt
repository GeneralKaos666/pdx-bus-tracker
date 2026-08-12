package com.trimettransit.tracker.activities

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.activities.qr_scanning.QRCameraActivity
import com.trimettransit.tracker.data.local.DatabaseHelper
import kotlinx.coroutines.Dispatchers
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.feature.arrivals.ArrivalsScreen
import com.trimettransit.tracker.feature.home.HomeScreen
import com.trimettransit.tracker.feature.qr.QRLoadingScreen
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

private data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

private val bottomNavItems = listOf(
    BottomNavItem("home", "Home", Icons.Filled.Home),
    BottomNavItem("stops", "Routes", Icons.Filled.Map),
    BottomNavItem("vehicle_positions", "What's Nearby", Icons.Filled.DirectionsBus),
    BottomNavItem("settings", "Settings", Icons.Filled.Settings),
)

@Composable
private fun MainBottomBar(currentRoute: String, onNavigate: (String) -> Unit) {
    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { if (item.route != currentRoute) onNavigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
            )
        }
    }
}

class MainActivity : ComponentActivity() {
    private var pendingQrUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIncomingIntent(intent)
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
                MainAppContent(incomingQrUri = pendingQrUri, isDark = isDark)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(incoming: Intent) {
        val qrUri = incoming.getStringExtra("qr_uri")
        if (qrUri != null) {
            pendingQrUri = qrUri
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppContent(incomingQrUri: String? = null, isDark: Boolean) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val inPip = rememberIsInPipMode()
    val pipActivity = LocalContext.current.findActivity()
    val context = LocalContext.current
    var showFabMenu by remember { mutableStateOf(false) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val refreshRotation = remember { Animatable(0f) }
    val currentRoute = currentBackStackEntry?.destination?.route ?: ""
    val isTopLevel = currentRoute in setOf("home", "stops", "settings", "vehicle_positions")
    val outerSnackbarHostState = remember { SnackbarHostState() }

    fun navigateToArrivals(stop: Stop, routeId: Int) {
        if (routeId > 0) stop.routeNum = routeId
        scope.launch(Dispatchers.IO) {
            DatabaseHelper(context.applicationContext).addRecentStop(stop)
        }
        navController.navigate(
            "arrivals/${stop.locId}?stopName=${URLEncoder.encode(stop.desc ?: "", "UTF-8")}&routeId=$routeId&lat=${stop.latitude}&lng=${stop.longitude}"
        )
    }

    LaunchedEffect(incomingQrUri) {
        if (incomingQrUri != null) {
            navController.navigate("qr_loading/${URLEncoder.encode(incomingQrUri, "UTF-8")}")
        }
    }

    fun onBottomNavSelected(route: String) {
        when (route) {
            "home" -> navController.navigate("home") { popUpTo("home") { inclusive = true } }
            "stops" -> navController.navigate("stops") { popUpTo("home") }
            "vehicle_positions" -> navController.navigate("vehicle_positions") { popUpTo("home") }
            "settings" -> navController.navigate("settings")
        }
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
                        route == "search" || route == "nearby_stops" -> TopAppBar(
                        title = {
                            Text(
                                if (route == "search") "Search Stops" else "Nearby Stops"
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
                                        val msg = toggleFavorite(context, locId, stopName, NavState.arrivalsIsFavorite, routeId, NavState.arrivalsLat, NavState.arrivalsLng)
                                        NavState.arrivalsIsFavorite = !NavState.arrivalsIsFavorite
                                        outerSnackbarHostState.showSnackbar(msg)
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
                        currentRoute = currentRoute,
                        onNavigate = ::onBottomNavSelected
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
                    visible = !currentRoute.startsWith("arrivals/"),
                    enter = fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)) +
                        slideInVertically(tween(durationMillis = 250, easing = FastOutSlowInEasing)) { it },
                    exit = fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                        slideOutVertically(tween(durationMillis = 180, easing = FastOutSlowInEasing)) { it }
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedVisibility(
                            visible = showFabMenu,
                            enter = fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)) + slideInVertically(tween(durationMillis = 250, easing = FastOutSlowInEasing)) { it / 2 },
                            exit = fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing)) + slideOutVertically(tween(durationMillis = 180, easing = FastOutSlowInEasing)) { it / 2 }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shadowElevation = 4.dp,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clearAndSetSemantics { }
                                ) {
                                    Text(
                                        "Scan QR code",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val qrFabSource = remember { MutableInteractionSource() }
                                SmallFloatingActionButton(
                                    onClick = {
                                        showFabMenu = false
                                        context.startActivity(Intent(context, QRCameraActivity::class.java))
                                    },
                                    interactionSource = qrFabSource,
                                    modifier = Modifier.pressScale(qrFabSource, 0.92f)
                                ) {
                                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR code")
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showFabMenu,
                            enter = fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)) + slideInVertically(tween(durationMillis = 250, easing = FastOutSlowInEasing)) { it / 2 },
                            exit = fadeOut(tween(durationMillis = 180, easing = FastOutSlowInEasing)) + slideOutVertically(tween(durationMillis = 180, easing = FastOutSlowInEasing)) { it / 2 }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                            ) {
                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shadowElevation = 4.dp,
                                    tonalElevation = 2.dp,
                                    modifier = Modifier
                                        .padding(end = 12.dp)
                                        .clearAndSetSemantics { }
                                ) {
                                    Text(
                                        "Search stops",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                val searchFabSource = remember { MutableInteractionSource() }
                                SmallFloatingActionButton(
                                    onClick = {
                                        showFabMenu = false
                                        navController.navigate("search")
                                    },
                                    interactionSource = searchFabSource,
                                    modifier = Modifier.pressScale(searchFabSource, 0.92f)
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search stops")
                                }
                            }
                        }

                        val fabRotation by animateFloatAsState(
                            targetValue = if (showFabMenu) 45f else 0f,
                            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                        )
                        val mainFabSource = remember { MutableInteractionSource() }
                        FloatingActionButton(
                            onClick = { showFabMenu = !showFabMenu },
                            interactionSource = mainFabSource,
                            modifier = Modifier.pressScale(mainFabSource, 0.92f)
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = if (showFabMenu) "Close menu" else "Open menu",
                                modifier = Modifier.rotate(fabRotation)
                            )
                        }
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
                    HomeScreen(
                        onNavigateToArrivals = { stop: Stop ->
                            navigateToArrivals(stop, stop.routeNum)
                        }
                    )
                }
                composable("stops", exitTransition = { navExitQuick }) {
                    StopsScreen(
                        onNavigateToArrivals = { stop: Stop, routeId: Int ->
                            navigateToArrivals(stop, routeId)
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen()
                }
                composable("search") {
                    SearchStopsScreen(
                        onNavigateToArrivals = { stop: Stop, _: Int ->
                            navigateToArrivals(stop, stop.routeNum)
                        }
                    )
                }
                composable("nearby_stops") {
                    NearbyStopsScreen(
                        onNavigateToArrivals = { stop: Stop, routeId: Int ->
                            navigateToArrivals(stop, routeId)
                        }
                    )
                }
                composable("vehicle_positions") {
                    WhatsNearbyScreen(
                        onNavigateToArrivals = { stop: Stop, _: Int ->
                            navigateToArrivals(stop, -1)
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
                composable(
                    route = "qr_loading/{qrUri}",
                    arguments = listOf(
                        navArgument("qrUri") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val qrUri = backStackEntry.arguments?.getString("qrUri") ?: return@composable
                    QRLoadingScreen(
                        qrUri = qrUri,
                        onNavigateBack = { navController.popBackStack() },
                        onStopResolved = { stopId: Int ->
                            navController.navigate("arrivals/$stopId?stopName=QR+Stop&routeId=-1") {
                                popUpTo("qr_loading/{qrUri}") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }

}

