package com.trimettransit.tracker.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.components.RememberOnResume
import com.trimettransit.tracker.util.SingleJobRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Load state for one of the home screen's DB-backed stop lists. */
class StopListState(
    val stops: List<Stop>,
    val isLoading: Boolean,
    val isError: Boolean
)

/**
 * Shared loader behind Favorites and Recent Stops: job-deduped reads that
 * auto-refresh on app re-entry via RememberOnResume (which replays ON_RESUME
 * synchronously when already resumed, covering the initial composition load).
 *
 * [read] is a suspend data-access call (from a repository) returning the stop
 * list, rather than a [DatabaseHelper] lambda, so callers stay decoupled from
 * the local-data implementation.
 */
@Composable
internal fun rememberStopListLoader(
    read: suspend () -> List<Stop>
): StopListState {
    val coroutineScope = rememberCoroutineScope()
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var hasLoaded by remember { mutableStateOf(false) }
    val runner = remember { SingleJobRunner(coroutineScope) }

    fun load() {
        // SingleJobRunner cancels any in-flight read so a slower older one can't
        // overwrite newer data.
        runner.launch {
            try {
                stops = withContext(Dispatchers.IO) { read() }
                isError = false
            } catch (e: Exception) {
                Timber.e(e, "Failed to load stop list")
                isError = true
            } finally {
                // Only the current load may clear the loading state.
                if (runner.isCurrent(coroutineContext[Job]!!)) isLoading = false
            }
        }
    }

    RememberOnResume {
        if (hasLoaded) {
            isLoading = true
        } else {
            hasLoaded = true
        }
        load()
    }

    return StopListState(stops, isLoading, isError)
}
