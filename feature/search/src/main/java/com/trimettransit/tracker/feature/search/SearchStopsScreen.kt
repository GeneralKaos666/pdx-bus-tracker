package com.trimettransit.tracker.feature.search

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.transit.JSONParser
import com.trimettransit.tracker.ui.components.EmptyState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.ErrorState
import com.trimettransit.tracker.ui.components.LoadingState
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.components.transitIconResource
import com.trimettransit.tracker.ui.components.transitTypeLabel
import androidx.compose.ui.res.painterResource
import com.trimettransit.tracker.ui.components.rememberSmoothFlingBehavior
import com.trimettransit.tracker.util.ConnectionUtils
import com.trimettransit.tracker.transit.ApiKeys
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
    var expanded by remember { mutableStateOf(false) }
    var allStops by remember { mutableStateOf<List<Stop>?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var hasError by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DockedSearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { expanded = false },
                        expanded = expanded,
                        onExpandedChange = { expanded = it },
                        enabled = true,
                        placeholder = { Text("Search stops by name or ID") },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        },
                        trailingIcon = { }
                    )
                },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                content = { }
            )

            Crossfade(
                targetState = when {
                    isLoading && allStops == null -> 0
                    hasError && allStops == null -> 1
                    !hasSearched && query.isBlank() -> 2
                    hasSearched && results.isEmpty() -> 3
                    else -> 4
                },
                animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
                label = "searchState"
            ) { state ->
                when (state) {
                    0 -> {
                        LoadingState()
                    }

                    1 -> {
                        ErrorState(message = "No connection.\nPlease check your internet.")
                    }

                    2 -> {
                        EmptyState(message = "Type to search stops")
                    }

                    3 -> {
                        EmptyState(message = "No stops found.")
                    }

                    else -> {
                        ContentEntrance(modifier = Modifier.fillMaxSize()) {
                        val smoothFling = rememberSmoothFlingBehavior()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            flingBehavior = smoothFling
                        ) {
                            items(results, key = { it.locId }, contentType = { "stopSearch" }) { stop ->
                                StopSearchItem(
                                    stop = stop,
                                    onClick = { onNavigateToArrivals(stop, -1) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                        }
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
        // Transit type indicator
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

private fun searchStops(allStops: List<Stop>, query: String): List<Stop> {
    val trimmed = query.trim().lowercase(Locale.US)
    if (trimmed.isEmpty()) return emptyList()

    val queryAsInt = trimmed.toIntOrNull()

    return allStops.filter { stop ->
        val matchDesc = stop.desc?.lowercase(Locale.US)?.contains(trimmed) == true
        val matchDir = stop.dirDesc?.lowercase(Locale.US)?.contains(trimmed) == true
        val matchId = queryAsInt != null && stop.locId.toString().contains(queryAsInt.toString())
        matchDesc || matchDir || matchId
    }.take(MAX_RESULTS)
}

private fun loadAllStops(context: Context): List<Stop>? {
    return try {
        val key = ApiKeys.getTrimetApiKey()
        if (key.isBlank()) return null

        if (!ConnectionUtils.isOnline(context)) return null

        val url = context.getString(R.string.base_route_url) + "/appID/$key/dir/true/stops/true"
        val json = JSONParser.fetch(url) ?: return null
        TransitApi.parseRouteConfigStops(json)
    } catch (e: Exception) {
        Log.e(TAG, "loadAllStops failed", e)
        null
    }
}

private object TransitApi {
    fun parseRouteConfigStops(json: org.json.JSONObject): List<Stop>? {
        val resultSet = json.optJSONObject("resultSet") ?: return null
        val routeArr = resultSet.optJSONArray("route") ?: return null
        val seen = HashSet<Int>()
        val stops = mutableListOf<Stop>()
        for (ri in 0 until routeArr.length()) {
            val routeObj = routeArr.getJSONObject(ri)
            val dirArr = routeObj.optJSONArray("dir") ?: continue
            val routeNum = routeObj.optInt("route", 0)
            for (di in 0 until dirArr.length()) {
                val dirObj = dirArr.getJSONObject(di)
                val stopArr = dirObj.optJSONArray("stop") ?: continue
                val dirDesc = dirObj.optString("desc", "")
                for (si in 0 until stopArr.length()) {
                    val obj = stopArr.getJSONObject(si)
                    val locId = obj.optInt("locid", 0)
                    if (!seen.add(locId)) continue
                    val stop = Stop()
                    stop.locId = locId
                    stop.desc = obj.optString("desc", "")
                    val stopDir = obj.optString("dir", "")
                    stop.dirDesc = if (stopDir == "") dirDesc else stopDir
                    stop.latitude = obj.optDouble("lat", 0.0)
                    stop.longitude = if (obj.has("lng")) obj.getDouble("lng") else obj.optDouble("lon", 0.0)
                    stop.routeNum = routeNum

                    stops.add(stop)
                }
            }
        }
        return stops
    }
}
