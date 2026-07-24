package com.trimettransit.tracker.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.ui.screens.components.AnimatedTabRow

@Composable
fun HomeScreen(
    onNavigateToArrivals: (Stop) -> Unit
) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var recentStops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoadingFavorites by remember { mutableStateOf(true) }
    var isLoadingRecent by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = DatabaseHelper(context)
        favorites = db.favorites
        isLoadingFavorites = false
    }

    LaunchedEffect(Unit) {
        val db = DatabaseHelper(context)
        recentStops = db.recentStops
        isLoadingRecent = false
    }

    val tabs = listOf("Favorites", "Recent")
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    AnimatedTabRow(
        tabs = tabs,
        pagerState = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> HomeStopListScreen(
                stops = favorites,
                isLoading = isLoadingFavorites,
                emptyText = "No favorite stops yet.\nTap the heart on arrivals to add one.",
                onNavigateToArrivals = onNavigateToArrivals
            )
            1 -> HomeStopListScreen(
                stops = recentStops,
                isLoading = isLoadingRecent,
                emptyText = "No recent stops.",
                onNavigateToArrivals = onNavigateToArrivals
            )
        }
    }
}
