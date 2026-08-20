package com.trimettransit.tracker.feature.home

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import com.trimettransit.tracker.ui.components.RememberOnResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "FavoritesScreen"

@Composable
fun FavoritesScreen(
    onNavigateToArrivals: (Stop) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var favorites by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var hasLoaded by remember { mutableStateOf(false) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    fun loadFavorites() {
        // Cancel any in-flight read so a slower older one can't overwrite newer data.
        loadJob?.cancel()
        val job = coroutineScope.launch {
            val me = coroutineContext[Job]!!
            try {
                val db = DatabaseHelper(context)
                favorites = withContext(Dispatchers.IO) { db.favorites }
                isError = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load favorites", e)
                isError = true
            } finally {
                // Only the current load may clear the loading state.
                if (loadJob == me) isLoading = false
            }
        }
        loadJob = job
    }

    // Auto-refresh on app re-entry (observer replays ON_RESUME synchronously
    // when already resumed, so this replaces LaunchedEffect for the initial load too)
    RememberOnResume {
        if (hasLoaded) {
            isLoading = true
        } else {
            hasLoaded = true
        }
        loadFavorites()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeSearchBar(onStopSelected = onNavigateToArrivals) {
            HomeStopListScreen(
                stops = favorites,
                isLoading = isLoading,
                isError = isError,
                emptyText = "No favorite stops yet.\nTap the heart on arrivals to add one.",
                onNavigateToArrivals = onNavigateToArrivals
            )
        }
    }
}