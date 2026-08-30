package com.trimettransit.tracker.wear

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Fades + slides screen content up the first time it appears.
 * Plays once per composition: MutableTransitionState starts false, target flips
 * to true on first composition (guaranteed to animate — unlike `AnimatedVisibility(visible = true)`,
 * which does NOT animate on first composition).
 */
@Composable
fun WearContentEntrance(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { transitionState.targetState = true }
    AnimatedVisibility(
        visibleState = transitionState,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(durationMillis = 300, easing = FastOutSlowInEasing)) { it / 24 },
        exit = ExitTransition.None
    ) { content() }
}

/**
 * Fades content in once per composition. Uses MutableTransitionState (like [WearContentEntrance])
 * because AnimatedVisibility(visible = true) does NOT animate on first composition.
 * For centered state branches (loading/error/empty) where a slide would look wrong.
 */
@Composable
fun WearFadeInOnce(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 250, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(durationMillis = 250, easing = FastOutSlowInEasing))
    ) { content() }
}

/**
 * Scales a Wear composable (Chip/Button) down while pressed, keeping its ripple.
 * The caller MUST wire the SAME [interactionSource] into the element's clickable
 * (Chip(interactionSource = ...), Button(interactionSource = ...)).
 * MUST be applied BEFORE the clickable in the modifier chain.
 */
@Composable
fun Modifier.wearPressScale(
    interactionSource: MutableInteractionSource,
    scale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "wearPressScale"
    )
    return graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }
}
