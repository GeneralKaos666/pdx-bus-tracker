package com.trimettransit.tracker.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * User-facing card appearance. Set by Settings and provided down the composition tree via
 * [LocalCardStyle]; cards read [cornerRadius], [cutCorners], and [appCardBorder] instead of
 * hardcoding shapes.
 */
data class CardStyle(
    val cornerRadius: Dp = 16.dp,
    val cutCorners: Boolean = false,
    val outlinesEnabled: Boolean = true,
    val outlineColor: Color? = null
)

val LocalCardStyle = staticCompositionLocalOf { CardStyle() }

/**
 * The single source of truth for card/panel corner geometry. Cards, pills, and badges all call
 * this instead of shaping themselves, so the user's "Corner style" preset (rounded vs. cut) and
 * corner radius apply consistently app-wide.
 */
@Composable
fun appCardShape(): Shape {
    val style = LocalCardStyle.current
    return if (style.cutCorners) CutCornerShape(style.cornerRadius) else RoundedCornerShape(style.cornerRadius)
}

/**
 * The outline stroke for cards, or `null` when card outlines are switched off.
 * A `null` [CardStyle.outlineColor] resolves to the theme's outlineVariant so it tracks
 * dark/light/dynamic palettes automatically.
 */
@Composable
fun appCardBorder(): BorderStroke? {
    val style = LocalCardStyle.current
    if (!style.outlinesEnabled) return null
    val color = style.outlineColor ?: MaterialTheme.colorScheme.outlineVariant
    return BorderStroke(1.dp, color)
}