package com.trimettransit.tracker.ui.screens.stops

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
import com.trimettransit.tracker.data.model.Direction
import com.trimettransit.tracker.ui.TransitApi
import com.trimettransit.tracker.ui.screens.components.LoadingState
import com.trimettransit.tracker.ui.screens.components.ErrorState
import com.trimettransit.tracker.ui.screens.components.EmptyState
import com.trimettransit.tracker.util.ApiKeys
import com.trimettransit.tracker.ui.screens.components.rememberSmoothFlingBehavior
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
            val url = context.getString(com.trimettransit.tracker.R.string.base_route_url) +
                    "/appID/$key/route/$routeId/dir/true"
            directions = TransitApi.fetchDirections(context, url)
        }
        isLoading = false
    }

    if (isLoading) {
        LoadingState()
    } else {
        val safeDirections = directions
        when {
            safeDirections == null -> {
                ErrorState(
                    message = if (isMissingApiKey) "API key not configured.\nPlease check app settings."
                               else "Unable to load directions.\nCheck your connection."
                )
            }
            safeDirections.isEmpty() -> {
                EmptyState(message = "No directions available.")
            }
            else -> {
                val smoothFling = rememberSmoothFlingBehavior()
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    flingBehavior = smoothFling,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(safeDirections, key = { it.dir }, contentType = { "direction" }) { direction ->
                        Card(
                            onClick = { onDirectionSelected(direction) },
                            modifier = Modifier
                                .animateItem()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
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
