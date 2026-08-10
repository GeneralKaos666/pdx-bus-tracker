package com.trimettransit.tracker.feature.stops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.transit.TransitApi
import com.trimettransit.tracker.ui.components.StopListItem
import com.trimettransit.tracker.transit.ApiKeys

@Composable
fun StopsStopList(
    routeId: Int,
    directionId: Int,
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
            val url = context.getString(com.trimettransit.tracker.transit.R.string.base_route_url) +
                    "/appID/$key/route/$routeId/dir/$directionId/stops/true"
            stops = TransitApi.fetchStops(context, url)
        }
        isLoading = false
    }

    val safeStops = stops
    StopListContent(
        isLoading = isLoading,
        items = safeStops,
        errorMessage = if (isMissingApiKey) "API key not configured.\nPlease check app settings."
                       else "Unable to load stops.\nCheck your connection.",
        emptyMessage = "No stops available.",
        stateLabel = "stopsState",
        key = { it.locId },
        contentType = { "stop" }
    ) { stop ->
        StopListItem(
            stop = stop,
            onClick = { onStopSelected(stop) },
            modifier = Modifier.animateItem()
        )
    }
}
