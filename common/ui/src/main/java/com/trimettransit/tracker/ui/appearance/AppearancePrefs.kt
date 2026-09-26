package com.trimettransit.tracker.ui.appearance

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Single source of truth for appearance pref keys and their stored-value strings.
 * Phone screens and the app Activity read/write through here instead of duplicating literals.
 *
 * IMPORTANT: each key's stored type (noted per key below) is part of its API. A key written
 * with one `put*` and read with a different `get*` throws ClassCastException (e.g. an Int
 * under a `getString` read). Never change a key's type — add a new key instead.
 */
object AppearancePrefs {
    const val THEME = "theme"                            // String: "system" | "light" | "dark"
    const val DYNAMIC_COLOR = "pref_key_dynamic_color"   // Boolean (legacy, pre-v4.18)
    const val CARDS_OUTLINES = "pref_key_card_outlines"  // Boolean
    const val CARDS_OUTLINE_COLOR = "pref_key_card_outline_color" // String: "#AARRGGBB" or "" → "auto"
    const val CARDS_CORNER_STYLE = "pref_key_card_corner_style"   // String: "rounded" | "cut"
    const val CARDS_CORNER_RADIUS = "pref_key_card_corner_radius" // Int dp
    const val ARRIVALS_ONLY_SELECTED_ROUTE = "pref_key_only_show_route_selected" // Boolean

    const val COLOR_MODE = "pref_key_accent_mode"        // String: "dynamic" | "seed"
    const val ACCENT_COLOR = "pref_key_accent_color"     // String: "#AARRGGBB" or "" (→ TrimetBlue)
    const val VIBRANCY = "pref_key_vibrancy"             // String: "muted" | "default" | "vibrant"
    const val AMOLED_DARK = "pref_key_amoled_dark"       // Boolean
    const val PILL_ACCENT = "pref_key_pill_accent"       // String: "#AARRGGBB" or "" (→ scheme primary)
    const val TRANSIT_BUS = "pref_key_transit_bus_color" // String: "#AARRGGBB" or "" (→ scheme)
    const val TRANSIT_RAIL = "pref_key_transit_rail_color"
    const val TRANSIT_STREETCAR = "pref_key_transit_streetcar_color"
    const val TRANSIT_WES = "pref_key_transit_wes_color"
    const val DENSITY = "pref_key_density"              // String: "comfortable" | "compact"
    const val FONT_SCALE = "pref_key_font_scale"        // String: "smaller" | "default" | "larger"
    const val MOTION = "pref_key_motion"                // String: "low" | "default" | "expressive"
    const val FAVORITES_COLUMNS = "pref_key_favorites_columns" // String: "auto" | "one" | "two"
    const val ARRIVALS_REFRESH_SECONDS = "pref_key_arrivals_refresh_seconds" // Int: 15..300
    const val ARRIVALS_SHOW_CLOCK = "pref_key_arrivals_show_clock"           // Boolean
    const val ARRIVALS_SHOW_ROUTE_BADGES = "pref_key_arrivals_show_route_badges" // Boolean
    const val ARRIVALS_SHOW_VEHICLE_INFO = "pref_key_arrivals_show_vehicle_info" // Boolean
    const val MAP_STYLE = "pref_key_map_style" // String: "streets" | "bright" | "positron" | "dark"

    const val SETTINGS_CARD_OUTLINES = CARDS_OUTLINES
    const val SETTINGS_CARD_OUTLINE_COLOR = CARDS_OUTLINE_COLOR
    const val SETTINGS_CARD_CORNER_STYLE = CARDS_CORNER_STYLE
    const val SETTINGS_CARD_CORNER_RADIUS = CARDS_CORNER_RADIUS
    const val SETTINGS_ONLY_SHOW_SELECTED_ROUTE = ARRIVALS_ONLY_SELECTED_ROUTE

