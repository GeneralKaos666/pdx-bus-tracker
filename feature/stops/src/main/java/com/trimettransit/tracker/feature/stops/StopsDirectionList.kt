package com.trimettransit.tracker.feature.stops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.trimettransit.tracker.model.Direction
import com.trimettransit.tracker.transit.TransitApi
import com.trimettransit.tracker.ui.components.LoadingState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.EmptyState
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.transit.ApiKeys
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import androidx.compose.material3.MaterialTheme

@Composable
fun StopsDirectionList(
    routeId: Int,
    routeDesc: String,
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
            val url = context.getString(R.string.base_route_url) +
                    "/appID/$key/route/$routeId/dir/true"
            directions = TransitApi.fetchDirections(context, url)
        }
        isLoading = false
    }

val safeDirections = directions
    Crossfade(
        targetState = when {
            isLoading -> 0
            safeDirections == null -> 1
            safeDirections.isEmpty() -> 2
            else -> 3
        },
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "directionsState"
    ) { state ->
        when (state) {
            0 -> {
                LoadingState()
            }
            1 -> {
                ErrorState(
                    message = if (isMissingApiKey) "API key not configured.\nPlease check app settings."
                              else "Unable to load directions.\nCheck your connection."
                )
            }
            2 -> {
                EmptyState(message = "No directions available.")
            }
            else -> {
                ContentEntrance(modifier = Modifier.fillMaxSize()) {
                val smoothFling = rememberSmoothFlingBehavior()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    flingBehavior = smoothFling,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(safeDirections ?: emptyList(), key = { it.dir }, contentType = { "direction" }) { direction ->
                        val interactionSource = remember { MutableInteractionSource() }
                        Card(
                            onClick = { onDirectionSelected(direction) },
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .pressScale(interactionSource),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Text(
                                text = direction.desc ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            )
                        }
                    }
                }
                }
            }
        }
    }
}
