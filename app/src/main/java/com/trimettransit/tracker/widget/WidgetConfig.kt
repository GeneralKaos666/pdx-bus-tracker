package com.trimettransit.tracker.widget

/** Widget color scheme preference; storage strings are the persistent-map values. */
enum class WidgetThemeOption(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark")
}

/** Per-widget-instance configuration, encoded losslessly as Preferences key/value pairs. */
data class WidgetConfig(
    val selectedStopIds: List<String> = emptyList(),
    val arrivalsPerStop: Int = DEFAULT_ARRIVALS_PER_STOP,
    val showClockTime: Boolean = false,
    val theme: WidgetThemeOption = WidgetThemeOption.SYSTEM,
    val compactRows: Boolean = false,
    val titleText: String? = null,
    val hideTitle: Boolean = false
) {
    companion object {
        const val KEY_STOP_IDS = "stop_ids"
        const val KEY_ARRIVALS_PER_STOP = "arrivals_per_stop"
        const val KEY_SHOW_CLOCK_TIME = "show_clock_time"
        const val KEY_THEME = "theme"
        const val KEY_COMPACT_ROWS = "compact_rows"
        const val KEY_TITLE_TEXT = "title_text"
        const val KEY_HIDE_TITLE = "hide_title"

        const val DEFAULT_ARRIVALS_PER_STOP = 2
        const val MIN_ARRIVALS_PER_STOP = 1
        const val MAX_ARRIVALS_PER_STOP = 3

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
            hideTitle = map[KEY_HIDE_TITLE] == "true"
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
}