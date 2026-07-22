package com.trimettransit.tracker.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.activities.qr_scanning.QRLoadingActivity
import com.trimettransit.tracker.activities.qr_scanning.QRScanningActivity
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

class MainActivity : ComponentActivity() {
    private var pendingQrUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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
                MainAppContent(incomingQrUri = pendingQrUri)
            }
        }
    }

    override fun onNewIntent(newIntent: Intent) {
        super.onNewIntent(newIntent)
        handleIncomingIntent(newIntent)
    }

    private fun handleIncomingIntent(incoming: Intent) {
        val qrUri = incoming.getStringExtra(QRLoadingActivity.EXTRA_QR_URI)
        if (qrUri != null) {
            pendingQrUri = qrUri
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppContent(incomingQrUri: String? = null) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var showFabMenu by remember { mutableStateOf(false) }
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: ""
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun navigateToArrivals(stop: Stop, routeId: Int) {
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
        drawerContent = {
            DrawerContent(actions = DrawerActions(
                selectedItem = currentRoute,
                onHomeClick = {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onRoutesClick = {
                    navController.navigate("stops") {
                        popUpTo("home")
                    }
                },
                onVehiclesClick = {
                    navController.navigate("vehicle_positions") {
                        popUpTo("home")
                    }
                },
                onSettings = {
                    navController.navigate("settings")
                },
            ))
        }
    ) {
        Scaffold(
            floatingActionButton = {
                FloatingActionButton(onClick = { showFabMenu = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Menu")
                }
            }
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier.padding(padding)
            ) {
                composable("home") {
                    HomeScreen(
                        onNavigateToArrivals = { stop: Stop ->
                            navigateToArrivals(stop, -1)
                        }
                    )
                }
                composable("stops") {
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
                            navigateToArrivals(stop, -1)
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
                    )
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

    // ModalBottomSheet for FAB menu
    if (showFabMenu) {
        ModalBottomSheet(
            onDismissRequest = { showFabMenu = false },
            sheetState = sheetState
        ) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFabMenu = false
                            navController.navigate("search")
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Search stops")
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showFabMenu = false
                            context.startActivity(Intent(context, QRScanningActivity::class.java))
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Scan QR")
                }
            }
        }
    }
}
