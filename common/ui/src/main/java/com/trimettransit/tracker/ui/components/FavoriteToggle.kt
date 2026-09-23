package com.trimettransit.tracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.ui.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A heart that shows favourite *state* rather than offering an action: filled when the stop is
 * saved, outlined when it is not, and tapping flips it. Used by the lists a user browses stops in;
 * the Favorites tab keeps its own Delete-with-confirm for removals.
 */
@Composable
fun FavoriteToggleButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = stringResource(
                if (isFavorite) R.string.common_remove_from_favorites
                else R.string.common_save_to_favorites
            ),
            tint = if (isFavorite) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

/** Favourited stop ids plus a toggle, shared by every screen that renders [FavoriteToggleButton]. */
class FavoriteIds(
    val ids: Set<Int>,
    /** Flips [stop]'s favourite state and reloads [ids]. Suspends, so callers can order work after it. */
    val toggle: suspend (Stop) -> Unit
)

/**
 * Loads the favourited stop ids and keeps them fresh: once on first composition, again on every app
 * resume, and again after each successful [FavoriteIds.toggle].
 *
 * The toggle asks the repository whether the stop is currently favourited instead of trusting [ids],
 * so a rapid second tap cannot act on a stale copy. If the write fails the set is left alone, which
 * means the heart does not move — the failure is not swallowed into a lie.
 */
@Composable
fun rememberFavoriteIds(favoritesRepository: FavoritesRepository): FavoriteIds {
    var ids by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        ids = withContext(Dispatchers.IO) {
            runCatching { favoritesRepository.favoriteIds() }.getOrDefault(ids)
        }
    }
    RememberOnResume { reloadKey++ }

    return remember(ids, favoritesRepository) {
        FavoriteIds(
            ids = ids,
            toggle = { stop ->
                withContext(Dispatchers.IO) {
                    runCatching {
                        if (favoritesRepository.isFavorite(stop.locId)) {
                            favoritesRepository.removeFavorite(stop.locId)
                        } else {
                            favoritesRepository.addFavorite(stop)
                        }
                    }
                }
                reloadKey++
            }
        )
    }
}
