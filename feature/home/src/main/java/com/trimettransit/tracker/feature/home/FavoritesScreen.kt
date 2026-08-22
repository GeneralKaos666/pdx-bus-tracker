package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trimettransit.tracker.model.Stop

@Composable
fun FavoritesScreen(
    onNavigateToArrivals: (Stop) -> Unit
) {
    val favorites = rememberStopListLoader(read = { it.favorites })

    Column(modifier = Modifier.fillMaxSize()) {
        HomeSearchBar(onStopSelected = onNavigateToArrivals) {
            HomeStopListScreen(
                stops = favorites.stops,
                isLoading = favorites.isLoading,
                isError = favorites.isError,
                emptyText = "No favorite stops yet.\nTap the heart on arrivals to add one.",
                onNavigateToArrivals = onNavigateToArrivals
            )
        }
    }
}
