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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import java.util.Locale

const val STOP_SEARCH_MAX_RESULTS = 250

/**
 * Shared filter over the full stop list used by search fields (Home search bar,
 * trip planner endpoint picking). Blank queries yield nothing; digits match stop
 * IDs too.
 */
fun searchStops(allStops: List<Stop>, query: String): List<Stop> {
    val trimmed = query.trim().lowercase(Locale.US)
    if (trimmed.isEmpty()) return emptyList()

    val queryAsInt = trimmed.toIntOrNull()

    return allStops.filter { stop ->
        val matchDesc = stop.desc.lowercase(Locale.US).contains(trimmed)
        val matchDir = stop.dirDesc.lowercase(Locale.US).contains(trimmed)
        val matchId = queryAsInt != null && stop.locId.toString().contains(queryAsInt.toString())
        matchDesc || matchDir || matchId
    }.take(STOP_SEARCH_MAX_RESULTS)
}

/** Result row for a stop found through search. */
@Composable
fun StopSearchItem(
    stop: Stop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val colorScheme = MaterialTheme.colorScheme
        val typeColor = remember(stop.transitType, colorScheme) {
            transitColor(stop.transitType, colorScheme)
        }
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = typeColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = transitIconResource(stop.transitType)),
                    contentDescription = transitTypeLabel(stop.transitType),
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
    }
}