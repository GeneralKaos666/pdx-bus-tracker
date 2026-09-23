package com.trimettransit.tracker.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    emptyActions: @Composable (() -> Unit)? = null,
    onRetry: (() -> Unit)? = null,
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
            1 -> ErrorState(message = errorMessage, onRetry = onRetry)
            2 -> if (emptyActions == null) {
                EmptyState(message = emptyMessage)
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EmptyState(message = emptyMessage)
                    }
                    emptyActions()
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
            else -> content()
        }
    }
}