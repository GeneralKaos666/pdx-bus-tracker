package com.trimettransit.tracker.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.ui.screens.DrawerActions
import com.trimettransit.tracker.ui.screens.DrawerContent
import com.trimettransit.tracker.ui.screens.arrivals.ArrivalsScreen
import com.trimettransit.tracker.ui.screens.home.HomeScreen
import com.trimettransit.tracker.ui.screens.qr.QRLoadingScreen
import com.trimettransit.tracker.ui.screens.search.SearchStopsScreen
import com.trimettransit.tracker.ui.screens.settings.SettingsScreen
import com.trimettransit.tracker.ui.screens.stops.NearbyStopsScreen
import com.trimettransit.tracker.ui.screens.stops.StopsScreen
import com.trimettransit.tracker.ui.screens.vehicles.VehiclePositionsScreen
import com.trimettransit.tracker.ui.theme.TriMetGoTheme
import java.net.URLEncoder

private val AnimatedContentTransitionScope<*>.navEnter: EnterTransition
    get() = slideInHorizontally(initialOffsetX = { it }) + fadeIn(initialAlpha = 0.3f)

private val AnimatedContentTransitionScope<*>.navExit: ExitTransition
    get() = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(targetAlpha = 0.3f)

private val AnimatedContentTransitionScope<*>.navPopEnter: EnterTransition
    get() = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(initialAlpha = 0.3f)

private val AnimatedContentTransitionScope<*>.navPopExit: ExitTransition
    get() = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(targetAlpha = 0.3f)

