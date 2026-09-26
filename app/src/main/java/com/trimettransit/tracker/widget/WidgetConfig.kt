package com.trimettransit.tracker.widget

/** Widget color scheme preference; storage strings are the persistent-map values. */
enum class WidgetThemeOption(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark")
}

/** Widget background source; storage strings are the persistent-map values. */
enum class WidgetBackgroundMode(val storageValue: String) {
    SYSTEM("system"),
    CUSTOM("custom")
}

/** Per-widget-instance configuration, encoded losslessly as Preferences key/value pairs. */
data class WidgetConfig(
    val selectedStopIds: List<String> = emptyList(),
    val arrivalsPerStop: Int = DEFAULT_ARRIVALS_PER_STOP,
    val showClockTime: Boolean = false,
    val theme: WidgetThemeOption = WidgetThemeOption.SYSTEM,
    val compactRows: Boolean = false,
    val titleText: String? = null,
    val hideTitle: Boolean = false,
    val showRouteBadge: Boolean = true,
    val showDetourAlerts: Boolean = true,
    val showArrivalStatus: Boolean = true,
    val maxStops: Int = DEFAULT_MAX_STOPS,
    val backgroundMode: WidgetBackgroundMode = WidgetBackgroundMode.SYSTEM,
    val customBackgroundHex: String? = null,
    val backgroundOpacity: Int = DEFAULT_BACKGROUND_OPACITY,
    val showGridDividers: Boolean = false,
    val outlineMode: WidgetBackgroundMode = WidgetBackgroundMode.SYSTEM,
    val customOutlineHex: String? = null
) {
    companion object {
        const val KEY_STOP_IDS = "stop_ids"
        const val KEY_ARRIVALS_PER_STOP = "arrivals_per_stop"
        const val KEY_SHOW_CLOCK_TIME = "show_clock_time"
        const val KEY_THEME = "theme"
        const val KEY_COMPACT_ROWS = "compact_rows"
        const val KEY_TITLE_TEXT = "title_text"
        const val KEY_HIDE_TITLE = "hide_title"
        const val KEY_SHOW_ROUTE_BADGE = "show_route_badge"
        const val KEY_SHOW_DETOUR_ALERTS = "show_detour_alerts"
        const val KEY_SHOW_ARRIVAL_STATUS = "show_arrival_status"
        const val KEY_MAX_STOPS = "max_stops"
        const val KEY_BACKGROUND_MODE = "background_mode"
        const val KEY_CUSTOM_BACKGROUND = "custom_background"
        const val KEY_BACKGROUND_OPACITY = "background_opacity"
        const val KEY_SHOW_GRID_DIVIDERS = "show_grid_dividers"
        const val KEY_OUTLINE_MODE = "outline_mode"
        const val KEY_CUSTOM_OUTLINE = "custom_outline"

        const val DEFAULT_ARRIVALS_PER_STOP = 2
        const val MIN_ARRIVALS_PER_STOP = 1
        const val MAX_ARRIVALS_PER_STOP = 3

        const val DEFAULT_MAX_STOPS = 12
        const val MIN_MAX_STOPS = 1
        const val MAX_MAX_STOPS = 12

        const val DEFAULT_BACKGROUND_OPACITY = 100
        const val MIN_BACKGROUND_OPACITY = 0
        const val MAX_BACKGROUND_OPACITY = 100

        /** Tolerant parser: any missing or invalid value falls back to its field's default. */
        fun fromPersistentMap(map: Map<String, String>): WidgetConfig = WidgetConfig(
            selectedStopIds = map[KEY_STOP_IDS].orEmpty()
                .split(",")
                .filter { it.isNotBlank() },
            arrivalsPerStop = map[KEY_ARRIVALS_PER_STOP]
                ?.toIntOrNull()
                ?.takeIf { it in MIN_ARRIVALS_PER_STOP..MAX_ARRIVALS_PER_STOP }
                ?: DEFAULT_ARRIVALS_PER_STOP,
            showClockTime = map[KEY_SHOW_CLOCK_TIME] == "true",
            theme = map[KEY_THEME]
                ?.let { value -> WidgetThemeOption.entries.firstOrNull { it.storageValue == value } }
                ?: WidgetThemeOption.SYSTEM,
            compactRows = map[KEY_COMPACT_ROWS] == "true",
            titleText = map[KEY_TITLE_TEXT],
            hideTitle = map[KEY_HIDE_TITLE] == "true",
            showRouteBadge = map[KEY_SHOW_ROUTE_BADGE] != "false",
            showDetourAlerts = map[KEY_SHOW_DETOUR_ALERTS] != "false",
            showArrivalStatus = map[KEY_SHOW_ARRIVAL_STATUS] != "false",
            maxStops = map[KEY_MAX_STOPS]
                ?.toIntOrNull()
                ?.takeIf { it in MIN_MAX_STOPS..MAX_MAX_STOPS }
                ?: DEFAULT_MAX_STOPS,
            backgroundMode = map[KEY_BACKGROUND_MODE]
                ?.let { value -> WidgetBackgroundMode.entries.firstOrNull { it.storageValue == value } }
                ?: WidgetBackgroundMode.SYSTEM,
            customBackgroundHex = map[KEY_CUSTOM_BACKGROUND]?.trim()?.takeIf { it.isNotBlank() },
            backgroundOpacity = map[KEY_BACKGROUND_OPACITY]
                ?.toIntOrNull()
                ?.takeIf { it in MIN_BACKGROUND_OPACITY..MAX_BACKGROUND_OPACITY }
                ?: DEFAULT_BACKGROUND_OPACITY,
            showGridDividers = map[KEY_SHOW_GRID_DIVIDERS] == "true",
            outlineMode = map[KEY_OUTLINE_MODE]
                ?.let { value -> WidgetBackgroundMode.entries.firstOrNull { it.storageValue == value } }
                ?: WidgetBackgroundMode.SYSTEM,
            customOutlineHex = map[KEY_CUSTOM_OUTLINE]?.trim()?.takeIf { it.isNotBlank() }
        )
    }
}

