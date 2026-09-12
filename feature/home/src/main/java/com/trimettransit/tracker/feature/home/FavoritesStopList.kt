package com.trimettransit.tracker.feature.home

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.components.EmptyState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.ListLoadingSkeleton
import com.trimettransit.tracker.ui.components.StopListItem
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.rememberDenseGridEnabled
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.ui.components.staggeredFadeIn
import com.trimettransit.tracker.ui.theme.m3EffectsDefault

@Composable
fun FavoritesStopList(
    stops: List<Stop>,
    isLoading: Boolean,
    isError: Boolean,
    emptyText: String,
    onNavigateToArrivals: (Stop) -> Unit
) {
    Crossfade(
        targetState = when {
            isLoading && stops.isEmpty() -> 0
            isError && stops.isEmpty() -> 1
            stops.isEmpty() -> 2
            else -> 3
        },
        animationSpec = m3EffectsDefault(),
        label = "favoritesStopList"
    ) { state ->
        when (state) {
            0 -> ListLoadingSkeleton()
            1 -> ErrorState(message = stringResource(R.string.unable_to_load))
            2 -> EmptyState(message = emptyText)
            else -> FavoritesList(
                stops = stops,
                onNavigateToArrivals = onNavigateToArrivals
            )
        }
    }
}

@Composable
private fun FavoritesList(
    stops: List<Stop>,
    onNavigateToArrivals: (Stop) -> Unit
) {
    val dense = rememberDenseGridEnabled()
    val listState = rememberLazyGridState()
    val smoothFling = rememberSmoothFlingBehavior()
    LazyVerticalGrid(
        columns = GridCells.Fixed(if (dense) 2 else 1),
        state = listState,
        modifier = Modifier.fillMaxSize(),
        flingBehavior = smoothFling,
        contentPadding = PaddingValues(
            top = 8.dp,
            start = 16.dp,
            end = 16.dp,
            bottom = navPillBottomPadding() + 8.dp
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(stops.size, key = { stops[it].locId }, contentType = { "stop" }) { index ->
            val stop = stops[index]
            StopListItem(
                stop = stop,
                onClick = { onNavigateToArrivals(stop) },
                modifier = Modifier.animateItem().staggeredFadeIn(index),
                zoomOnTap = true,
                gridMode = dense
            )
        }
    }
}