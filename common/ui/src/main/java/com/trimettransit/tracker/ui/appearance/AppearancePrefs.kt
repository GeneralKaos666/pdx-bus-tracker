package com.trimettransit.tracker.ui.appearance

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Single source of truth for appearance pref keys and their stored-value strings.
 * Phone screens and the app Activity read/write through here instead of duplicating literals.
 */
object AppearancePrefs {
    const val THEME = "theme"
    const val DYNAMIC_COLOR = "pref_key_dynamic_color"
    const val CARDS_OUTLINES = "pref_key_card_outlines"
    const val CARDS_OUTLINE_COLOR = "pref_key_card_outline_color"
    const val CARDS_CORNER_STYLE = "pref_key_card_corner_style"
    const val CARDS_CORNER_RADIUS = "pref_key_card_corner_radius"
    const val ARRIVALS_ONLY_SELECTED_ROUTE = "pref_key_only_show_route_selected"

    const val COLOR_MODE = "pref_key_accent_mode"        // "dynamic" | "seed"
    const val ACCENT_COLOR = "pref_key_accent_color"     // "#AARRGGBB" or "" (→ TrimetBlue)
    const val VIBRANCY = "pref_key_vibrancy"             // "muted" | "default" | "vibrant"
    const val AMOLED_DARK = "pref_key_amoled_dark"
    const val PILL_ACCENT = "pref_key_pill_accent"       // "#AARRGGBB" or "" (→ scheme primary)
    const val TRANSIT_BUS = "pref_key_transit_bus_color"
    const val TRANSIT_RAIL = "pref_key_transit_rail_color"
    const val TRANSIT_STREETCAR = "pref_key_transit_streetcar_color"
    const val TRANSIT_WES = "pref_key_transit_wes_color"
    const val DENSITY = "pref_key_density"              // "comfortable" | "compact"
    const val FONT_SCALE = "pref_key_font_scale"        // "smaller" | "default" | "larger"
    const val MOTION = "pref_key_motion"                // "low" | "default" | "expressive"
    const val ARRIVALS_REFRESH_SECONDS = "pref_key_arrivals_refresh_seconds"
    const val ARRIVALS_SHOW_CLOCK = "pref_key_arrivals_show_clock"
    const val ARRIVALS_SHOW_ROUTE_BADGES = "pref_key_arrivals_show_route_badges"
    const val ARRIVALS_SHOW_VEHICLE_INFO = "pref_key_arrivals_show_vehicle_info"
}

/** Maps the persisted string prefs onto an [AppearanceStyle]. Pure, so it's unit-testable. */
fun appearanceStyleFromPrefs(
    theme: String,
    colorMode: String,
    accentColor: String,
    vibrancy: String,
    amoledDark: Boolean,
    pillAccent: String,
    transitBus: String = "",
    transitRail: String = "",
    transitStreetcar: String = "",
    transitWes: String = "",
    density: String,
    fontScale: String,
    motion: String
): AppearanceStyle = AppearanceStyle(
    theme = when (theme) {
        "light" -> ThemePreference.LIGHT
        "dark" -> ThemePreference.DARK
        else -> ThemePreference.SYSTEM
    },
    colorMode = if (colorMode == "seed") ColorMode.SEED else ColorMode.DYNAMIC,
    seedColor = parseColorSpec(accentColor) ?: AppearanceStyle().seedColor,
    vibrancy = when (vibrancy) {
        "muted" -> Vibrancy.MUTED
        "vibrant" -> Vibrancy.VIBRANT
        else -> Vibrancy.DEFAULT
    },
    amoledDark = amoledDark,
    pillAccent = parseColorSpec(pillAccent),
    transitTypeColors = TransitTypeColors(
        bus = parseColorSpec(transitBus),
        rail = parseColorSpec(transitRail),
        streetcar = parseColorSpec(transitStreetcar),
        wes = parseColorSpec(transitWes)
    ),
    density = if (density == "compact") Density.COMPACT else Density.COMFORTABLE,
    fontScale = when (fontScale) {
        "smaller" -> FontScale.SMALLER
        "larger" -> FontScale.LARGER
        else -> FontScale.DEFAULT
    },
    motionIntensity = when (motion) {
        "low" -> MotionIntensity.LOW
        "default" -> MotionIntensity.DEFAULT
        else -> MotionIntensity.EXPRESSIVE
    }
)

/** Reads the full [AppearanceStyle] from the app-default SharedPreferences. */
@Composable
fun rememberAppearanceStyle(): AppearanceStyle {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { defaultSharedPreferences(context) }
    return readAppearanceStyle(prefs)
}

/**
 * The app-default SharedPreferences file used everywhere (`androidx.preference`'s default name).
 * Kept dependency-free inside common/ui so this layer needs no Preference artifact.
 */
internal fun defaultSharedPreferences(context: Context): SharedPreferences =
    context.applicationContext.getSharedPreferences(
        context.packageName + "_preferences",
        Context.MODE_PRIVATE
    )

/** Reads the persisted appearance as a plain value (non-composable call sites, e.g. widgets). */
fun readAppearanceStyle(prefs: SharedPreferences): AppearanceStyle {
    // Legacy migration: before v4.18 the dedicated "use dynamic color" boolean existed and
    // its "off" state meant the static TrimetBlue scheme. Map it onto the new seed mode.
    val colorMode = prefs.getString(AppearancePrefs.COLOR_MODE, null) ?: if (
        prefs.getBoolean(AppearancePrefs.DYNAMIC_COLOR, true)
    ) "dynamic" else "seed"
    return appearanceStyleFromPrefs(
        theme = prefs.getString(AppearancePrefs.THEME, "system") ?: "system",
        colorMode = colorMode,
        accentColor = prefs.getString(AppearancePrefs.ACCENT_COLOR, "") ?: "",
        vibrancy = prefs.getString(AppearancePrefs.VIBRANCY, "default") ?: "default",
        amoledDark = prefs.getBoolean(AppearancePrefs.AMOLED_DARK, false),
        pillAccent = prefs.getString(AppearancePrefs.PILL_ACCENT, "") ?: "",
        transitBus = prefs.getString(AppearancePrefs.TRANSIT_BUS, "") ?: "",
        transitRail = prefs.getString(AppearancePrefs.TRANSIT_RAIL, "") ?: "",
        transitStreetcar = prefs.getString(AppearancePrefs.TRANSIT_STREETCAR, "") ?: "",
        transitWes = prefs.getString(AppearancePrefs.TRANSIT_WES, "") ?: "",
        density = prefs.getString(AppearancePrefs.DENSITY, "comfortable") ?: "comfortable",
        fontScale = prefs.getString(AppearancePrefs.FONT_SCALE, "default") ?: "default",
        motion = prefs.getString(AppearancePrefs.MOTION, "expressive") ?: "expressive"
    )
}