/** Encodes this config as Preferences key/value pairs, losslessly. */
fun WidgetConfig.toPersistentMap(): Map<String, String> = buildMap {
    put(WidgetConfig.KEY_STOP_IDS, selectedStopIds.joinToString(","))
    put(WidgetConfig.KEY_ARRIVALS_PER_STOP, arrivalsPerStop.toString())
    put(WidgetConfig.KEY_SHOW_CLOCK_TIME, showClockTime.toString())
    put(WidgetConfig.KEY_THEME, theme.storageValue)
    put(WidgetConfig.KEY_COMPACT_ROWS, compactRows.toString())
    titleText?.let { put(WidgetConfig.KEY_TITLE_TEXT, it) }
    put(WidgetConfig.KEY_HIDE_TITLE, hideTitle.toString())
    put(WidgetConfig.KEY_SHOW_ROUTE_BADGE, showRouteBadge.toString())
    put(WidgetConfig.KEY_SHOW_DETOUR_ALERTS, showDetourAlerts.toString())
    put(WidgetConfig.KEY_SHOW_ARRIVAL_STATUS, showArrivalStatus.toString())
    put(WidgetConfig.KEY_MAX_STOPS, maxStops.toString())
    put(WidgetConfig.KEY_BACKGROUND_MODE, backgroundMode.storageValue)
    customBackgroundHex?.let { put(WidgetConfig.KEY_CUSTOM_BACKGROUND, it) }
    put(WidgetConfig.KEY_BACKGROUND_OPACITY, backgroundOpacity.toString())
    put(WidgetConfig.KEY_SHOW_GRID_DIVIDERS, showGridDividers.toString())
    put(WidgetConfig.KEY_OUTLINE_MODE, outlineMode.storageValue)
    customOutlineHex?.let { put(WidgetConfig.KEY_CUSTOM_OUTLINE, it) }
}

/**
 * Parses a `#RRGGBB`, `RRGGBB`, `#AARRGGBB` or `AARRGGBB` hex color into an ARGB
 * int, or null when the string is null, blank, or malformed. Pure and
 * Context-free so unit tests pin the shape.
 */
fun parseWidgetBackgroundArgb(hex: String?): Int? {
    val cleaned = hex?.trim()?.removePrefix("#")?.takeIf { it.isNotEmpty() } ?: return null
    if (cleaned.length != 6 && cleaned.length != 8) return null
    if (!cleaned.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) return null
    val full = if (cleaned.length == 6) "FF$cleaned" else cleaned
    return full.toLongOrNull(16)?.toInt()
}

/** Opacity percentage (0..100) as an alpha fraction, coercing out-of-range input. */
fun backgroundOpacityFraction(opacityPercent: Int): Float =
    opacityPercent.coerceIn(
        WidgetConfig.MIN_BACKGROUND_OPACITY,
        WidgetConfig.MAX_BACKGROUND_OPACITY
    ) / 100f
