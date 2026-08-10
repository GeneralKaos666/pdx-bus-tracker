package com.trimettransit.tracker.ui.components

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import com.trimettransit.tracker.ui.R

/** Returns a transit-type color derived from the M3 color scheme. */
fun transitColor(type: String?, scheme: ColorScheme): Color = when (type) {
    "B", "S", "T" -> scheme.primary          // Bus, Streetcar
    "R" -> scheme.tertiary               // Rail
    "M" -> scheme.secondary              // MAX Light Rail
    "W" -> scheme.outline                // WES (alt)
    else -> scheme.primary
}

/** Returns the drawable resource ID for a transit-type icon. */
fun transitIconResource(type: String?): Int = when (type) {
    "B" -> R.drawable.ic_transit_bus
    "M", "R" -> R.drawable.ic_transit_rail
    "S", "T" -> R.drawable.ic_transit_streetcar
    "W" -> R.drawable.ic_transit_rail
    else -> R.drawable.ic_transit_bus
}

/** Returns a human-readable label for a transit type (for accessibility). */
fun transitTypeLabel(type: String?): String = when (type) {
    "B" -> "Bus"
    "M", "R" -> "MAX Light Rail"
    "S", "T" -> "Streetcar"
    "W" -> "WES Commuter Rail"
    else -> "Transit"
}

/**
 * Returns the route badge letter for a route number ("M" for MAX, "R" for
 * Rail/Streetcar 90·100, "B" for buses; "" when unknown).
 */
fun transitBadgeLetter(routeNumber: Int): String = when {
    routeNumber == 200 -> "M"
    routeNumber == 100 || routeNumber == 90 -> "R"
    routeNumber in 1..99 -> "B"
    else -> ""
}

/** Letters actually produced by [transitBadgeLetter]. */
fun transitBadgeLetters(): List<String> = listOf("B", "M", "R")
