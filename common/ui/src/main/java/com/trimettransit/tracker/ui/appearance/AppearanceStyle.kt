package com.trimettransit.tracker.ui.appearance

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.theme.TrimetBlue

/** How the OS-level light/dark preference maps onto the app. */
enum class ThemePreference {
    SYSTEM, LIGHT, DARK
}

/** What feeds the color scheme: the wallpaper (Material You) or a user-chosen seed color. */
enum class ColorMode {
    DYNAMIC, SEED
}

/** Saturation boost applied to the whole scheme after it is generated. */
enum class Vibrancy(val factor: Float) {
    MUTED(1.0f),
    DEFAULT(1.25f),
    VIBRANT(1.5f)
}

/** Row/padding preset used across screens. */
enum class Density {
    COMFORTABLE, COMPACT
}

/** In-app type scale override on top of the system font size. */
enum class FontScale {
    SMALLER, DEFAULT, LARGER
}

/** How lively the M3 motion schemes are. */
enum class MotionIntensity {
    LOW, DEFAULT, EXPRESSIVE
}

/** User-chosen per-transit-type colors; `null` falls back to scheme-derived tokens. */
data class TransitTypeColors(
    val bus: Color? = null,
    val rail: Color? = null,
    val streetcar: Color? = null,
    val wes: Color? = null
) {
    val isCustomized: Boolean
        get() = bus != null || rail != null || streetcar != null || wes != null
}

/**
 * Every user-controllable appearance token, mirroring the [CardStyle] pattern but app-wide.
 * Holds sane defaults so call sites that don't set one (widget config, previews, tests) behave
 * exactly like today without plumbing.
 */
data class AppearanceStyle(
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val colorMode: ColorMode = ColorMode.DYNAMIC,
    val seedColor: Color = TrimetBlue,
    val vibrancy: Vibrancy = Vibrancy.DEFAULT,
    val amoledDark: Boolean = false,
    val density: Density = Density.COMFORTABLE,
    val fontScale: FontScale = FontScale.DEFAULT,
    val pillAccent: Color? = null,
    val transitTypeColors: TransitTypeColors = TransitTypeColors(),
    val motionIntensity: MotionIntensity = MotionIntensity.EXPRESSIVE
)

/** Parses a stored colour-string pref ("#AARRGGBB" or "#RRGGBB"); empty/unparseable → null. */
fun parseColorSpec(raw: String): Color? {
    if (raw.isBlank()) return null
    val cleaned = raw.removePrefix("#")
    val argb = when (cleaned.length) {
        6 -> "FF$cleaned"
        8 -> cleaned
        else -> return null
    }
    return argb.toLongOrNull(16)?.let { Color(it) }
}

/** Round-trips a [Color] to its persisted "#AARRGGBB" string, or `""` for null/auto. */
fun colorToSpec(color: Color?): String {
    if (color == null) return ""
    return "#%08X".format(color.toArgb())
}

/**
 * Vertical padding for list-row content at the current density: 16.dp comfortable / 8.dp
 * compact by default; pass in smaller values (e.g. 12.dp / 6.dp) for shorter rows.
 */
@Composable
fun rowContentPadding(comfortable: Dp = 16.dp, compact: Dp = 8.dp): Dp =
    if (LocalAppearanceStyle.current.density == Density.COMPACT) compact else comfortable