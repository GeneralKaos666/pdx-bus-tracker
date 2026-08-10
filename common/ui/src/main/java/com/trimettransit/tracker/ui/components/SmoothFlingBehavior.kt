package com.trimettransit.tracker.ui.components

import androidx.compose.animation.core.AnimationState
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
 * uses an exponential decay animation for a smooth roll-off. Mirrors
 * foundation's [androidx.compose.foundation.gestures.DefaultFlingBehavior]
 * contract: the decay is cancelled as soon as the list edge stops consuming
 * the requested delta, and the CANCELED animation's leftover velocity (px/s)
 * is returned so nested-scroll consumers (pull-to-refresh) can take over —
 * not the total scroll distance, which would swallow the fling entirely.
 */
private const val SMOOTH_FLING_VELOCITY_MULTIPLIER = 0.85f

@Composable
fun rememberSmoothFlingBehavior(): FlingBehavior {
    val decayAnimationSpec = remember { exponentialDecay<Float>() }
    return object : FlingBehavior {
        override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
            if (initialVelocity == 0f) return 0f

            val adjustedVelocity = initialVelocity * SMOOTH_FLING_VELOCITY_MULTIPLIER
            // A fresh AnimationState per fling (same as foundation) — the state is a
            // plain data holder; the initial velocity carries the 0.85x scale.
            val animationState = AnimationState(0f, adjustedVelocity)
            var lastValue = 0f
            var remainingVelocity = adjustedVelocity

            animationState.animateDecay(decayAnimationSpec) {
                val delta = value - lastValue
                // The list consumes the delta; at an edge it consumes less than requested.
                val consumed = this@performFling.scrollBy(delta)
                lastValue = value
                // Track the leftover velocity every frame so we can return it if the fling
                // is stopped at an edge. Natural decay completion leaves this ~0.
                remainingVelocity = velocity
                // The list edge stopped the fling: stop animating against it and hand the
                // leftover velocity to nested scroll / pull-to-refresh instead.
                if (Math.abs(delta - consumed) > 0.5f) {
                    cancelAnimation()
                }
            }

            return remainingVelocity
        }
    }
}