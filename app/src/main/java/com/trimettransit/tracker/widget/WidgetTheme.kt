package com.trimettransit.tracker.widget

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.glance.color.ColorProviders
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.material3.ColorProviders as material3ColorProviders
import com.trimettransit.tracker.ui.appearance.AppearanceStyle
import com.trimettransit.tracker.ui.appearance.ColorMode
import com.trimettransit.tracker.ui.appearance.ThemePreference
import com.trimettransit.tracker.ui.appearance.asAmoled
import com.trimettransit.tracker.ui.appearance.seedColorSchemeFor
import com.trimettransit.tracker.ui.appearance.withVibrancy
import com.trimettransit.tracker.ui.theme.DarkBackground
import com.trimettransit.tracker.ui.theme.DarkError
import com.trimettransit.tracker.ui.theme.DarkErrorContainer
import com.trimettransit.tracker.ui.theme.DarkInverseOnSurface
import com.trimettransit.tracker.ui.theme.DarkInversePrimary
import com.trimettransit.tracker.ui.theme.DarkInverseSurface
import com.trimettransit.tracker.ui.theme.DarkOnBackground
import com.trimettransit.tracker.ui.theme.DarkOnError
import com.trimettransit.tracker.ui.theme.DarkOnErrorContainer
import com.trimettransit.tracker.ui.theme.DarkOnPrimary
import com.trimettransit.tracker.ui.theme.DarkOnPrimaryContainer
import com.trimettransit.tracker.ui.theme.DarkOnSecondary
import com.trimettransit.tracker.ui.theme.DarkOnSurface
import com.trimettransit.tracker.ui.theme.DarkOnSurfaceVariant
import com.trimettransit.tracker.ui.theme.DarkOutline
import com.trimettransit.tracker.ui.theme.DarkPrimary
import com.trimettransit.tracker.ui.theme.DarkPrimaryContainer
import com.trimettransit.tracker.ui.theme.DarkSecondary
import com.trimettransit.tracker.ui.theme.DarkSurface
import com.trimettransit.tracker.ui.theme.DarkSurfaceVariant
import com.trimettransit.tracker.ui.theme.LightBackground
import com.trimettransit.tracker.ui.theme.LightError
import com.trimettransit.tracker.ui.theme.LightErrorContainer
import com.trimettransit.tracker.ui.theme.LightInverseOnSurface
import com.trimettransit.tracker.ui.theme.LightInversePrimary
import com.trimettransit.tracker.ui.theme.LightInverseSurface
import com.trimettransit.tracker.ui.theme.LightOnBackground
import com.trimettransit.tracker.ui.theme.LightOnError
import com.trimettransit.tracker.ui.theme.LightOnErrorContainer
import com.trimettransit.tracker.ui.theme.LightOnPrimary
import com.trimettransit.tracker.ui.theme.LightOnPrimaryContainer
import com.trimettransit.tracker.ui.theme.LightOnSecondary
import com.trimettransit.tracker.ui.theme.LightOnSurface
import com.trimettransit.tracker.ui.theme.LightOnSurfaceVariant
import com.trimettransit.tracker.ui.theme.LightOutline
import com.trimettransit.tracker.ui.theme.LightPrimary
import com.trimettransit.tracker.ui.theme.LightPrimaryContainer
import com.trimettransit.tracker.ui.theme.LightSecondary
import com.trimettransit.tracker.ui.theme.LightSurface
import com.trimettransit.tracker.ui.theme.LightSurfaceVariant

private val LightScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,
    inversePrimary = LightInversePrimary
)

private val DarkScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    inversePrimary = DarkInversePrimary
)

/**
 * Glance [ColorProviders] for the widget, derived from the configured [WidgetThemeOption] plus the
 * app-wide [AppearanceStyle]. Forced LIGHT/DARK options keep the static palettes in dynamic mode so
 * they ignore system dark mode; with a custom seed colour, LIGHT/DARK/SYSTEM all render the user's
 * palette (SYSTEM also resolves the app's own theme preference against [isSystemDark]).
 */
fun widgetColorProviders(
    option: WidgetThemeOption,
    appearance: AppearanceStyle,
    isSystemDark: Boolean
): ColorProviders {
    val seedActive = appearance.colorMode == ColorMode.SEED
    val dark = when (appearance.theme) {
        ThemePreference.DARK -> true
        ThemePreference.LIGHT -> false
        else -> isSystemDark
    }
    return when (option) {
        WidgetThemeOption.SYSTEM -> when {
            seedActive -> material3ColorProviders(
                seedColorSchemeFor(
                    appearance.seedColor,
                    dark,
                    appearance.amoledDark,
                    appearance.vibrancy
                )
            )
            else -> DynamicThemeColorProviders
        }
        WidgetThemeOption.LIGHT -> material3ColorProviders(
            if (seedActive) {
                seedColorSchemeFor(appearance.seedColor, false, false, appearance.vibrancy)
            } else {
                LightScheme.withVibrancy(appearance.vibrancy)
            }
        )
        WidgetThemeOption.DARK -> material3ColorProviders(
            if (seedActive) {
                seedColorSchemeFor(appearance.seedColor, true, appearance.amoledDark, appearance.vibrancy)
            } else {
                applyWidgetPalette(DarkScheme, appearance)
            }
        )
    }
}

/** Applies vibrancy and AMOLED-black to a static scheme (dynamic-mode forced dark). */
private fun applyWidgetPalette(scheme: ColorScheme, appearance: AppearanceStyle): ColorScheme {
    var styled = scheme.withVibrancy(appearance.vibrancy)
    if (appearance.amoledDark) styled = styled.asAmoled()
    return styled
}