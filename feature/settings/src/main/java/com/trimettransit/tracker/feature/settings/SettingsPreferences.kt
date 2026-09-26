package com.trimettransit.tracker.feature.settings

import android.content.SharedPreferences
import androidx.core.content.edit
import com.trimettransit.tracker.ui.appearance.AppearancePrefs
import com.trimettransit.tracker.ui.appearance.MapStyles
import kotlin.math.roundToInt

/**
 * The settings UI's persisted state. Stored names and types are deliberately kept in
 * [AppearancePrefs]; this value object only centralizes defaults and screen updates.
 */
internal data class SettingsPreferenceState(
    val theme: String,
    val colorMode: String,
    val accentColor: String,
    val vibrancy: String,
    val amoledDark: Boolean,
    val pillAccent: String,
    val transitBus: String,
    val transitRail: String,
    val transitStreetcar: String,
    val transitWes: String,
    val density: String,
    val fontScale: String,
    val motion: String,
    val favoritesColumns: String,
    val mapStyle: String,
    val onlyShowSelectedRoute: Boolean,
    val cardOutlines: Boolean,
    val cardOutlineColor: String,
    val cornerRadius: Float,
    val cornerStyle: String,
    val arrivalsRefreshSeconds: Int,
    val showArrivalClock: Boolean,
    val showArrivalRouteBadges: Boolean,
    val showArrivalVehicleInfo: Boolean
) {
    fun save(prefs: SharedPreferences) {
        prefs.edit {
            putString(AppearancePrefs.THEME, theme)
            putString(AppearancePrefs.COLOR_MODE, colorMode)
            putString(AppearancePrefs.ACCENT_COLOR, accentColor)
            putString(AppearancePrefs.VIBRANCY, vibrancy)
            putBoolean(AppearancePrefs.AMOLED_DARK, amoledDark)
            putString(AppearancePrefs.PILL_ACCENT, pillAccent)
            putString(AppearancePrefs.TRANSIT_BUS, transitBus)
            putString(AppearancePrefs.TRANSIT_RAIL, transitRail)
            putString(AppearancePrefs.TRANSIT_STREETCAR, transitStreetcar)
            putString(AppearancePrefs.TRANSIT_WES, transitWes)
            putString(AppearancePrefs.DENSITY, density)
            putString(AppearancePrefs.FONT_SCALE, fontScale)
            putString(AppearancePrefs.MOTION, motion)
            putString(AppearancePrefs.FAVORITES_COLUMNS, favoritesColumns)
            putString(AppearancePrefs.MAP_STYLE, mapStyle)
            putBoolean(AppearancePrefs.ARRIVALS_ONLY_SELECTED_ROUTE, onlyShowSelectedRoute)
            putBoolean(AppearancePrefs.CARDS_OUTLINES, cardOutlines)
            putString(AppearancePrefs.CARDS_OUTLINE_COLOR, cardOutlineColor)
            putInt(AppearancePrefs.CARDS_CORNER_RADIUS, cornerRadius.roundToInt())
            putString(AppearancePrefs.CARDS_CORNER_STYLE, cornerStyle)
            putInt(AppearancePrefs.ARRIVALS_REFRESH_SECONDS, arrivalsRefreshSeconds)
            putBoolean(AppearancePrefs.ARRIVALS_SHOW_CLOCK, showArrivalClock)
            putBoolean(AppearancePrefs.ARRIVALS_SHOW_ROUTE_BADGES, showArrivalRouteBadges)
            putBoolean(AppearancePrefs.ARRIVALS_SHOW_VEHICLE_INFO, showArrivalVehicleInfo)
        }
    }

    companion object {
        fun read(prefs: SharedPreferences): SettingsPreferenceState {
            val colorMode = prefs.getString(AppearancePrefs.COLOR_MODE, null)
                ?: if (prefs.getBoolean(AppearancePrefs.DYNAMIC_COLOR, true)) {
                    AppearancePrefs.Values.COLOR_MODE_DYNAMIC
                } else {
                    AppearancePrefs.Values.COLOR_MODE_SEED
                }
            return SettingsPreferenceState(
                theme = normalizeTheme(
                    prefs.getString(AppearancePrefs.THEME, AppearancePrefs.DEFAULT_THEME)
                ),
                colorMode = normalizeColorMode(colorMode),
                accentColor = prefs.getString(AppearancePrefs.ACCENT_COLOR, "") ?: "",
                vibrancy = normalizeVibrancy(prefs.getString(AppearancePrefs.VIBRANCY, null)),
                amoledDark = prefs.getBoolean(AppearancePrefs.AMOLED_DARK, false),
                pillAccent = prefs.getString(AppearancePrefs.PILL_ACCENT, "") ?: "",
                transitBus = prefs.getString(AppearancePrefs.TRANSIT_BUS, "") ?: "",
                transitRail = prefs.getString(AppearancePrefs.TRANSIT_RAIL, "") ?: "",
                transitStreetcar = prefs.getString(AppearancePrefs.TRANSIT_STREETCAR, "") ?: "",
                transitWes = prefs.getString(AppearancePrefs.TRANSIT_WES, "") ?: "",
                density = normalizeDensity(prefs.getString(AppearancePrefs.DENSITY, null)),
                fontScale = normalizeFontScale(prefs.getString(AppearancePrefs.FONT_SCALE, null)),
                motion = normalizeMotion(prefs.getString(AppearancePrefs.MOTION, null)),
                favoritesColumns = normalizeFavoritesColumns(prefs.getString(AppearancePrefs.FAVORITES_COLUMNS, null)),
                mapStyle = normalizeMapStyle(prefs.getString(AppearancePrefs.MAP_STYLE, null)),
                onlyShowSelectedRoute = prefs.getBoolean(
                    AppearancePrefs.ARRIVALS_ONLY_SELECTED_ROUTE,
                    AppearancePrefs.DEFAULT_ONLY_SHOW_SELECTED_ROUTE
                ),
                cardOutlines = prefs.getBoolean(AppearancePrefs.CARDS_OUTLINES, true),
                cardOutlineColor = prefs.getString(
                    AppearancePrefs.CARDS_OUTLINE_COLOR,
                    AppearancePrefs.DEFAULT_CARD_OUTLINE_COLOR
                ) ?: AppearancePrefs.DEFAULT_CARD_OUTLINE_COLOR,
                cornerRadius = prefs.getInt(
                    AppearancePrefs.CARDS_CORNER_RADIUS,
                    AppearancePrefs.DEFAULT_CARD_CORNER_RADIUS
                ).toFloat(),
                cornerStyle = prefs.getString(
                    AppearancePrefs.CARDS_CORNER_STYLE,
                    AppearancePrefs.DEFAULT_CARD_CORNER_STYLE
                ) ?: AppearancePrefs.DEFAULT_CARD_CORNER_STYLE,
                arrivalsRefreshSeconds = prefs.getInt(
                    AppearancePrefs.ARRIVALS_REFRESH_SECONDS,
                    AppearancePrefs.DEFAULT_ARRIVALS_REFRESH_SECONDS
                ),
                showArrivalClock = prefs.getBoolean(
                    AppearancePrefs.ARRIVALS_SHOW_CLOCK,
                    AppearancePrefs.DEFAULT_ARRIVALS_SHOW_CLOCK
                ),
                showArrivalRouteBadges = prefs.getBoolean(
                    AppearancePrefs.ARRIVALS_SHOW_ROUTE_BADGES,
                    AppearancePrefs.DEFAULT_ARRIVALS_SHOW_ROUTE_BADGES
                ),
                showArrivalVehicleInfo = prefs.getBoolean(
                    AppearancePrefs.ARRIVALS_SHOW_VEHICLE_INFO,
                    AppearancePrefs.DEFAULT_ARRIVALS_SHOW_VEHICLE_INFO
                )
            )
        }
    }
}

