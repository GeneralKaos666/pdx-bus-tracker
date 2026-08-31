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

/**
 * Returns the M3 on-color that pairs with [transitColor] for a transit type, for painting a
 * legible glyph/center inside a filled marker. In the light scheme these resolve to white (the
 * prior behavior); in the dark scheme they become dark inks on the light pastel fills, so the
 * icon no longer vanishes. WES is intentionally left white for now.
 */
fun transitOnColor(type: String?, scheme: ColorScheme): Color = when (type) {
    "R" -> scheme.onTertiary              // Rail
    "M" -> scheme.onSecondary             // MAX Light Rail
    "W" -> Color.White                    // WES: unchanged for now
    else -> scheme.onPrimary              // Bus, Streetcar
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
 * Returns the route badge letter for a route number ("M" for MAX, "S" for
 * Streetcar, "W" for WES, "B" for buses; "" when unknown).
 * TriMet route numbers: MAX Blue=100, Red=90, Yellow=190, Green=200,
 * Orange=290, Vintage Trolley=196; WES=203; Streetcar A=193, B=194, NS=195.
 */
fun transitBadgeLetter(routeNumber: Int): String = when {
    routeNumber == 90 || routeNumber == 100 || routeNumber == 190 ||
        routeNumber == 200 || routeNumber == 290 || routeNumber == 196 -> "M"
    routeNumber == 203 -> "W"
    routeNumber in 193..195 -> "S"
    routeNumber in 1..99 -> "B"
    else -> ""
}

/** Letters actually produced by [transitBadgeLetter]. */
fun transitBadgeLetters(): List<String> = listOf("B", "M", "S", "W")
