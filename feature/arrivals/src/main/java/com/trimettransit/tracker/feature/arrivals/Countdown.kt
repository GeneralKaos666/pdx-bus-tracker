package com.trimettransit.tracker.feature.arrivals

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

private const val FLIP_DURATION_MS = 380

/**
 * Countdown "N min" / "Due" label rendered as a split-flap clock: when the minute
 * bucket rolls, the old value's halves fold away to reveal the new value waiting
 * underneath, like a mechanical flip board.
 *
 * Honors the system reduce-motion setting by falling back to a plain text swap.
 */
@Composable
internal fun CountdownLabel(
    minutesAway: Long,
    isEstimated: Boolean,
    color: Color,
    style: TextStyle,
    modifier: Modifier = Modifier
) {
    val visibleMinutes = minutesAway.coerceAtLeast(0L)
    val targetText = if (visibleMinutes <= 0) {
        stringResource(R.string.due)
    } else {
        stringResource(R.string.minutes, visibleMinutes)
    }
    val weight = if (isEstimated) FontWeight.Bold else FontWeight.Normal

    var displayedText by remember { mutableStateOf(targetText) }
    var flipProgress by remember { mutableFloatStateOf(1f) }
    val flip = remember { Animatable(1f) }

    LaunchedEffect(targetText) {
        if (targetText != displayedText) {
            flip.snapTo(0f)
            flip.animateTo(1f, animationSpec = tween(FLIP_DURATION_MS)) {
                flipProgress = value
            }
            flipProgress = 1f
            displayedText = targetText
        }
    }

    val context = LocalContext.current
    val reduceMotion = remember(context) { reduceMotionEnabled(context) }

    if (reduceMotion || displayedText == targetText) {
        FlipLabelBox(
            text = targetText,
            style = style,
            color = color,
            weight = weight,
            modifier = modifier
        )
    } else {
        FlipLabelBox(
            text = targetText,
            style = style,
            color = color,
            weight = weight,
            modifier = modifier,
            flippingFrom = displayedText,
            flipProgress = flipProgress
        )
    }
}

private fun reduceMotionEnabled(context: Context): Boolean {
    val resolver = context.contentResolver
    val animatorScale = Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
    val transitionScale = Settings.Global.getFloat(resolver, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f)
    return animatorScale == 0f || transitionScale == 0f
}

/**
 * A label that always renders inside a fixed-size box (the wider of the two values)
 * so the flap halves stay perfectly aligned while the text rolls over.
 */
@Composable
private fun FlipLabelBox(
    text: String,
    style: TextStyle,
    color: Color,
    weight: FontWeight,
    modifier: Modifier = Modifier,
    flippingFrom: String? = null,
    flipProgress: Float = 1f
) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val measuredSize = remember(text, flippingFrom, style, color, weight) {
        val usedStyle = style.copy(color = color, fontWeight = weight)
        fun measure(text: String): IntSize = measurer.measure(
            text = AnnotatedString(text),
            style = usedStyle,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            maxLines = 1
        ).size
        val current = measure(text)
        val previous = flippingFrom?.let { measure(it) } ?: current
        IntSize(
            width = maxOf(current.width, previous.width),
            height = maxOf(current.height, previous.height)
        )
    }
    val modifierWithSize = modifier.size(
        width = with(density) { measuredSize.width.toDp() },
        height = with(density) { measuredSize.height.toDp() }
    )

    Box(modifier = modifierWithSize) {
        // Target halves sit flat and static, revealed by the folding front flaps.
        HalfLabel(text, top = true, style = style, color = color, weight = weight)
        HalfLabel(text, top = false, style = style, color = color, weight = weight)
        val front = flippingFrom ?: text
        HalfFlap(front, top = true, progress = flipProgress, style = style, color = color, weight = weight)
        HalfFlap(front, top = false, progress = flipProgress, style = style, color = color, weight = weight)
    }
}

@Composable
private fun HalfLabel(
    text: String,
    top: Boolean,
    style: TextStyle,
    color: Color,
    weight: FontWeight
) {
    Half(top = top) {
        Text(
            text = text,
            color = color,
            style = style,
            fontWeight = weight,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun HalfFlap(
    text: String,
    top: Boolean,
    progress: Float,
    style: TextStyle,
    color: Color,
    weight: FontWeight
) {
    Half(
        top = top,
        modifier = Modifier.graphicsLayer {
            rotationX = if (top) 90f * progress else -90f * progress
            transformOrigin = if (top) {
                TransformOrigin(0.5f, 1f)
            } else {
                TransformOrigin(0.5f, 0f)
            }
            cameraDistance = 14f * density
        }
    ) {
        Text(
            text = text,
            color = color,
            style = style,
            fontWeight = weight,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** Draws [content] centered, clipping it to the upper or lower half of the box. */
@Composable
private fun Half(
    top: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithContent {
                if (top) {
                    clipRect(bottom = size.height / 2f) { this@drawWithContent.drawContent() }
                } else {
                    clipRect(top = size.height / 2f) { this@drawWithContent.drawContent() }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}