package com.trimettransit.tracker.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.theme.appCardShape

/**
 * Round-cornered transit badge: the line number for a bus, the mode letter otherwise,
 * filled with the type colour and an ink that stays legible on custom palettes.
 *
 * One implementation for every screen that draws a line badge. Callers pass the letter
 * they already derive from their domain model, so this stays free of trip/arrival types.
 */
@Composable
fun TransitBadge(
    label: String,
    transitLetter: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    compact: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    val overrides = LocalAppearanceStyle.current.transitTypeColors
    val badgeSize: Dp = if (compact) 22.dp else 28.dp
    Surface(
        shape = appCardShape(),
        color = transitColor(transitLetter, scheme, overrides),
        modifier = if (contentDescription != null) {
            modifier.semantics { this.contentDescription = contentDescription }
        } else {
            modifier
        }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(badgeSize)
                .clearAndSetSemantics { }
        ) {
            Text(
                text = label,
                style = if (compact) {
                    MaterialTheme.typography.labelSmall
                } else {
                    MaterialTheme.typography.labelMedium
                },
                fontWeight = FontWeight.Bold,
                color = transitOnColor(transitLetter, scheme, overrides),
                maxLines = 1
            )
        }
    }
}
