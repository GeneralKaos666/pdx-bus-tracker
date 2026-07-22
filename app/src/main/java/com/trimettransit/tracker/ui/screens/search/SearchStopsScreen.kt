package com.trimettransit.tracker.ui.screens.search

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.R
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.model.Route
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.network.JSONParser
import com.trimettransit.tracker.ui.screens.components.transitColor
import com.trimettransit.tracker.ui.screens.components.transitInitial
import com.trimettransit.tracker.util.ConnectionUtils
import com.trimettransit.tracker.util.Constants2
import com.trimettransit.tracker.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private const val TAG = "SearchStopsScreen"
private const val MAX_RESULTS = 250

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchStopsScreen(
    onNavigateToArrivals: (Stop, Int) -> Unit
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var allStops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    // Lazy-load all stops once
    LaunchedEffect(Unit) {
        if (allStops == null && ConnectionUtils.isOnline(context)) {
            isLoading = true
            allStops = withContext(Dispatchers.IO) { loadAllStops(context) }
            isLoading = false
            if (allStops == null) hasError = true
        }
    }

    // Filter whenever query changes
    LaunchedEffect(query, allStops) {
        if (query.isBlank() || allStops == null) {
            results = emptyList()
            return@LaunchedEffect
        }
        hasSearched = true
        results = withContext(Dispatchers.Default) { searchStops(allStops!!, query) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        DockedSearchBar(
            query = query,
            onQueryChange = { query = it },
            onSearch = { },
            active = false,
            onActiveChange = { },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            placeholder = { Text("Search stops by name or ID") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            content = {}
        )

        when {
            isLoading && allStops == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            hasError && allStops == null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No connection.\nPlease check your internet.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            !hasSearched && query.isBlank() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Type to search stops",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            hasSearched && results.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No stops found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(results, key = { it.locId }) { stop ->
                        StopSearchItem(
                            stop = stop,
                            onClick = { onNavigateToArrivals(stop, -1) },
                            onLongClick = { addStopToFavorites(context, stop) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StopSearchItem(
    stop: Stop,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Transit type indicator
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = transitColor(stop.transitType)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = transitInitial(stop.transitType),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.desc ?: "",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stop.dirDesc ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun addStopToFavorites(context: Context, stop: Stop) {
    try {
        val db = DatabaseHelper(context.applicationContext)
        db.addFavorite(stop, null)
        Toast.makeText(context, "Added to favorites", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to add favorite", e)
    }
}

private fun searchStops(allStops: List<Stop>, query: String): List<Stop> {
    val trimmed = query.trim().lowercase(Locale.US)
    if (trimmed.isEmpty()) return emptyList()

    val queryAsInt = trimmed.toIntOrNull()

    return allStops.filter { stop ->
        // Match stop name
        val nameMatch = stop.desc?.lowercase(Locale.US)?.contains(trimmed) == true
        // Match stop ID
        val idMatch = queryAsInt != null && queryAsInt == stop.locId
        // Match nearby landmarks / notes
        val dirMatch = stop.dirDesc?.lowercase(Locale.US)?.contains(trimmed) == true

        nameMatch || idMatch || dirMatch
    }.take(MAX_RESULTS)
}

private fun loadAllStops(context: Context): List<Stop>? {
    return try {
        if (!SecurityUtils.hasConfiguredTrimetApiKey()) {
            Log.w(TAG, "TriMet API key not configured")
            return null
        }
        val url = "${context.getString(R.string.base_route_url)}/appID/${Constants2.getTrimetApiKey()}/dir/true/stops/true"
        val json = JSONParser().fetch(url) ?: return null
        val routeSet = json.getJSONObject("resultSet")
        val routes = routeSet.optJSONArray("route")

        if (routes == null || routes.length() == 0) return emptyList()

        val stopMap = LinkedHashMap<Int, Stop>()

        for (i in 0 until routes.length()) {
            val route = routes.getJSONObject(i)
            val typeLetter = route.optString("type", "")
            val dirs = route.optJSONArray("dir")
            if (dirs == null) continue

            for (j in 0 until dirs.length()) {
                val dir = dirs.getJSONObject(j)
                val stops = dir.optJSONArray("stop")
                if (stops == null) continue

                for (k in 0 until stops.length()) {
                    val stopJson = stops.getJSONObject(k)
                    val locId = stopJson.optInt("locid", 0)
                    if (locId == 0 || stopMap.containsKey(locId)) continue

                    val desc = stopJson.optString("desc", "")
                    val dirDesc = stopJson.optString("dir", null)
                    val latitude = stopJson.optDouble("lat", 0.0)
                    val longitude = stopJson.optDouble("lng", 0.0)

                    stopMap[locId] = Stop(
                        desc,
                        dirDesc,
                        latitude,
                        longitude,
                        typeLetter,
                        locId,
                        emptyList()
                    )
                }
            }
        }

        stopMap.values.toList()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load stops", e)
        null
    }
}
