package com.trimettransit.tracker.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.appearance.LocalAppearanceStyle
import com.trimettransit.tracker.ui.appearance.rowContentPadding
import com.trimettransit.tracker.ui.theme.appCardShape

const val STOP_SEARCH_MAX_RESULTS = 250

/**
 * Shared filter over the full stop list used by search fields (Home search bar,
 * trip planner endpoint picking). Blank queries yield nothing; digits match stop
 * IDs too.
 */
fun searchStops(allStops: List<Stop>, query: String): List<Stop> {
    // Trim once up front; per-row matching is case-insensitive without allocating
    // lowercased copies of every stop name on each keystroke.
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return emptyList()

    val queryAsInt = trimmed.toIntOrNull()

    return allStops.filter { stop ->
        val matchDesc = stop.desc.contains(trimmed, ignoreCase = true)
        val matchDir = stop.dirDesc.contains(trimmed, ignoreCase = true)
        val matchId = queryAsInt != null && stop.locId.toString().contains(queryAsInt.toString())
        matchDesc || matchDir || matchId
    }.take(STOP_SEARCH_MAX_RESULTS)
}

/** Result row for a stop found through search. */
@Composable
fun StopSearchItem(
    stop: Stop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gridMode: Boolean = false,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .then(if (gridMode) Modifier else Modifier.padding(
                horizontal = 16.dp,
                vertical = rowContentPadding(comfortable = 12.dp, compact = 6.dp)
            )),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val typeColor = transitColor(
            stop.transitType,
            colorScheme,
            LocalAppearanceStyle.current.transitTypeColors
        )
        Surface(
            modifier = Modifier.size(40.dp),
            shape = appCardShape(),
            color = typeColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = transitIconResource(stop.transitType)),
                    contentDescription = stringResource(transitTypeLabel(stop.transitType)),
                    tint = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.desc,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stop.dirDesc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (trailingContent != null) {
            Spacer(modifier = Modifier.width(4.dp))
            Box(contentAlignment = Alignment.Center) {
                trailingContent()
            }
        }
    }
}