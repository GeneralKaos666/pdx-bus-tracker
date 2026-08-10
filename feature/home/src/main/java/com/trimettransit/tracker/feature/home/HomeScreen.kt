package com.trimettransit.tracker.feature.home

import android.util.Log
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
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.components.AnimatedTabRow
import com.trimettransit.tracker.ui.components.rememberOnResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "HomeScreen"

@Composable
fun HomeScreen(
    onNavigateToArrivals: (Stop) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var favorites by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var recentStops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoadingFavorites by remember { mutableStateOf(true) }
    var isLoadingRecent by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var hasLoaded by remember { mutableStateOf(false) }

    fun loadFavorites() {
        coroutineScope.launch {
            try {
                val db = DatabaseHelper(context)
                val (favs, recents) = withContext(Dispatchers.IO) { db.favorites to db.recentStops }
                favorites = favs
                recentStops = recents
                isError = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load favorites and recent stops", e)
                isError = true
            } finally {
                isLoadingFavorites = false
                isLoadingRecent = false
            }
        }
    }

    // Auto-refresh both tabs on app re-entry (observer replays ON_RESUME synchronously
    // when already resumed, so this replaces LaunchedEffect for the initial load too)
    rememberOnResume {
        if (hasLoaded) {
            isLoadingFavorites = true
            isLoadingRecent = true
        } else {
            hasLoaded = true
        }
        loadFavorites()
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
                isError = isError,
                emptyText = "No favorite stops yet.\nTap the heart on arrivals to add one.",
                onNavigateToArrivals = onNavigateToArrivals
            )
            1 -> HomeStopListScreen(
                stops = recentStops,
                isLoading = isLoadingRecent,
                isError = isError,
                emptyText = "No recent stops.",
                onNavigateToArrivals = onNavigateToArrivals
            )
        }
    }
}