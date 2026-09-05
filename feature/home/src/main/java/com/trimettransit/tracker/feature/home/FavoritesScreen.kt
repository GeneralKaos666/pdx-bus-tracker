package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.TransitRepository

@Composable
fun FavoritesScreen(
    favoritesRepository: FavoritesRepository,
    transitRepository: TransitRepository,
    onNavigateToArrivals: (Stop) -> Unit
) {
    val favorites = rememberStopListLoader(read = { favoritesRepository.getFavorites() })

    Column(modifier = Modifier.fillMaxSize()) {
        HomeSearchBar(transitRepository = transitRepository, onStopSelected = onNavigateToArrivals) {
            HomeStopListScreen(
                stops = favorites.stops,
                isLoading = favorites.isLoading,
                isError = favorites.isError,
                emptyText = stringResource(R.string.no_favorite_stops),
                onNavigateToArrivals = onNavigateToArrivals
            )
        }
    }
}
