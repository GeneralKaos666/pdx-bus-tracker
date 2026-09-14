package com.trimettransit.tracker

import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Re-runs [block] until it returns non-null or [attempts] tries are exhausted,
 * backing off exponentially (2^(k-1) · [retryDelayMs]/2 between tries). Returns
 * null — logging a warning tagged [label] — when the last attempt also failed.
 * Shared by the widget-refresh and departure-alert workers' per-stop fetches.
 */
suspend fun <T> retryFetch(
    attempts: Int,
    label: String,
    retryDelayMs: Long = 2_000L,
    block: suspend () -> T?
): T? {
    for (attempt in 1..attempts) {
        val result = block()
        if (result != null) return result
        if (attempt == attempts) {
            Timber.w("$label fetch failed after $attempts attempts")
            return null
        }
        delay(retryDelayMs / 2 * (1L shl (attempt - 1)))
    }
    return null
}