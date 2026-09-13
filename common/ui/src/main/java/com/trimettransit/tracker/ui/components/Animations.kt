package com.trimettransit.tracker.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.theme.AppMotion
import com.trimettransit.tracker.ui.theme.m3EffectsDefault
import com.trimettransit.tracker.ui.theme.m3EffectsFast
import com.trimettransit.tracker.ui.theme.m3SpatialDefault
import com.trimettransit.tracker.ui.theme.m3SpatialFast
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        enter = if (AppMotion.reduceMotion) {
            fadeIn(m3EffectsDefault())
        } else {
            fadeIn(m3EffectsDefault()) +
                slideInVertically(m3SpatialDefault()) { it / 24 }
        },
        exit = ExitTransition.None
    ) { content() }
}

/**
 * Fades content in once per composition. Uses MutableTransitionState (like [ContentEntrance])
 * because AnimatedVisibility(visible = true) does NOT animate on first composition.
 * For centered state branches (loading/error/empty) where a slide would look wrong.
 */
@Composable
fun FadeInOnce(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val visibleState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) { visibleState.targetState = true }
    AnimatedVisibility(
        visibleState = visibleState,
        modifier = modifier,
        enter = fadeIn(m3EffectsDefault()),
        exit = fadeOut(m3EffectsFast())
    ) { content() }
}

/**
 * Fades + slides a list item in on first appearance with a per-index delay so items
 * cascade from top to bottom. Uses [Animatable]s (like [ContentEntrance]'s
 * MutableTransitionState trick) so the animation plays on first composition — unlike
 * `AnimatedVisibility(visible = true)`, which appears instantly. Delay caps at
 * [maxDelay] so far-off items never wait for the whole list.
 *
 * [enabled] gates the stagger for the whole screen: pass a state the screen flips off
 * after the first visit so items never re-animate when scrolled back into view, and
 * any in-flight item snaps to its final state. Under system reduced motion the items
 * render immediately (no fade) instead of moving.
 */
@Composable
fun Modifier.staggeredFadeIn(
    index: Int,
    delayPerItem: Int = 60,
    maxDelay: Int = 360,
    slideUp: Dp = 12.dp,
    enabled: Boolean = true
): Modifier {
    val delayMillis = min(index * delayPerItem, maxDelay)
    val slidePx = with(LocalDensity.current) { slideUp.toPx() }
    val reduceMotion = AppMotion.reduceMotion
    val alpha = remember { Animatable(0f) }
    val translateY = remember { Animatable(slidePx) }
    LaunchedEffect(enabled, reduceMotion) {
        if (!enabled || reduceMotion) {
            alpha.snapTo(1f)
            translateY.snapTo(0f)
            return@LaunchedEffect
        }
        delay(delayMillis.toLong())
        launch { alpha.animateTo(1f, m3EffectsDefault()) }
        launch { translateY.animateTo(0f, m3SpatialDefault()) }
    }
    return graphicsLayer {
        this.alpha = alpha.value
        translationY = if (reduceMotion) 0f else translateY.value
    }
}

/**
 * Scales the element down while pressed, keeping the ripple.
 * The caller MUST wire the SAME [interactionSource] into the element's clickable
 * (Card(onClick, interactionSource = ...), Modifier.clickable(interactionSource = ..., indication = LocalIndication.current, ...),
 * IconButton(interactionSource = ...), Button(interactionSource = ...), NavigationDrawerItem(interactionSource = ...)) —
 * a private source would never emit and the scale would never animate.
 * MUST be applied BEFORE the clickable in the modifier chain (graphicsLayer wraps
 * the clickable so the ripple scales too). Uses the M3 Expressive spatial-fast spring so the
 * release pops back through 1.0 with the characteristic slight overshoot.
 */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    scale: Float = 0.96f
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) scale else 1f,
        animationSpec = m3SpatialFast(),
        label = "pressScale"
    )
    return graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
    }
}
