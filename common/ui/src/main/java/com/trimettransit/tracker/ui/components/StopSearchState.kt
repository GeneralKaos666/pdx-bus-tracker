package com.trimettransit.tracker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.trimettransit.tracker.model.Stop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * Lazy stop catalogue plus the debounced filter over it, shared by the Home search bar and
 * the trip planner's endpoint picker.
 *
 * The multi-megabyte stop list loads once, on the first non-blank query. Rapid keystrokes
 * share one filter pass ([debounceMillis]); `collectLatest` cancels a superseded filter so
 * only the latest query publishes. [retry] re-runs a failed load.
 */
data class StopSearchResults(
    /** The full catalogue, or null until it has loaded (or when the load failed). */
    val stops: List<Stop>?,
    val isLoading: Boolean,
    val hasError: Boolean,
    val results: List<Stop>,
    val retry: () -> Unit
)

@OptIn(FlowPreview::class)
@Composable
fun rememberStopSearchResults(
    loadStops: suspend () -> List<Stop>?,
    query: String,
    debounceMillis: Long = 200
): StopSearchResults {
    var stops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Stop>>(emptyList()) }
    // Bumped by the error panel's retry action so the fetch effect re-runs.
    var attempt by remember { mutableIntStateOf(0) }

    // Keyed on (stops == null, query.isNotBlank()) so typing never re-launches the network
    // call while the list is still loading.
    LaunchedEffect(stops == null, query.isNotBlank(), attempt) {
        if (stops == null && query.isNotBlank()) {
            isLoading = true
            hasError = false
            val loaded = withContext(Dispatchers.IO) { loadStops() }
            stops = loaded
            isLoading = false
            if (loaded == null) hasError = true
        }
    }

    LaunchedEffect(stops) {
        snapshotFlow { query }
            .debounce(debounceMillis.milliseconds)
            .collectLatest { q ->
                val catalogue = stops
                results = if (q.isBlank() || catalogue == null) {
                    emptyList()
                } else {
                    withContext(Dispatchers.Default) { searchStops(catalogue, q) }
                }
            }
    }

    return StopSearchResults(
        stops = stops,
        isLoading = isLoading,
        hasError = hasError,
        results = results,
        retry = { attempt++ }
    )
}
