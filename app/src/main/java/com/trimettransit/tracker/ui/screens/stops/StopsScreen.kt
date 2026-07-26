package com.trimettransit.tracker.ui.screens.stops

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.trimettransit.tracker.data.model.Direction
import com.trimettransit.tracker.data.model.Route
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.ui.NavState
import com.trimettransit.tracker.ui.screens.components.AnimatedTabRow

@Composable
fun StopsScreen(
    pagerScrollEnabled: Boolean = true,
    onNavigateToArrivals: (Stop, routeId: Int) -> Unit
) {
    val tabs = listOf("Routes", "Directions", "Stops")
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    var selectedRoute by remember { mutableStateOf<Route?>(null) }
    var selectedDirection by remember { mutableStateOf<Direction?>(null) }
    var initialRouteConsumed by remember { mutableStateOf(false) }

    LaunchedEffect(initialRouteConsumed) {
        if (!initialRouteConsumed) {
            val route = NavState.consumeRouteSelection()
            val direction = NavState.consumeDirectionSelection()
            if (route != null) {
                selectedRoute = route
                if (direction != null) selectedDirection = direction
                initialRouteConsumed = true
            }
        }
    }

    // Auto-advance pager when route/direction is selected
    LaunchedEffect(selectedRoute) {
        if (selectedRoute != null) pagerState.animateScrollToPage(1)
    }
    LaunchedEffect(selectedDirection) {
        if (selectedDirection != null) pagerState.animateScrollToPage(2)
    }

    AnimatedTabRow(
        tabs = tabs,
        pagerState = pagerState,
        userScrollEnabled = pagerScrollEnabled,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> StopsRouteList(
                onRouteSelected = { route ->
                    selectedRoute = route
                    selectedDirection = null
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
                        }
                    )
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
                        onStopSelected = { stop ->
                            NavState.savedRoute = route
                            NavState.savedDirection = direction
                            onNavigateToArrivals(stop, route.routeId)
                        }
                    )
                }
            }
        }
    }
}
