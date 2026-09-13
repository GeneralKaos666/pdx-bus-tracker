package com.trimettransit.tracker.ui.appearance

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.materialkolor.dynamicColorScheme
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** Picks a legible content color (near-black ink or white) for a given [background]. */
fun onColorFor(background: Color): Color =
    if (background.luminance() > 0.55f) Color(0xFF1A1C1E) else Color.White

/**
 * Builds a Material 3 [ColorScheme] from a user seed color, applying AMOLED-black surfaces
 * (when dark) and the chosen [Vibrancy]. Shared by the phone theme and the home-screen widget
 * so both render the same custom palette.
 */
fun seedColorSchemeFor(
    seedColor: Color,
    isDark: Boolean,
    amoledDark: Boolean,
    vibrancy: Vibrancy
): ColorScheme =
    dynamicColorScheme(
        seedColor = seedColor,
        isDark = isDark,
        isAmoled = amoledDark && isDark
    ).withVibrancy(vibrancy)

/** HSL saturation in `0..1` for a [Color] (used by the vibrancy tests/boosts). */
internal fun Color.saturation(): Float {
    val mx = maxOf(red, green, blue)
    val mn = minOf(red, green, blue)
    val l = (mx + mn) / 2f
    if (mx == mn) return 0f
    val denom = 1f - abs(2f * l - 1f)
    if (denom == 0f) return 0f
    return (mx - mn) / denom
}

/**
 * Multiplies a color's HSL saturation by [factor], preserving hue and lightness. Pure Kotlin
 * (no `android.graphics`) so it runs in plain JVM unit tests. Achromatic colors are returned
 * unchanged, and saturation never exceeds 1.
 */
internal fun Color.saturated(factor: Float): Color {
    if (factor <= 1.0f) return this
    val r = red
    val g = green
    val b = blue
    val mx = maxOf(r, g, b)
    val mn = minOf(r, g, b)
    val delta = mx - mn
    if (delta == 0f) return this

    var hue = when (mx) {
        r -> (g - b) / delta % 6f
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    } * 60f
    if (hue < 0f) hue += 360f

    val lightness = (mx + mn) / 2f
    val saturation = if (lightness > 0.5f) delta / (2f - mx - mn) else delta / (mx + mn)
    val newSaturation = (saturation * factor).coerceAtMost(1f)
    if (newSaturation == 0f) return this

    val chroma = (1f - abs(2f * lightness - 1f)) * newSaturation
    val huePrime = hue / 60f
    val x = chroma * (1f - abs(huePrime % 2f - 1f))
    val (r1, g1, b1) = when {
        huePrime < 1f -> Triple(chroma, x, 0f)
        huePrime < 2f -> Triple(x, chroma, 0f)
        huePrime < 3f -> Triple(0f, chroma, x)
        huePrime < 4f -> Triple(0f, x, chroma)
        huePrime < 5f -> Triple(x, 0f, chroma)
        else -> Triple(chroma, 0f, x)
    }
    val match = lightness - chroma / 2f
    return Color(red = r1 + match, green = g1 + match, blue = b1 + match, alpha = alpha)
}

/**
 * Applies the chosen [Vibrancy] boost to a generated scheme. MUTED (factor 1.0) is an identity
 * so the default palette is never double-processed.
 */
fun ColorScheme.withVibrancy(vibrancy: Vibrancy): ColorScheme {
    val factor = vibrancy.factor
    if (factor <= 1f) return this
    return copy(
        primary = primary.saturated(factor),
        primaryContainer = primaryContainer.saturated(factor),
        secondary = secondary.saturated(factor),
        secondaryContainer = secondaryContainer.saturated(factor),
        tertiary = tertiary.saturated(factor),
        tertiaryContainer = tertiaryContainer.saturated(factor),
        error = error.saturated(factor),
        errorContainer = errorContainer.saturated(factor)
    )
}

/**
 * Crushes the surface ramp to pure black for AMOLED displays; used on dark schemes only
 * (the theme decides when to apply it; this function is unconditional).
 */
fun ColorScheme.asAmoled(): ColorScheme = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceDim = Color.Black,
    surfaceContainerLowest = Color.Black
)