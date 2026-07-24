package com.trimettransit.tracker.ui.screens.stops

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
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.ui.TransitApi
import com.trimettransit.tracker.ui.screens.components.LoadingState
import com.trimettransit.tracker.ui.screens.components.ErrorState
import com.trimettransit.tracker.ui.screens.components.EmptyState
import com.trimettransit.tracker.ui.screens.components.StopListItem
import com.trimettransit.tracker.util.ApiKeys

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
            val url = context.getString(com.trimettransit.tracker.R.string.base_route_url) +
                    "/appID/$key/route/$routeId/dir/$directionId/stops/true"
            stops = TransitApi.fetchStops(context, url)
        }
        isLoading = false
    }

    if (isLoading) {
        LoadingState()
    } else {
        val safeStops = stops
        when {
            safeStops == null -> {
                ErrorState(
                    message = if (isMissingApiKey) "API key not configured.\nPlease check app settings."
                               else "Unable to load stops.\nCheck your connection."
                )
            }
            safeStops.isEmpty() -> {
                EmptyState(message = "No stops available.")
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(safeStops, key = { it.locId }) { stop ->
                        StopListItem(
                            stop = stop,
                            onClick = { onStopSelected(stop) }
                        )
                    }
                }
            }
        }
    }
}
