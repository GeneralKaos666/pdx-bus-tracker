package com.trimettransit.tracker.ui.appearance

/**
 * Selectable OpenFreeMap basemap styles. The light presets keep a dark variant for dark
 * mode (and "Always dark" forces it), so the map always stays legible in both themes.
 * URLs are OpenFreeMap style endpoints; the app only ever labels them descriptively.
 */
object MapStyles {
    private const val LIBERTY_URL = "https://tiles.openfreemap.org/styles/liberty"
    private const val BRIGHT_URL = "https://tiles.openfreemap.org/styles/bright"
    private const val POSITRON_URL = "https://tiles.openfreemap.org/styles/positron"
    private const val DARK_URL = "https://tiles.openfreemap.org/styles/dark"

    const val DEFAULT = "streets"

    /** Preferred outdoor-ish, high-contrast light basemap beside the default "Streets" look. */
    const val BRIGHT = "bright"

    /** Minimal light-gray basemap. */
    const val POSITRON = "positron"

    /** Always-dark basemap regardless of theme. */
    const val DARK = "dark"

    val supported: List<String> = listOf(DEFAULT, BRIGHT, POSITRON, DARK)

    /** Resolves the stored pref value to a concrete style URL for the current theme. */
    fun styleUrlFor(preset: String, isDark: Boolean): String = when {
        preset == DARK || isDark -> DARK_URL
        preset == BRIGHT -> BRIGHT_URL
        preset == POSITRON -> POSITRON_URL
        else -> LIBERTY_URL
    }
}