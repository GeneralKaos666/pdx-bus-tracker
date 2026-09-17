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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun RecentStopsScreen(
    recentStopsRepository: RecentStopsRepository,
    favoritesRepository: FavoritesRepository,
    onNavigateToArrivals: (Stop) -> Unit,
    onFindNearby: () -> Unit
) {
    val recent = rememberStopListLoader(read = { recentStopsRepository.getRecentStops() })
    var editable by remember(recent.stops) { mutableStateOf(recent.stops) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMessage = stringResource(R.string.added_to_favorites)

    fun handlePromote(stop: Stop) {
        scope.launch {
            val ok = runCatching { favoritesRepository.addFavorite(stop) }
                .onFailure { Timber.e(it, "Failed to promote recent to favorite") }
                .getOrDefault(false)
            if (ok) snackbarHost.showSnackbar(addedMessage)
        }
    }

    fun handleDismiss(stop: Stop) {
        editable = editable.filterNot { it.locId == stop.locId }
        scope.launch {
            runCatching { recentStopsRepository.removeRecent(stop.locId) }
                .onFailure { Timber.e(it, "Failed to remove recent stop") }
        }
    }

    fun handleClearAll() {
        editable = emptyList()
        scope.launch {
            runCatching { recentStopsRepository.clearRecents() }
                .onFailure { Timber.e(it, "Failed to clear recent stops") }
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
                    onPromote = ::handlePromote,
                    onDismiss = ::handleDismiss,
                    emptyActions = {
                        TextButton(onClick = onFindNearby) {
                            Text(stringResource(R.string.find_nearby))
                        }
                    }
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
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
