package com.trimettransit.tracker.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * User-facing card appearance. Set by Settings and provided down the composition tree via
 * [LocalCardStyle]; cards read [cornerRadius] and [appCardBorder] instead of hardcoding shapes.
 */
data class CardStyle(
    val cornerRadius: Dp = 16.dp,
    val outlinesEnabled: Boolean = true,
    val outlineColor: Color? = null
)

val LocalCardStyle = staticCompositionLocalOf { CardStyle() }

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