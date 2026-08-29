package com.trimettransit.tracker.wear

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.trimettransit.tracker.model.Stop

private object Routes {
    const val MAIN = "main"
    const val FAVORITES = "favorites"
    const val RECENT = "recent"
    const val ARRIVALS = "arrivals/{locId}?name={name}&route={route}"

    fun arrivals(stop: Stop) =
        "arrivals/${stop.locId}?name=${Uri.encode(stop.desc)}&route=${stop.routeNum}"
}

@Composable
fun WearApp() {
    PdxWearTheme {
        val navController = rememberSwipeDismissableNavController()
        SwipeDismissableNavHost(navController, startDestination = Routes.MAIN) {
            composable(Routes.MAIN) {
                HomeScreen(
                    onOpenFavorites = { navController.navigate(Routes.FAVORITES) },
                    onOpenRecent = { navController.navigate(Routes.RECENT) }
                )
            }
            composable(Routes.FAVORITES) {
                StopListScreen(
                    header = "Favorites",
                    read = { it.favorites },
                    emptyText = "No favorites yet.\nTap the heart on your phone.",
                    onStopClick = { stop -> navController.navigate(Routes.arrivals(stop)) }
                )
            }
            composable(Routes.RECENT) {
                StopListScreen(
                    header = "Recent Stops",
                    read = { it.recentStops },
                    emptyText = "No recent stops yet.\nView stops on your phone.",
                    onStopClick = { stop -> navController.navigate(Routes.arrivals(stop)) }
                )
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

@Composable
private fun PdxWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}