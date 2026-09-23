package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.FavoriteEdits
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.TransitRepository
import com.trimettransit.tracker.ui.components.navPillBottomPadding
import com.trimettransit.tracker.ui.components.rememberFavoriteIds
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

private const val PREF_WELCOME_SHOWN = "pref_key_favorites_welcome_shown"

@Composable
fun FavoritesScreen(
    favoritesRepository: FavoritesRepository,
    transitRepository: TransitRepository,
    pageVisible: Boolean,
    onNavigateToArrivals: (Stop) -> Unit,
    onBrowseRoutes: () -> Unit,
    onFindNearby: () -> Unit
) {
    val favorites = rememberStopListLoader(read = { favoritesRepository.getFavorites() })
    val favoriteIds = rememberFavoriteIds(favoritesRepository, pageVisible)

    // The pager keeps adjacent pages composed, so returning to this tab fires no resume event.
    // Reload when it becomes visible so a stop saved elsewhere is actually in the list.
    LaunchedEffect(pageVisible) {
        if (pageVisible) favorites.reload()
    }
    var editable by remember(favorites.stops) { mutableStateOf(favorites.stops) }
    var deleteTarget by remember { mutableStateOf<Stop?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removeMutex = remember { Mutex() }
    val removedMessage = stringResource(R.string.favorite_removed)
    val undoLabel = stringResource(R.string.undo)
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var welcomeDismissed by remember { mutableStateOf(prefs.getBoolean(PREF_WELCOME_SHOWN, false)) }
    fun dismissWelcome() {
        welcomeDismissed = true
        prefs.edit { putBoolean(PREF_WELCOME_SHOWN, true) }
    }

    fun persistOrder(stops: List<Stop>) {
        scope.launch {
            runCatching { favoritesRepository.setOrder(stops.map { it.locId }) }
                .onFailure { Timber.e(it, "Failed to persist favorites order") }
        }
    }

    fun handleMove(from: Int, to: Int) {
        if (editable.isEmpty()) return
        val target = FavoriteEdits.reorderTarget(from, to - from, editable.size)
        val reordered = FavoriteEdits.moveStops(editable, from, target)
        if (reordered === editable) return
        editable = reordered
        persistOrder(reordered)
    }

    fun handleRemove(stop: Stop) {
        scope.launch {
            removeMutex.withLock {
                val index = editable.indexOfFirst { it.locId == stop.locId }
                if (index < 0) return@withLock
                editable = editable.filterNot { it.locId == stop.locId }
                val removed = runCatching { favoritesRepository.removeFavorite(stop.locId) }
                    .onFailure { Timber.e(it, "Failed to remove favorite") }
                    .getOrDefault(false)
                if (!removed) {
                    // DB delete failed: roll back instead of offering undo over a lie.
                    editable = editable.toMutableList()
                        .also { it.add(index.coerceIn(0, it.size), stop) }
                    return@withLock
                }
                val result = snackbarHost.showSnackbar(removedMessage, undoLabel)
                if (result == SnackbarResult.ActionPerformed) {
                    val added = runCatching { favoritesRepository.addFavorite(stop) }
                        .onFailure { Timber.e(it, "Failed to restore favorite") }
                        .getOrDefault(false)
                    val current = editable
                    val at = index.coerceIn(0, current.size)
                    if (added) {
                        editable = current.toMutableList()
                            .also { it.add(at, stop) }
                        persistOrder(editable)
                    } else {
                        // Row still exists (duplicate add ignored): just reconcile order.
                        persistOrder(current)
                    }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Names the tab, matching the Recents and Lines headers. The "Favorites" divider below
            // the search field stays: it labels the list, this labels the screen.
            Text(
                text = stringResource(R.string.home_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )
            HomeSearchBar(
                transitRepository = transitRepository,
                favoriteIds = favoriteIds.ids,
                onToggleFavorite = { stop ->
                    scope.launch {
                        favoriteIds.toggle(stop)
                        favorites.reload()
                    }
                },
                onStopSelected = onNavigateToArrivals,
                header = { FavoritesHeader() }
            ) {
                FavoritesStopList(
                    stops = editable,
                    isLoading = favorites.isLoading,
                    isError = favorites.isError,
                    emptyText = stringResource(R.string.no_favorite_stops),
                    onNavigateToArrivals = onNavigateToArrivals,
                    onMove = ::handleMove,
                    onDeleteRequest = { deleteTarget = it },
                    emptyActions = {
                        FavoritesEmptyActions(
                            onBrowseRoutes = onBrowseRoutes,
                            onFindNearby = onFindNearby
                        )
                    },
                    onRetry = favorites.reload
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = navPillBottomPadding() + 8.dp)
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.remove_favorite_title)) },
            text = { Text(target.desc) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    handleRemove(target)
                }) {
                    Text(stringResource(R.string.remove_favorite))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (FavoriteEdits.shouldShowWelcome(
            alreadyShown = welcomeDismissed,
            isEmpty = editable.isEmpty(),
            isLoading = favorites.isLoading
        )
    ) {
        WelcomeDialog(
            onDismiss = { dismissWelcome() },
            onBrowseRoutes = {
                dismissWelcome()
                onBrowseRoutes()
            },
            onFindNearby = {
                dismissWelcome()
                onFindNearby()
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FavoritesEmptyActions(
    onBrowseRoutes: () -> Unit,
    onFindNearby: () -> Unit
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth()
    ) {
        TextButton(onClick = onBrowseRoutes) {
            Text(stringResource(R.string.browse_routes))
        }
        TextButton(onClick = onFindNearby) {
            Text(stringResource(R.string.find_nearby))
        }
    }
}

@Composable
private fun WelcomeDialog(
    onDismiss: () -> Unit,
    onBrowseRoutes: () -> Unit,
    onFindNearby: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.welcome_title)) },
        text = { Text(stringResource(R.string.welcome_message)) },
        confirmButton = {
            TextButton(onClick = onFindNearby) {
                Text(stringResource(R.string.find_nearby))
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onBrowseRoutes) {
                    Text(stringResource(R.string.browse_routes))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.got_it))
                }
            }
        }
    )
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
