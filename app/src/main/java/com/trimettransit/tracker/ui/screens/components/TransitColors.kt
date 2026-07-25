package com.trimettransit.tracker.ui.screens.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color

/** Returns a transit-type color derived from the M3 color scheme. */
fun transitColor(type: String?, scheme: ColorScheme): Color = when (type) {
    "B", "T" -> scheme.primary          // Bus, Streetcar
    "R" -> scheme.tertiary               // MAX light rail
    "M" -> scheme.secondary              // WES commuter rail
    "W" -> scheme.outline                // WES (alt)
    else -> scheme.primary
}

/** Returns a one-letter label for a transit type. */
fun transitInitial(type: String?): String = when (type) {
    "B" -> "B"
    "T" -> "T"
    "R" -> "R"
    "M" -> "M"
    "W" -> "W"
    else -> "?"
}
