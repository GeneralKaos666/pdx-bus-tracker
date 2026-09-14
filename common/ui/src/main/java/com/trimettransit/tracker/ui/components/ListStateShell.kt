package com.trimettransit.tracker.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.trimettransit.tracker.ui.theme.m3EffectsDefault

/**
 * Crossfades a list between its four canonical states: loading (only while the list is
 * [isEmpty]), load error (only while empty), empty, and the given [content]. When stale
 * content is already on screen (loading or error with a non-empty list) the content keeps
 * showing — shared by the home stop lists and the arrivals screen.
 */
@Composable
fun ListStateShell(
    isLoading: Boolean,
    isError: Boolean,
    isEmpty: Boolean,
    emptyMessage: String,
    errorMessage: String,
    label: String,
    modifier: Modifier = Modifier,
    loadingContent: @Composable () -> Unit = { ListLoadingSkeleton() },
    content: @Composable () -> Unit
) {
    Crossfade(
        targetState = when {
            isLoading && isEmpty -> 0
            isError && isEmpty -> 1
            isEmpty -> 2
            else -> 3
        },
        animationSpec = m3EffectsDefault(),
        modifier = modifier,
        label = label
    ) { state ->
        when (state) {
            0 -> loadingContent()
            1 -> ErrorState(message = errorMessage)
            2 -> EmptyState(message = emptyMessage)
            else -> content()
        }
    }
}