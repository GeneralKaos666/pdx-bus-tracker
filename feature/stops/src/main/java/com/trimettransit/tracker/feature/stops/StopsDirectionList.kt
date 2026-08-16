package com.trimettransit.tracker.feature.stops

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.transit.TransitApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.transit.ApiKeys

@Composable
fun StopsDirectionList(
    routeId: Int,
    onDirectionSelected: (Direction) -> Unit
) {
    val context = LocalContext.current
    var directions by remember { mutableStateOf<List<Direction>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMissingApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(routeId) {
        val key = ApiKeys.getTrimetApiKey()
        if (key.isBlank()) {
            isMissingApiKey = true
            directions = null
        } else {
            val url = context.getString(com.trimettransit.tracker.transit.R.string.base_route_url) +
                    "/appID/$key/route/$routeId/dir/true"
            directions = TransitApi.fetchDirections(context, url)
        }
        isLoading = false
    }

    val safeDirections = directions
    StopListContent(
        isLoading = isLoading,
        items = safeDirections,
        errorMessage = if (isMissingApiKey) "API key not configured.\nPlease check app settings."
                       else "Unable to load directions.\nCheck your connection.",
        emptyMessage = "No directions available.",
        stateLabel = "directionsState",
        key = { it.dir },
        contentType = { "direction" }
    ) { direction ->
        val interactionSource = remember { MutableInteractionSource() }
        Card(
            onClick = { onDirectionSelected(direction) },
            interactionSource = interactionSource,
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .pressScale(interactionSource),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = direction.desc,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }
    }
}
