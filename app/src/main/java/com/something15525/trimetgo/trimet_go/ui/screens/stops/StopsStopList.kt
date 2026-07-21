package com.something15525.trimetgo.trimet_go.ui.screens.stops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.something15525.trimetgo.trimet_go.data.model.Stop
import com.something15525.trimetgo.trimet_go.util.Constants2
import com.something15525.trimetgo.trimet_go.ui.TransitApi
import com.something15525.trimetgo.trimet_go.ui.screens.components.StopListItem

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
        val key = Constants2.getTrimetApiKey()
        if (key.isBlank()) {
            isMissingApiKey = true
            stops = null
        } else {
            val url = context.getString(com.something15525.trimetgo.trimet_go.R.string.base_route_url) +
                    "/appID/$key/route/$routeId/dir/$directionId/stops/true"
            stops = TransitApi.fetchStops(context, url)
        }
        isLoading = false
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        val safeStops = stops
        if (safeStops == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isMissingApiKey) "API key not configured.\nPlease check app settings."
                           else "Unable to load stops.\nCheck your connection.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (safeStops.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No stops available.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
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
