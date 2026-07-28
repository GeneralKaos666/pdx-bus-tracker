package com.trimettransit.tracker.ui.screens.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * A [FlingBehavior] that applies a gentler deceleration curve, making
 * scroll-fling feel smoother than the default spline-based behavior.
 *
 * Scales the initial velocity by [SMOOTH_FLING_VELOCITY_MULTIPLIER] and
 * uses an exponential decay animation for a smooth roll-off.
 */
private const val SMOOTH_FLING_VELOCITY_MULTIPLIER = 0.85f

@Composable
fun rememberSmoothFlingBehavior(): FlingBehavior {
    val decayAnimationSpec = remember { exponentialDecay<Float>() }
    val animatable = remember { Animatable(0f) }
    return object : FlingBehavior {
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            if (initialVelocity == 0f) return 0f

            val adjustedVelocity = initialVelocity * SMOOTH_FLING_VELOCITY_MULTIPLIER
            var lastValue = 0f
            var consumed = 0f

            animatable.snapTo(0f)
            animatable.animateDecay(
                initialVelocity = adjustedVelocity,
                animationSpec = decayAnimationSpec
            ) {
                val current = this.value
                val delta = current - lastValue
                consumed += this@performFling.scrollBy(delta)
                lastValue = current
            }

            return consumed
        }
    }
}
