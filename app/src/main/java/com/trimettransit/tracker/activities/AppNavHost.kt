package com.trimettransit.tracker.activities

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.R
import com.trimettransit.tracker.feature.arrivals.ArrivalsScreen
import com.trimettransit.tracker.feature.home.FavoritesScreen
import com.trimettransit.tracker.feature.home.RecentStopsScreen
import com.trimettransit.tracker.feature.settings.SettingsScreen
import com.trimettransit.tracker.feature.stops.NearbyStopsScreen
import com.trimettransit.tracker.feature.stops.StopsScreen
import com.trimettransit.tracker.feature.trips.TripPlannerScreen
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.model.Route
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.RecentStopsRepository
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.notifications.DepartureAlertPrefs
import com.trimettransit.tracker.notifications.DepartureAlertsSection
import com.trimettransit.tracker.widget.WidgetScheduler
import com.trimettransit.tracker.widget.settings.WidgetSettingsSection

/**
 * The app's NavHost, extracted from [MainAppContent] so the single activity composition stays
 * readable; all state is hoisted and passed in. The Arrivals destination also answers
 * pdxbus://arrivals/{stopId} deep links ([ARRIVALS_DEEP_LINK_BASE]). Deep-link arrivals views
 * deliberately do NOT record a recent stop (unlike widget/notification taps, which route
 * through navigateToArrivals) — automation shouldn't spam the recents list.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    saveableStateHolder: SaveableStateHolder,
    topPagerState: PagerState,
    favoritesRepository: FavoritesRepository,
    recentStopsRepository: RecentStopsRepository,
    transitRepository: TransitRepository,
    selectedStopsRoute: Route?,
    selectedStopsDirection: Direction?,
    onRouteToggle: (Route) -> Unit,
    onDirectionToggle: (Direction) -> Unit,
    onNavigateToArrivals: (Stop, Int) -> Unit,
    onNavigateToTopPage: (Int) -> Unit,
    expandedPane: Boolean,
    detailStop: ArrivalsDestination?,
    arrivalsDetailPane: @Composable (ArrivalsDestination) -> Unit,
    isDark: Boolean,
    arrivalsStateStopId: Int,
    onResetArrivalsState: (Int) -> Unit,
    onArrivalsState: (stopId: Int, name: String, fav: Boolean, lat: Double, lng: Double) -> Unit,
    onRegisterArrivalsRefresh: ((() -> Unit)?) -> Unit,
    onRegisterScrollToTop: ((() -> Unit)?) -> Unit,
    onRegisterSettingsBackAction: ((() -> Boolean)?) -> Unit
) {
    val context = LocalContext.current
    NavHost(
        navController = navController,
        startDestination = HomeDestination,
        modifier = Modifier
            .padding(contentPadding)
            .consumeWindowInsets(contentPadding),
        enterTransition = { navEnter },
        exitTransition = { navExit },
        popEnterTransition = { navPopEnter },
        popExitTransition = { navPopExit }
    ) {
        composable<HomeDestination> {
            val topLevelPage: @Composable (Int) -> Unit = { page ->
                saveableStateHolder.SaveableStateProvider(page) {
                    when (page) {
                        0 -> FavoritesScreen(
                            favoritesRepository = favoritesRepository,
                            transitRepository = transitRepository,
                            pageVisible = topPagerState.currentPage == page,
                            onNavigateToArrivals = { stop: Stop ->
                                onNavigateToArrivals(stop, stop.routeNum)
                            },
                            onBrowseRoutes = { onNavigateToTopPage(2) },
                            onFindNearby = { navController.navigate(NearbyStopsDestination) { launchSingleTop = true } }
                        )
                        1 -> RecentStopsScreen(
                            recentStopsRepository = recentStopsRepository,
                            favoritesRepository = favoritesRepository,
                            pageVisible = topPagerState.currentPage == page,
                            onNavigateToArrivals = { stop: Stop ->
                                onNavigateToArrivals(stop, stop.routeNum)
                            },
                            onFindNearby = { navController.navigate(NearbyStopsDestination) { launchSingleTop = true } }
                        )
                        2 -> StopsScreen(
                            transitRepository = transitRepository,
                            favoritesRepository = favoritesRepository,
                            pageVisible = topPagerState.currentPage == page,
                            selectedRoute = selectedStopsRoute,
                            selectedDirection = selectedStopsDirection,
                            onRouteToggle = onRouteToggle,
                            onDirectionToggle = onDirectionToggle,
                            onNavigateToArrivals = { stop: Stop, routeId: Int ->
                                onNavigateToArrivals(stop, routeId)
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
            val settingsPrefs = remember {
                PreferenceManager.getDefaultSharedPreferences(context)
            }
            SettingsScreen(
                widgetSection = { WidgetSettingsSection() },
                notificationsSection = { DepartureAlertsSection() },
                notificationsEnabled = DepartureAlertPrefs.isEnabled(context),
                widgetRefreshIntervalMin = settingsPrefs.getInt(
                    WidgetScheduler.KEY_REFRESH_INTERVAL_MIN,
                    30
                ),
                apiKeyConfigured = transitRepository.isConfigured(),
                onRegisterScrollToTop = onRegisterScrollToTop,
                onRegisterBackAction = onRegisterSettingsBackAction
            )
        }
        composable<NearbyStopsDestination> {
            NearbyStopsScreen(
                transitRepository = transitRepository,
                onNavigateToArrivals = { stop: Stop, routeId: Int ->
                    onNavigateToArrivals(stop, routeId)
                },
                onRegisterScrollToTop = onRegisterScrollToTop
            )
        }
        composable<ArrivalsDestination>(
            deepLinks = listOf(navDeepLink<ArrivalsDestination>(basePath = ARRIVALS_DEEP_LINK_BASE)),
            enterTransition = { navEnterArrivals }
        ) { backStackEntry ->
            val dest: ArrivalsDestination = backStackEntry.toRoute()
            LaunchedEffect(dest.stopId) { onResetArrivalsState(dest.stopId) }
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
                    onArrivalsState(dest.stopId, name, fav, lat, lng)
                },
                onRegisterRefresh = {
                    if (arrivalsStateStopId == dest.stopId) onRegisterArrivalsRefresh(it)
                },
                onRegisterScrollToTop = onRegisterScrollToTop
            )
        }
    }
}
