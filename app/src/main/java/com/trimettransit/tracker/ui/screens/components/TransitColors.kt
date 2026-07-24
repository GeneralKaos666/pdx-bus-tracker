package com.trimettransit.tracker.ui.screens.components

import androidx.compose.ui.graphics.Color

/** Returns a transit-type color (Bus/Streetcar = blue, MAX = orange, WES = gray). */
fun transitColor(type: String?): Color = when (type) {
    "B", "T" -> Color(0xFF0070C0)
    "R" -> Color(0xFFE87722)
    "M" -> Color(0xFF008542)
    "W" -> Color(0xFF414141)
    else -> Color(0xFF0070C0)
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
