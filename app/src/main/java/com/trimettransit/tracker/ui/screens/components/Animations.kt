package com.trimettransit.tracker.ui.screens.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
 * which does NOT animate on first composition). Does not replay while the wrapper stays
 * composed (e.g. arrivals pull-to-refresh keeps the branch composed).
 */
@Composable
fun ContentEntrance(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { transitionState.targetState = true }
    AnimatedVisibility(
        visibleState = transitionState,
        modifier = modifier,
        enter = fadeIn(tween(durationMillis = 350, easing = FastOutSlowInEasing)) +
                slideInVertically(tween(durationMillis = 350, easing = FastOutSlowInEasing)) { it / 24 },
        exit = ExitTransition.None
    ) { content() }
}

/**
 * Scales the element down while pressed, keeping the ripple.
 * The caller MUST wire the SAME [interactionSource] into the element's clickable
 * (Card(onClick, interactionSource = ...), Modifier.clickable(interactionSource = ..., indication = LocalIndication.current, ...),
 * IconButton(interactionSource = ...), Button(interactionSource = ...), NavigationDrawerItem(interactionSource = ...)) —
 * a private source would never emit and the scale would never animate.
 * MUST be applied BEFORE the clickable in the modifier chain (graphicsLayer wraps
 * the clickable so the ripple scales too). Press-down ~100ms, release ~150ms,
 * no overshoot (Spring.DampingRatioNoBouncy, Spring.StiffnessMedium).
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    scale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }
}
