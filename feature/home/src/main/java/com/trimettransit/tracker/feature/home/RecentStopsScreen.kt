package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.RecentStopsRepository
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.rememberFavoriteIds
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun RecentStopsScreen(
    recentStopsRepository: RecentStopsRepository,
    favoritesRepository: FavoritesRepository,
    pageVisible: Boolean,
    onNavigateToArrivals: (Stop) -> Unit,
    onFindNearby: () -> Unit
) {
    val recent = rememberStopListLoader(read = { recentStopsRepository.getRecentStops() })
    val favoriteIds = rememberFavoriteIds(favoritesRepository, pageVisible)
    var editable by remember(recent.stops) { mutableStateOf(recent.stops) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var dismissTarget by remember { mutableStateOf<Stop?>(null) }
    val scope = rememberCoroutineScope()
    fun handleDismiss(stop: Stop) {
        val index = editable.indexOfFirst { it.locId == stop.locId }
        if (index < 0) return
        editable = editable.filterNot { it.locId == stop.locId }
        scope.launch {
            val removed = runCatching { recentStopsRepository.removeRecent(stop.locId) }
                .onFailure { Timber.e(it, "Failed to remove recent stop") }
                .getOrDefault(false)
            if (!removed) {
                // DB delete failed: roll back instead of diverging from the DB.
                editable = editable.toMutableList()
                    .also { it.add(index.coerceIn(0, it.size), stop) }
            }
        }
    }

    fun handleClearAll() {
        val snapshot = editable
        editable = emptyList()
        scope.launch {
            val cleared = runCatching {
                recentStopsRepository.clearRecents()
                true
            }
                .onFailure { Timber.e(it, "Failed to clear recent stops") }
                .getOrDefault(false)
            if (!cleared) {
                // DB clear failed: roll back instead of diverging from the DB.
                editable = snapshot
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            ContentEntrance {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.recent_stops_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (editable.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text(stringResource(R.string.clear_recents))
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                RecentStopsStopList(
                    stops = editable,
                    isLoading = recent.isLoading,
                    isError = recent.isError,
                    emptyText = stringResource(R.string.no_recent_stops),
                    onNavigateToArrivals = onNavigateToArrivals,
                    favoriteIds = favoriteIds.ids,
                    onToggleFavorite = { stop ->
                        scope.launch { favoriteIds.toggle(stop) }
                    },
                    onDismiss = { dismissTarget = it },
                    emptyActions = {
                        TextButton(onClick = onFindNearby) {
                            Text(stringResource(R.string.find_nearby))
                        }
                    },
                    onRetry = recent.reload
                )
            }
        }
    }

    dismissTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { dismissTarget = null },
            title = { Text(stringResource(R.string.remove_recent_title)) },
            text = { Text(target.desc) },
            confirmButton = {
                TextButton(onClick = {
                    dismissTarget = null
                    handleDismiss(target)
                }) {
                    Text(stringResource(R.string.remove_recent))
                }
            },
            dismissButton = {
                TextButton(onClick = { dismissTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.clear_recents_title)) },
            text = { Text(stringResource(R.string.clear_recents_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    handleClearAll()
                }) {
                    Text(stringResource(R.string.clear_recents))
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