internal fun normalizeTheme(value: String?): String = when (value) {
    AppearancePrefs.Values.THEME_LIGHT,
    AppearancePrefs.Values.THEME_DARK,
    AppearancePrefs.Values.THEME_SYSTEM -> value
    else -> AppearancePrefs.DEFAULT_THEME
}

internal fun normalizeColorMode(value: String?): String =
    if (value == AppearancePrefs.Values.COLOR_MODE_SEED) {
        AppearancePrefs.Values.COLOR_MODE_SEED
    } else {
        AppearancePrefs.DEFAULT_COLOR_MODE
    }

internal fun normalizeVibrancy(value: String?): String = when (value) {
    AppearancePrefs.Values.VIBRANCY_MUTED,
    AppearancePrefs.Values.VIBRANCY_VIBRANT,
    AppearancePrefs.Values.VIBRANCY_DEFAULT -> value
    else -> AppearancePrefs.DEFAULT_VIBRANCY
}

internal fun normalizeDensity(value: String?): String =
    if (value == AppearancePrefs.Values.DENSITY_COMPACT) {
        AppearancePrefs.Values.DENSITY_COMPACT
    } else {
        AppearancePrefs.DEFAULT_DENSITY
    }

internal fun normalizeFontScale(value: String?): String = when (value) {
    AppearancePrefs.Values.FONT_SMALLER,
    AppearancePrefs.Values.FONT_LARGER,
    AppearancePrefs.Values.FONT_DEFAULT -> value
    else -> AppearancePrefs.DEFAULT_FONT_SCALE
}

internal fun normalizeMotion(value: String?): String = when (value) {
    AppearancePrefs.Values.MOTION_LOW,
    AppearancePrefs.Values.MOTION_DEFAULT,
    AppearancePrefs.Values.MOTION_EXPRESSIVE -> value
    else -> AppearancePrefs.DEFAULT_MOTION
}

internal fun normalizeFavoritesColumns(value: String?): String = when (value) {
    AppearancePrefs.Values.FAVORITES_ONE,
    AppearancePrefs.Values.FAVORITES_TWO,
    AppearancePrefs.Values.FAVORITES_AUTO -> value
    else -> AppearancePrefs.DEFAULT_FAVORITES_COLUMNS
}

internal fun normalizeMapStyle(value: String?): String = when (value) {
    MapStyles.BRIGHT,
    MapStyles.POSITRON,
    MapStyles.DARK,
    MapStyles.DEFAULT -> value
    else -> MapStyles.DEFAULT
}
