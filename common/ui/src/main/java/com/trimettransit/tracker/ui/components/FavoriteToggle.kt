package com.trimettransit.tracker.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.model.repository.FavoritesRepository
import com.trimettransit.tracker.ui.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * A heart that shows favourite *state* rather than offering an action: filled when the stop is
 * saved, outlined when it is not, and tapping flips it. Used by every list a user browses stops in,
 * plus the arrivals header; the Favorites tab keeps its own Delete-with-confirm for removals.
 */
@Composable
fun FavoriteToggleButton(
    isFavorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // The press feedback the rest of the app's icon buttons have. In the arrivals bar this heart
    // sits next to press-scaled controls, so without it it was the only one that did not react.
    val interactionSource = remember { MutableInteractionSource() }
    IconButton(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier
            .size(48.dp)
            .pressScale(interactionSource)
    ) {
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
    /**
     * Flips [stop]'s favourite state. Suspends until the write completes; [ids] refreshes
     * asynchronously afterwards, so callers must not treat [ids] as fresh on return.
     */
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
fun rememberFavoriteIds(
    favoritesRepository: FavoritesRepository,
    /**
     * True while this screen's pager page is the current one. The pager keeps adjacent pages
     * composed, so a retained page fires no lifecycle event when you swipe to it — without this
     * it would never re-read and would show stale hearts after a toggle on another tab.
     */
    pageVisible: Boolean = true
): FavoriteIds {
    var ids by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var hasLoaded by remember { mutableStateOf(false) }
    val mutex = remember { Mutex() }

    LaunchedEffect(reloadKey, pageVisible) {
        if (!pageVisible) return@LaunchedEffect
        ids = withContext(Dispatchers.IO) {
            try {
                favoritesRepository.favoriteIds()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Timber.w(e, "Failed to load favorite ids")
                ids
            }
        }
    }
    RememberOnResume {
        // Skip the resume that lands during first composition: the effect above already loaded,
        // and bumping the key here would read the table twice on every entry.
        if (pageVisible) {
            if (hasLoaded) reloadKey++ else hasLoaded = true
        }
    }

    return remember(ids, favoritesRepository) {
        FavoriteIds(
            ids = ids,
            toggle = { stop ->
                // Serialised: two fast taps must not both read the pre-write state, or they would
                // net one change instead of returning the stop to where it started.
                mutex.withLock {
                    withContext(Dispatchers.IO) {
                        try {
                            if (favoritesRepository.isFavorite(stop.locId)) {
                                favoritesRepository.removeFavorite(stop.locId)
                            } else {
                                favoritesRepository.addFavorite(stop)
                            }
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            Timber.w(e, "Failed to toggle favorite")
                        }
                    }
                }
                reloadKey++
            }
        )
    }
}
