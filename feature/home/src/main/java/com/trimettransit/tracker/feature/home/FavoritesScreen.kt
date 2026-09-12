package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.TransitRepository

private enum class HomeTab { FAVORITES, ALERTS }

@Composable
fun FavoritesScreen(
    favoritesRepository: FavoritesRepository,
    transitRepository: TransitRepository,
    onNavigateToArrivals: (Stop) -> Unit
) {
    val favorites = rememberStopListLoader(read = { favoritesRepository.getFavorites() })
    var homeTab by rememberSaveable { mutableStateOf(HomeTab.FAVORITES) }

    Column(modifier = Modifier.fillMaxSize()) {
        HomeSearchBar(
            transitRepository = transitRepository,
            onStopSelected = onNavigateToArrivals,
            header = {
                HomeTabRow(
                    selected = homeTab,
                    onSelect = { homeTab = it }
                )
                if (homeTab == HomeTab.FAVORITES) {
                    FavoritesHeader()
                }
            }
        ) {
            when (homeTab) {
                HomeTab.FAVORITES -> FavoritesStopList(
                    stops = favorites.stops,
                    isLoading = favorites.isLoading,
                    isError = favorites.isError,
                    emptyText = stringResource(R.string.no_favorite_stops),
                    onNavigateToArrivals = onNavigateToArrivals
                )
                HomeTab.ALERTS -> ServiceAlertsList(transitRepository = transitRepository)
            }
        }
    }
}

@Composable
private fun HomeTabRow(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit
) {
    PrimaryTabRow(
        selectedTabIndex = if (selected == HomeTab.ALERTS) 1 else 0,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Tab(
            selected = selected == HomeTab.FAVORITES,
            onClick = { onSelect(HomeTab.FAVORITES) },
            text = { Text(stringResource(R.string.favorites_title)) }
        )
        Tab(
            selected = selected == HomeTab.ALERTS,
            onClick = { onSelect(HomeTab.ALERTS) },
            text = { Text(stringResource(R.string.service_alerts)) }
        )
    }
}

@Composable
private fun FavoritesHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.favorites_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        HorizontalDivider(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
