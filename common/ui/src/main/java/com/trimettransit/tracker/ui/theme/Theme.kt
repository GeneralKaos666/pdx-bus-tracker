package com.trimettransit.tracker.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.appearance.AppearanceStyle
import com.trimettransit.tracker.ui.appearance.ColorMode
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.appearance.asAmoled
import com.trimettransit.tracker.ui.appearance.seedColorSchemeFor
import com.trimettransit.tracker.ui.appearance.withVibrancy

val TriMetGoShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

/** Maps the chosen [AppearanceStyle] onto a full M3 [ColorScheme] for the current darkness. */
@Composable
private fun buildColorScheme(appearance: AppearanceStyle, darkTheme: Boolean): ColorScheme {
    val context = LocalContext.current
    return remember(appearance, darkTheme) {
        val base = when {
            appearance.colorMode == ColorMode.SEED -> seedColorSchemeFor(
                seedColor = appearance.seedColor,
                isDark = darkTheme,
                amoledDark = appearance.amoledDark,
                vibrancy = appearance.vibrancy
            )
            darkTheme -> dynamicDarkColorScheme(context)
            else -> dynamicLightColorScheme(context)
        }
        if (appearance.colorMode == ColorMode.SEED) {
            base
        } else {
            val styled = base.withVibrancy(appearance.vibrancy)
            if (appearance.amoledDark && darkTheme) styled.asAmoled() else styled
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TriMetGoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    appearance: AppearanceStyle = AppearanceStyle(),
    cardCornerRadius: Dp = 16.dp,
    cardCutCorners: Boolean = false,
    cardOutlinesEnabled: Boolean = true,
    cardOutlineColor: Color? = null,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = animatedColorScheme(buildColorScheme(appearance, darkTheme)),
        motionScheme = MotionScheme.expressive(),
        typography = TriMetGoTypography,
        shapes = TriMetGoShapes,
        content = {
            CompositionLocalProvider(
                LocalAppearanceStyle provides appearance,
                LocalCardStyle provides CardStyle(
                    cornerRadius = cardCornerRadius,
                    cutCorners = cardCutCorners,
                    outlinesEnabled = cardOutlinesEnabled,
                    outlineColor = cardOutlineColor
                )
            ) {
                content()
            }
        }
    )
}

/**
 * Animates the theme tokens toward [target] so a dark/light/dynamic scheme switch melts between
 * palettes instead of snapping. A whole-content Crossfade would wipe per-screen `remember` state
 * and double-compose the map views, so the color values themselves are animated instead — the
 * existing tree stays composed and just re-colors. First composition jumps straight to [target].
 */
@Composable
private fun animatedColorScheme(target: ColorScheme): ColorScheme {
    val spec = m3EffectsSlow<Color>()
    val background by animateColorAsState(target.background, spec, label = "bg")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "onBg")
    val surface by animateColorAsState(target.surface, spec, label = "sf")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "onSf")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "sfV")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "onSfV")
    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "primaryC")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryC")
    val secondary by animateColorAsState(target.secondary, spec, label = "secondary")
    val onSecondary by animateColorAsState(target.onSecondary, spec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, spec, label = "secondaryC")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, spec, label = "onSecondaryC")
    val tertiary by animateColorAsState(target.tertiary, spec, label = "tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, spec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, spec, label = "tertiaryC")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, spec, label = "onTertiaryC")
    val error by animateColorAsState(target.error, spec, label = "error")
    val onError by animateColorAsState(target.onError, spec, label = "onError")
    val errorContainer by animateColorAsState(target.errorContainer, spec, label = "errorC")
    val onErrorContainer by animateColorAsState(target.onErrorContainer, spec, label = "onErrorC")
    val outline by animateColorAsState(target.outline, spec, label = "outline")
    val outlineVariant by animateColorAsState(target.outlineVariant, spec, label = "outlineV")
    val surfaceContainerLow by animateColorAsState(target.surfaceContainerLow, spec, label = "sfcLow")
    val surfaceContainerLowest by animateColorAsState(target.surfaceContainerLowest, spec, label = "sfcLowest")
    val surfaceContainer by animateColorAsState(target.surfaceContainer, spec, label = "sfc")
    val surfaceContainerHigh by animateColorAsState(target.surfaceContainerHigh, spec, label = "sfcHigh")
    val surfaceContainerHighest by animateColorAsState(target.surfaceContainerHighest, spec, label = "sfcHighest")
    val surfaceBright by animateColorAsState(target.surfaceBright, spec, label = "sfcBright")
    val surfaceDim by animateColorAsState(target.surfaceDim, spec, label = "sfcDim")
    return target.copy(
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        error = error,
        onError = onError,
        errorContainer = errorContainer,
        onErrorContainer = onErrorContainer,
        outline = outline,
        outlineVariant = outlineVariant,
        surfaceContainerLow = surfaceContainerLow,
        surfaceContainerLowest = surfaceContainerLowest,
        surfaceContainer = surfaceContainer,
        surfaceContainerHigh = surfaceContainerHigh,
        surfaceContainerHighest = surfaceContainerHighest,
        surfaceBright = surfaceBright,
        surfaceDim = surfaceDim
    )
}
