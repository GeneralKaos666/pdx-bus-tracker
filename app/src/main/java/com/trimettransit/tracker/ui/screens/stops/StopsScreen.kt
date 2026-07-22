package com.trimettransit.tracker.ui.screens.stops

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.trimettransit.tracker.data.model.Direction
import com.trimettransit.tracker.data.model.Route
import com.trimettransit.tracker.data.model.Stop

import com.trimettransit.tracker.ui.NavState
import kotlinx.coroutines.launch

@Composable
fun StopsScreen(
    onNavigateToArrivals: (Stop, routeId: Int) -> Unit
) {
    val tabs = listOf("Routes", "Directions", "Stops")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    var selectedRoute by remember { mutableStateOf<Route?>(null) }
    var selectedDirection by remember { mutableStateOf<Direction?>(null) }
    var initialRouteConsumed by remember { mutableStateOf(false) }

    LaunchedEffect(initialRouteConsumed) {
        if (!initialRouteConsumed) {
            val preselected = NavState.consumeRouteSelection()
            if (preselected != null) {
                selectedRoute = preselected
                initialRouteConsumed = true
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (pagerState.currentPage == index) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> StopsRouteList(
                    onRouteSelected = { route ->
                        selectedRoute = route
                        selectedDirection = null
                        coroutineScope.launch { pagerState.animateScrollToPage(1) }
                    }
                )
                1 -> {
                    val route = selectedRoute
                    if (route != null) {
                        StopsDirectionList(
                            routeId = route.routeId,
                            routeDesc = route.desc ?: "",
                            onDirectionSelected = { direction ->
                                selectedDirection = direction
                                coroutineScope.launch { pagerState.animateScrollToPage(2) }
                            }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a route first",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                2 -> {
                    val route = selectedRoute
                    val direction = selectedDirection
                    if (route != null && direction != null) {
                        StopsStopList(
                            routeId = route.routeId,
                            directionId = direction.dir,
                            directionDesc = direction.desc ?: "",
                            onStopSelected = { stop -> onNavigateToArrivals(stop, route.routeId) }
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Select a route and direction first",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
