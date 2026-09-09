package com.trimettransit.tracker.wear

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ListHeaderDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScrollIndicator
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TextButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.SwipeToDismissBox
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import com.trimettransit.tracker.R
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.FavoritesRepositoryImpl
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.wear.tile.TileScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages the watch's favorites: each row opens its arrivals, swiping a row right removes it, and
 * a dedicated row clears everything (with confirmation). The tile is refreshed after every change
 * so the home-screen tile's first favorite stays in sync.
 */
@Composable
fun FavoritesManageScreen(onStopClick: (Stop) -> Unit) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val favoritesRepository = remember {
        FavoritesRepositoryImpl(DatabaseHelper(appContext))
    }
    var favorites by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var showClearConfirm by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refresh() {
        favorites = withContext(Dispatchers.IO) { favoritesRepository.getFavorites() }
    }

    LaunchedEffect(Unit) { refresh() }

    val removeFavorite: (Int) -> Unit = { locId ->
        scope.launch {
            withContext(Dispatchers.IO) { favoritesRepository.removeFavorite(locId) }
            TileScheduler.refreshNow(appContext)
            refresh()
        }
    }

    val clearAll: () -> Unit = {
        scope.launch {
            withContext(Dispatchers.IO) {
                favorites.forEach { favoritesRepository.removeFavorite(it.locId) }
            }
            TileScheduler.refreshNow(appContext)
            refresh()
        }
    }

    val listState = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()

    ScreenScaffold(
        scrollState = listState,
        scrollIndicator = { ScrollIndicator(listState) }
    ) { contentPadding ->
        WearContentEntrance(modifier = Modifier.fillMaxSize()) {
            TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
                item {
                    ListHeader(
                        modifier = Modifier
                            .fillMaxWidth()
                            .transformedHeight(this, transformationSpec)
                            .minimumVerticalContentPadding(ListHeaderDefaults.minimumTopListContentPadding),
                        transformation = SurfaceTransformation(transformationSpec)
                    ) { Text(stringResource(R.string.manage_favorites)) }
                }
                if (favorites.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.favorites_empty),
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .padding(vertical = 12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    items(favorites, key = { it.locId }) { stop ->
                        FavoriteDismissibleRow(
                            stop = stop,
                            onClick = { onStopClick(stop) },
                            onRemove = { removeFavorite(stop.locId) },
                            modifier = Modifier
                                .transformedHeight(this, transformationSpec)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                        )
                    }
                    item {
                        Button(
                            onClick = { showClearConfirm = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .transformedHeight(this, transformationSpec)
                                .minimumVerticalContentPadding(ButtonDefaults.minimumVerticalListContentPadding)
                                .padding(top = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors()
                        ) {
                            Text(stringResource(R.string.clear_all_favorites))
                        }
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            visible = true,
            onDismissRequest = { showClearConfirm = false },
            confirmButton = {
                Button(onClick = {
                    showClearConfirm = false
                    clearAll()
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
            title = { Text(pluralStringResource(R.plurals.confirm_clear_favorites, favorites.size, favorites.size)) }
        )
    }
}

@Composable
private fun FavoriteDismissibleRow(
    stop: Stop,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        when (state.currentValue) {
            SwipeToDismissValue.Dismissed -> onRemove()
            SwipeToDismissValue.Default -> Unit
        }
    }
    SwipeToDismissBox(
        state = state,
        modifier = modifier,
        userSwipeEnabled = true
    ) { isBackground ->
        if (isBackground) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = stringResource(R.string.remove_label),
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            Button(
                onClick = onClick,
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(
                        text = stop.desc,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                },
                secondaryLabel = {
                    Text(
                        text = stop.dirDesc,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Start
                    )
                }
            )
        }
    }
}