package com.trimettransit.tracker.feature.stops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.transit.TransitApi
import com.trimettransit.tracker.ui.components.LoadingState
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.EmptyState
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.StopListItem
import com.trimettransit.tracker.transit.ApiKeys
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior

@Composable
fun StopsStopList(
    routeId: Int,
    directionId: Int,
    directionDesc: String,
    onStopSelected: (Stop) -> Unit
) {
    val context = LocalContext.current
    var stops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isMissingApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(routeId, directionId) {
        val key = ApiKeys.getTrimetApiKey()
        if (key.isBlank()) {
            isMissingApiKey = true
            stops = null
        } else {
            val url = context.getString(R.string.base_route_url) +
                    "/appID/$key/route/$routeId/dir/$directionId/stops/true"
            stops = TransitApi.fetchStops(context, url)
        }
        isLoading = false
    }

    val safeStops = stops
    Crossfade(
        targetState = when {
            isLoading -> 0
            safeStops == null -> 1
            safeStops.isEmpty() -> 2
            else -> 3
        },
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "stopsState"
    ) { state ->
        when (state) {
            0 -> {
                LoadingState()
            }
            1 -> {
                ErrorState(
                    message = if (isMissingApiKey) "API key not configured.\nPlease check app settings."
                              else "Unable to load stops.\nCheck your connection."
                )
            }
            2 -> {
                EmptyState(message = "No stops available.")
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
                    items(safeStops ?: emptyList(), key = { it.locId }, contentType = { "stop" }) { stop ->
                        StopListItem(
                            stop = stop,
                            onClick = { onStopSelected(stop) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
                }
            }
        }
    }
}
