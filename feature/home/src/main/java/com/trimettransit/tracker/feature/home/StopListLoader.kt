package com.trimettransit.tracker.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.components.RememberOnResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Load state for one of the home screen's DB-backed stop lists. */
class StopListState(
    val stops: List<Stop>,
    val isLoading: Boolean,
    val isError: Boolean
)

/**
 * Shared loader behind Favorites and Recent Stops: job-deduped DB reads that
 * auto-refresh on app re-entry via RememberOnResume (which replays ON_RESUME
 * synchronously when already resumed, covering the initial composition load).
 */
@Composable
internal fun rememberStopListLoader(
    read: (DatabaseHelper) -> List<Stop>
): StopListState {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var stops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }
    var hasLoaded by remember { mutableStateOf(false) }
    var loadJob by remember { mutableStateOf<Job?>(null) }

    fun load() {
        // Cancel any in-flight read so a slower older one can't overwrite newer data.
        loadJob?.cancel()
        val job = coroutineScope.launch {
            val me = coroutineContext[Job]!!
            try {
                val db = DatabaseHelper(context)
                stops = withContext(Dispatchers.IO) { read(db) }
                isError = false
            } catch (e: Exception) {
                Timber.e(e, "Failed to load stop list")
                isError = true
            } finally {
                // Only the current load may clear the loading state.
                if (loadJob == me) isLoading = false
            }
        }
        loadJob = job
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
