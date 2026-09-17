package com.trimettransit.tracker.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.feature.home.R
import com.trimettransit.tracker.model.FavoriteEdits
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.model.repository.TransitRepository
import kotlinx.coroutines.launch
import timber.log.Timber

@Composable
fun FavoritesScreen(
    favoritesRepository: FavoritesRepository,
    transitRepository: TransitRepository,
    onNavigateToArrivals: (Stop) -> Unit
) {
    val favorites = rememberStopListLoader(read = { favoritesRepository.getFavorites() })
    var editable by remember(favorites.stops) { mutableStateOf(favorites.stops) }
    var renameTarget by remember { mutableStateOf<Stop?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val removedMessage = stringResource(R.string.favorite_removed)
    val undoLabel = stringResource(R.string.undo)

    fun persistOrder(stops: List<Stop>) {
        scope.launch {
            runCatching { favoritesRepository.setOrder(stops.map { it.locId }) }
                .onFailure { Timber.e(it, "Failed to persist favorites order") }
        }
    }

    fun handleMove(from: Int, to: Int) {
        val reordered = FavoriteEdits.moveStops(editable, from, to)
        if (reordered === editable) return
        editable = reordered
        persistOrder(reordered)
    }

    fun handleRemove(stop: Stop) {
        val snapshot = editable
        val removedIndex = snapshot.indexOfFirst { it.locId == stop.locId }
        if (removedIndex < 0) return
        editable = snapshot.filterNot { it.locId == stop.locId }
        scope.launch {
            runCatching { favoritesRepository.removeFavorite(stop.locId) }
                .onFailure { Timber.e(it, "Failed to remove favorite") }
            val result = snackbarHost.showSnackbar(removedMessage, undoLabel)
            if (result == SnackbarResult.ActionPerformed) {
                runCatching { favoritesRepository.addFavorite(stop) }
                editable = snapshot
                persistOrder(snapshot)
            }
        }
    }

    fun handleRename(stop: Stop, label: String) {
        val clean = FavoriteEdits.sanitizeLabel(label)
        editable = editable.map { if (it.locId == stop.locId) it.copy(label = clean) else it }
        scope.launch {
            runCatching { favoritesRepository.updateLabel(stop.locId, clean) }
                .onFailure { Timber.e(it, "Failed to rename favorite") }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            HomeSearchBar(
                transitRepository = transitRepository,
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
                    onRemove = ::handleRemove,
                    onRename = { renameTarget = it }
                )
            }
        }
        SnackbarHost(
            hostState = snackbarHost,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    renameTarget?.let { target ->
        RenameFavoriteDialog(
            initial = target.label,
            onDismiss = { renameTarget = null },
            onSave = {
                handleRename(target, it)
                renameTarget = null
            }
        )
    }
}

@Composable
private fun RenameFavoriteDialog(
    initial: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var text by remember(initial) { mutableStateOf(initial) }
    LaunchedEffect(initial) { text = initial }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rename_favorite_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(FavoriteEdits.MAX_LABEL_LENGTH + 20) },
                placeholder = { Text(stringResource(R.string.rename_favorite_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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