    object Values {
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val COLOR_MODE_DYNAMIC = "dynamic"
        const val COLOR_MODE_SEED = "seed"
        const val VIBRANCY_MUTED = "muted"
        const val VIBRANCY_DEFAULT = "default"
        const val VIBRANCY_VIBRANT = "vibrant"
        const val DENSITY_COMFORTABLE = "comfortable"
        const val DENSITY_COMPACT = "compact"
        const val FONT_SMALLER = "smaller"
        const val FONT_DEFAULT = "default"
        const val FONT_LARGER = "larger"
        const val MOTION_LOW = "low"
        const val MOTION_DEFAULT = "default"
        const val MOTION_EXPRESSIVE = "expressive"
        const val FAVORITES_AUTO = "auto"
        const val FAVORITES_ONE = "one"
        const val FAVORITES_TWO = "two"
        const val CARD_OUTLINE_AUTO = "auto"
        const val CARD_STYLE_ROUNDED = "rounded"
        const val CARD_STYLE_CUT = "cut"
    }

    const val DEFAULT_THEME = Values.THEME_SYSTEM
    const val DEFAULT_COLOR_MODE = Values.COLOR_MODE_DYNAMIC
    const val DEFAULT_VIBRANCY = Values.VIBRANCY_DEFAULT
    const val DEFAULT_DENSITY = Values.DENSITY_COMFORTABLE
    const val DEFAULT_FONT_SCALE = Values.FONT_DEFAULT
    const val DEFAULT_MOTION = Values.MOTION_EXPRESSIVE
    const val DEFAULT_FAVORITES_COLUMNS = Values.FAVORITES_AUTO
    const val DEFAULT_CARD_OUTLINE_COLOR = Values.CARD_OUTLINE_AUTO
    const val DEFAULT_CARD_CORNER_STYLE = Values.CARD_STYLE_ROUNDED
    const val DEFAULT_CARD_CORNER_RADIUS = 16
    const val DEFAULT_ARRIVALS_REFRESH_SECONDS = 30
    const val DEFAULT_ONLY_SHOW_SELECTED_ROUTE = true
    const val DEFAULT_ARRIVALS_SHOW_CLOCK = true
    const val DEFAULT_ARRIVALS_SHOW_ROUTE_BADGES = true
    const val DEFAULT_ARRIVALS_SHOW_VEHICLE_INFO = true
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
    motion: String,
    favoritesColumns: String = AppearancePrefs.DEFAULT_FAVORITES_COLUMNS
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
    },
    favoritesColumns = when (favoritesColumns) {
        AppearancePrefs.Values.FAVORITES_ONE -> FavoritesColumns.ONE
        AppearancePrefs.Values.FAVORITES_TWO -> FavoritesColumns.TWO
        else -> FavoritesColumns.AUTO
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
        motion = prefs.getString(AppearancePrefs.MOTION, "expressive") ?: "expressive",
        favoritesColumns = prefs.getString(
            AppearancePrefs.FAVORITES_COLUMNS,
            AppearancePrefs.DEFAULT_FAVORITES_COLUMNS
        ) ?: AppearancePrefs.DEFAULT_FAVORITES_COLUMNS
    )
}

/** Effective Favorites grid span: forced 1/2 win, Auto follows width (>=600dp = 2). Pure. */
fun favoritesColumnCount(raw: String, dense: Boolean): Int = when (raw) {
    AppearancePrefs.Values.FAVORITES_ONE -> 1
    AppearancePrefs.Values.FAVORITES_TWO -> 2
    else -> if (dense) 2 else 1
}

/**
 * Arrivals refresh cadence as a delay in millis, clamped to the tuning range (15 s – 5 min).
 * The stored value is seconds under [AppearancePrefs.ARRIVALS_REFRESH_SECONDS]; read it as
 * an Int — a previous build read this key as a String, which crashed at runtime.
 */
fun SharedPreferences.refreshDelayMillis(): Long =
    refreshDelayMillis(getInt(AppearancePrefs.ARRIVALS_REFRESH_SECONDS, 30))

/** Pure variant of [refreshDelayMillis] so clamping/defaulting is unit-testable. */
fun refreshDelayMillis(secondsStored: Int): Long = secondsStored.coerceIn(15, 300) * 1000L