class MainActivity : ComponentActivity() {
    private var pendingQrUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
        handleIncomingIntent(intent)
        setContent {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val themePref = prefs.getString("theme", "system") ?: "system"
            val isDark = when (themePref) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            TriMetGoTheme(darkTheme = isDark) {
                val activity = LocalContext.current as ComponentActivity
                SideEffect {
                    WindowInsetsControllerCompat(
                        activity.window, activity.window.decorView
                    ).isAppearanceLightStatusBars = !isDark
                    WindowInsetsControllerCompat(
                        activity.window, activity.window.decorView
                    ).isAppearanceLightNavigationBars = !isDark
                }
                MainAppContent(incomingQrUri = pendingQrUri)
            }
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        handleIncomingIntent(newIntent)
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
private fun MainAppContent(incomingQrUri: String? = null) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showFabMenu by remember { mutableStateOf(false) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: ""
    val isRootScreen = currentRoute in setOf("home", "stops", "settings", "search", "nearby_stops", "vehicle_positions")
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var homeRefreshKey by remember { mutableStateOf(0) }

    fun navigateToArrivals(stop: Stop, routeId: Int) {
        scope.launch(Dispatchers.IO) {
            DatabaseHelper(context.applicationContext).addRecentStop(stop)
        }
        homeRefreshKey++
        navController.navigate(
            "arrivals/${stop.locId}?stopName=${URLEncoder.encode(stop.desc ?: "", "UTF-8")}&routeId=$routeId"
        )
    }

    LaunchedEffect(incomingQrUri) {
        if (incomingQrUri != null) {
            navController.navigate("qr_loading/${URLEncoder.encode(incomingQrUri, "UTF-8")}")
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(actions = DrawerActions(
                selectedItem = currentRoute,
                onHomeClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onRoutesClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("stops") {
                        popUpTo("home")
                    }
                },
                onVehiclesClick = {
                    scope.launch { drawerState.close() }
                    navController.navigate("vehicle_positions") {
                        popUpTo("home")
                    }
                },
                onSettings = {
                    scope.launch { drawerState.close() }
                    navController.navigate("settings")
                },
            ))
        }
    ) {
        Scaffold(
            topBar = {
                if (isRootScreen) {
                    TopAppBar(
                        title = {
                            Text(
                                when (currentRoute) {
                                    "home" -> "TriMet Go"
                                    "stops" -> "Routes"
                                    "settings" -> "Settings"
                                    "search" -> "Search Stops"
                                    "nearby_stops" -> "Nearby Stops"
                                    "vehicle_positions" -> "Vehicles"
                                    else -> "TriMet Go"
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Open navigation drawer")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Open menu")
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable(
                    "home",
                    enterTransition = { navEnter },
                    exitTransition = { navExit },
                    popEnterTransition = { navPopEnter },
                    popExitTransition = { navPopExit }
                ) {
                    HomeScreen(
                        refreshKey = homeRefreshKey,
                        onNavigateToArrivals = { stop: Stop ->
                            navigateToArrivals(stop, -1)
                        }
                    )
                }
                composable(
                    "stops",
                    enterTransition = { navEnter },
                    exitTransition = { navExit },
                    popEnterTransition = { navPopEnter },
                    popExitTransition = { navPopExit }
                ) {
                    StopsScreen(
                        onNavigateToArrivals = { stop: Stop, routeId: Int ->
                            navigateToArrivals(stop, routeId)
                        }
                    )
                }
                composable(
                    "settings",
                    enterTransition = { navEnter },
                    exitTransition = { navExit },
                    popEnterTransition = { navPopEnter },
                    popExitTransition = { navPopExit }
                ) {
                    SettingsScreen()
                }
                composable(
                    "search",
                    enterTransition = { navEnter },
                    exitTransition = { navExit },
                    popEnterTransition = { navPopEnter },
                    popExitTransition = { navPopExit }
                ) {
                    SearchStopsScreen(
                        onNavigateToArrivals = { stop: Stop, _: Int ->
                            navigateToArrivals(stop, -1)
                        }
                    )
                }
                composable(
                    "nearby_stops",
                    enterTransition = { navEnter },
                    exitTransition = { navExit },
                    popEnterTransition = { navPopEnter },
                    popExitTransition = { navPopExit }
                ) {
                    NearbyStopsScreen(
                        onNavigateToArrivals = { stop: Stop, routeId: Int ->
                            navigateToArrivals(stop, routeId)
                        }
                    )
                }
                composable(
                    "vehicle_positions",
                    enterTransition = { navEnter },
                    exitTransition = { navExit },
                    popEnterTransition = { navPopEnter },
                    popExitTransition = { navPopExit }
                ) {
                    VehiclePositionsScreen(
                        onNavigateToArrivals = { stop: Stop, routeId: Int ->
                            navigateToArrivals(stop, routeId)
                        }
                    )
                }
                composable(
                    route = "arrivals/{stopId}?stopName={stopName}&routeId={routeId}",
                    arguments = listOf(
                        navArgument("stopId") { type = NavType.StringType },
                        navArgument("stopName") { type = NavType.StringType; defaultValue = "" },
                        navArgument("routeId") { type = NavType.IntType; defaultValue = -1 }
                    ),
                    enterTransition = { navEnter },
                    exitTransition = { navExit },
                    popEnterTransition = { navPopEnter },
                    popExitTransition = { navPopExit }
                ) { backStackEntry ->
                    ArrivalsScreen(
                        stopId = backStackEntry.arguments?.getString("stopId") ?: "",
                        stopName = backStackEntry.arguments?.getString("stopName") ?: "",
                        routeId = backStackEntry.arguments?.getInt("routeId") ?: -1,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "qr_loading/{qrUri}",
                    arguments = listOf(
                        navArgument("qrUri") { type = NavType.StringType }
                    ),
                    enterTransition = { navEnter },
                    exitTransition = { navExit },
                    popEnterTransition = { navPopEnter },
                    popExitTransition = { navPopExit }
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

    // ModalBottomSheet for FAB menu
    if (showFabMenu) {
        ModalBottomSheet(
            onDismissRequest = { showFabMenu = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                // Sheet title
                Text(
                    text = "Quick Actions",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )

                HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))

                // Search stops
                FABMenuItem(
                    icon = { Icon(Icons.Default.Search, contentDescription = null) },
                    label = "Search stops",
                    onClick = {
                        showFabMenu = false
                        navController.navigate("search")
                    }
                )

                // Scan QR
                FABMenuItem(
                    icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                    label = "Scan QR code",
                    onClick = {
                        showFabMenu = false
                        context.startActivity(Intent(context, QRCameraActivity::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun FABMenuItem(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit
) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.padding(end = 16.dp)) {
            icon()
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
