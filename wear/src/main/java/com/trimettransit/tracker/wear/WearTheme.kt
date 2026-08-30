package com.trimettransit.tracker.wear

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.dynamicColorScheme

/**
 * Material 3 Expressive theme for the watch.
 *
 * When the watch supports watchface-driven dynamic color (API 35+), the UI adopts
 * the user's watchface palette so it blends with the system. Otherwise we fall back
 * to a branded dark scheme built around the app's navy launcher color with a teal
 * E-M3 expressive primary.
 */
@Composable
fun WearBusTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val dynamic = dynamicColorScheme(context)
    MaterialTheme(colorScheme = dynamic ?: BrandDarkColorScheme) {
        content()
    }
}

private val BrandDarkColorScheme = ColorScheme(
    primary = Color(0xFF53D6F2),
    onPrimary = Color(0xFF0B1A2A),
    primaryDim = Color(0xFF1E7A99),
    primaryContainer = Color(0xFF0E4C73),
    onPrimaryContainer = Color(0xFFCBEDFA),
    secondary = Color(0xFF6FE6B9),
    onSecondary = Color(0xFF0B1A2A),
    secondaryDim = Color(0xFF2F9E78),
    secondaryContainer = Color(0xFF124D3B),
    onSecondaryContainer = Color(0xFFC4F3DF),
    background = Color(0xFF0B1A2A),
    onBackground = Color(0xFFE3ECF4),
    surfaceContainerLow = Color(0xFF15242F),
    surfaceContainer = Color(0xFF1C2B3B),
    surfaceContainerHigh = Color(0xFF243545),
    onSurface = Color(0xFFE3ECF4),
    onSurfaceVariant = Color(0xFFB6C2CC),
    outline = Color(0xFF7E8B96),
    outlineVariant = Color(0xFF3A4854)
)