package com.trimettransit.tracker.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Runs at most one [block] coroutine at a time, cancelling any in-flight run
 * when a newer one is launched. Mirrors the manual "cancel the old Job, keep
 * the current one for stale-completion guards" pattern screens use, but
 * encapsulated and testable.
 *
 * Typical use: a refresh triggered on screens/resume that must not stack
 * overlapping reads, and where a superseded completion must not clear the
 * newer load's loading state.
 *
 * @param scope the [CoroutineScope] every run is launched into.
 */
class SingleJobRunner(private val scope: CoroutineScope) {

    /**
     * The currently running job, exposed for callers that must distinguish the
     * current load when a coroutine completes (to avoid stale writes).
     */
    val current: MutableStateFlow<Job?> = MutableStateFlow(null)

    fun launch(block: suspend CoroutineScope.() -> Unit): Job {
        current.value?.cancel()
        val job = scope.launch(block = block)
        current.value = job
        return job
    }

    /** True while a run is in flight. */
    val isActive: Boolean
        get() = current.value?.isActive == true

    fun isCurrent(job: Job): Boolean = current.value == job
}
