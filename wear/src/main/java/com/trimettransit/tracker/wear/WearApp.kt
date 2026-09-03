package com.trimettransit.tracker.wear

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop

private object Routes {
    const val MAIN = "main"
    const val FAVORITES = "favorites"
    const val RECENT = "recent"
    const val ARRIVALS = "arrivals/{locId}?name={name}&route={route}"

    const val ROUTES_LIST = "routes"
    const val ROUTE_DIRS = "routes/{routeId}?name={name}"
    const val ROUTE_STOPS = "stops/{routeId}/{dir}?routeName={routeName}&dirName={dirName}"
    const val ABOUT = "about"

    fun arrivals(stop: Stop) =
        "arrivals/${stop.locId}?name=${Uri.encode(stop.desc)}&route=${stop.routeNum}"

    fun routeDirs(route: Route) =
        "routes/${route.routeId}?name=${Uri.encode(route.desc)}"

    fun routeStops(routeId: Int, direction: Direction) =
        "stops/$routeId/${direction.dir}?routeName=${Uri.encode(routeId.toString())}" +
            "&dirName=${Uri.encode(direction.desc)}"
}

@Composable
fun WearApp(startStop: Stop? = null) {
    WearBusTheme {
        AppScaffold {
            val navController = rememberSwipeDismissableNavController()
            val context = androidx.compose.ui.platform.LocalContext.current
            val transitRepository =
                remember { com.trimettransit.tracker.transit.TransitRepositoryImpl(context.applicationContext) }
            // The stand-alone Tile launches the app with a stop in its extras.
            LaunchedEffect(startStop) {
                startStop?.let { navController.navigate(Routes.arrivals(it)) }
            }
            SwipeDismissableNavHost(navController, startDestination = Routes.MAIN) {
                composable(Routes.MAIN) {
                    HomeScreen(
                        onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                        onOpenRecent = { navController.navigate(Routes.RECENT) },
                        onOpenRoutes = { navController.navigate(Routes.ROUTES_LIST) },
                        onOpenAbout = { navController.navigate(Routes.ABOUT) }
                    )
                }
                composable(Routes.FAVORITES) {
                    StopListScreen(
                        header = "Favorites",
                        read = { it.favorites },
                        emptyText = "No favorites yet.\nTap the heart on any stop.",
                        onStopClick = { stop -> navController.navigate(Routes.arrivals(stop)) }
                    )
                }
                composable(Routes.RECENT) {
                    StopListScreen(
                        header = "Recent Stops",
                        read = { it.recentStops },
                        emptyText = "No recent stops yet.\nStop by to get started.",
                        onStopClick = { stop -> navController.navigate(Routes.arrivals(stop)) }
                    )
                }
                composable(Routes.ROUTES_LIST) {
                    RoutesScreen(
                        transitRepository = transitRepository,
                        level = RoutesLevel.ROUTES,
                        onRouteClick = { route -> navController.navigate(Routes.routeDirs(route)) }
                    )
                }
                composable(
                    route = Routes.ROUTE_DIRS,
                    arguments = listOf(
                        navArgument("routeId") { type = NavType.IntType },
                        navArgument("name") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { entry ->
                    val routeId = entry.arguments?.getInt("routeId") ?: 0
                    RoutesScreen(
                        transitRepository = transitRepository,
                        level = RoutesLevel.DIRECTIONS,
                        routeId = routeId,
                        routeName = entry.arguments?.getString("name") ?: "",
                        onDirectionClick = { direction ->
                            navController.navigate(Routes.routeStops(routeId, direction))
                        }
                    )
                }
                composable(
                    route = Routes.ROUTE_STOPS,
                    arguments = listOf(
                        navArgument("routeId") { type = NavType.IntType },
                        navArgument("dir") { type = NavType.IntType },
                        navArgument("routeName") { type = NavType.StringType; defaultValue = "" },
                        navArgument("dirName") { type = NavType.StringType; defaultValue = "" }
                    )
                ) { entry ->
                    val routeId = entry.arguments?.getInt("routeId") ?: 0
                    val dirId = entry.arguments?.getInt("dir") ?: 0
                    RoutesScreen(
                        transitRepository = transitRepository,
                        level = RoutesLevel.STOPS,
                        routeId = routeId,
                        dirId = dirId,
                        dirName = entry.arguments?.getString("dirName") ?: "",
                        onStopClick = { stop -> navController.navigate(Routes.arrivals(stop)) }
                    )
                }
                composable(Routes.ABOUT) {
                    AboutScreen()
                }
                composable(
                    route = Routes.ARRIVALS,
                    arguments = listOf(
                        navArgument("locId") { type = NavType.IntType },
                        navArgument("name") { type = NavType.StringType; defaultValue = "" },
                        navArgument("route") { type = NavType.IntType; defaultValue = 0 }
                    )
                ) { entry ->
                    val stop = Stop(
                        desc = entry.arguments?.getString("name") ?: "",
                        locId = entry.arguments?.getInt("locId") ?: 0,
                        routeNum = entry.arguments?.getInt("route") ?: 0
                    )
                    ArrivalsScreen(stop)
                }
            }
        }
    }
}
