package com.trimettransit.tracker.ui.screens.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.ui.screens.components.AnimatedTabRow
import com.trimettransit.tracker.ui.screens.components.rememberOnResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Composable
fun HomeScreen(
    refreshKey: Int = 0,
    pagerScrollEnabled: Boolean = true,
    onNavigateToArrivals: (Stop) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var favorites by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var recentStops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoadingFavorites by remember { mutableStateOf(true) }
    var isLoadingRecent by remember { mutableStateOf(true) }

    // Auto-refresh both tabs on app re-entry (observer replays ON_RESUME synchronously
    // when already resumed, so this replaces LaunchedEffect for the initial load too)
    rememberOnResume {
        coroutineScope.launch {
            val db = DatabaseHelper(context)
            favorites = withContext(Dispatchers.IO) { db.favorites }
            isLoadingFavorites = false
            recentStops = withContext(Dispatchers.IO) { db.recentStops }
            isLoadingRecent = false
        }
    }

    val tabs = listOf("Favorites", "Recent")
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    AnimatedTabRow(
        tabs = tabs,
        pagerState = pagerState,
        userScrollEnabled = pagerScrollEnabled,
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
