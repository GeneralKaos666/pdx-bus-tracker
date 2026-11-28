package com.trimettransit.tracker.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.appCardShape

/**
 * One shimmer clock for a whole skeleton list.
 *
 * Returns a lambda rather than a `State<Float>` on purpose: callers read it inside a
 * draw lambda, so the animation invalidates draw, never composition. A skeleton of 18
 * boxes therefore costs one animation and zero recompositions per frame.
 */
@Composable
fun rememberShimmerProgress(): () -> Float {
    val reduceMotion = AppMotion.reduceMotion
    // Return before rememberInfiniteTransition: keeping one alive under reduced motion
    // still drives a per-frame choreographer loop for no visible effect.
    if (reduceMotion) return { 0f }
    val transition = rememberInfiniteTransition(label = "shimmer")
    val progress = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerProgress"
    )
    return { progress.value }
}

/**
 * Rounded shimmer placeholder block — a surface-tinted fill with a soft highlight band
 * sweeping across it.
 *
 * Pass the [progress] from [rememberShimmerProgress] when several boxes share a skeleton;
 * omitting it gives this box its own clock (a single standalone placeholder).
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape? = null,
    progress: (() -> Float)? = null
) {
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val highlight = MaterialTheme.colorScheme.surfaceContainerHighest
    val resolvedShape = shape ?: appCardShape()
    val reduceMotion = AppMotion.reduceMotion
    // Static skeleton under reduced motion: no highlight band is drawn.
    if (reduceMotion) {
        Box(
            modifier = modifier
                .clip(resolvedShape)
                .background(base)
        )
        return
    }
    val clock = progress ?: rememberShimmerProgress()

    Box(
        modifier = modifier
            .clip(resolvedShape)
            .background(base)
            .drawWithContent {
                drawContent()
                val band = size.width * 0.45f
                if (band <= 0f) return@drawWithContent
                // Sweeps from off the left edge to off the right edge.
                val x = -band + (size.width + band) * clock()
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            highlight.copy(alpha = 0.85f),
                            Color.Transparent
                        ),
                        startX = x,
                        endX = x + band
                    )
                )
            }
    )
}

/** Full-screen skeleton of stop-row-shaped placeholders, fades in like the state branches. */
@Composable
fun ListLoadingSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 6,
    contentPadding: PaddingValues = PaddingValues(vertical = 8.dp)
) {
    val progress = rememberShimmerProgress()
    FadeInOnce(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(rows) {
                ShimmerRow(progress)
            }
        }
    }
}

/** Compact shimmer placeholder for inline (accordion) loading. */
@Composable
fun InlineSkeleton(
    modifier: Modifier = Modifier,
    rows: Int = 2
) {
    val progress = rememberShimmerProgress()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(rows) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp), progress = progress)
        }
    }
}

@Composable
private fun ShimmerRow(progress: () -> Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShimmerBox(modifier = Modifier.size(40.dp), shape = appCardShape(), progress = progress)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerBox(modifier = Modifier.fillMaxWidth().height(16.dp), progress = progress)
            Spacer(modifier = Modifier.height(8.dp))
            ShimmerBox(modifier = Modifier.fillMaxWidth(0.6f).height(12.dp), progress = progress)
        }
    }
}
