package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.RecentStopsRepository
import com.trimettransit.tracker.ui.components.ContentEntrance

@Composable
fun RecentStopsScreen(
    recentStopsRepository: RecentStopsRepository,
    onNavigateToArrivals: (Stop) -> Unit
) {
    val recent = rememberStopListLoader(read = { recentStopsRepository.getRecentStops() })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        ContentEntrance {
            Text(
                text = stringResource(R.string.recent_stops_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Box(modifier = Modifier.weight(1f)) {
            HomeStopListScreen(
                stops = recent.stops,
                isLoading = recent.isLoading,
                isError = recent.isError,
                emptyText = stringResource(R.string.no_recent_stops),
                onNavigateToArrivals = onNavigateToArrivals
            )
        }
    }
}
