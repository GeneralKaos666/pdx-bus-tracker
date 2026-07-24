@file:Suppress("DEPRECATION", "TYPE_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")

package com.trimettransit.tracker.ui.screens.search

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.R
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.model.Stop
import com.trimettransit.tracker.network.JSONParser
import com.trimettransit.tracker.ui.screens.components.EmptyState
import com.trimettransit.tracker.ui.screens.components.ErrorState
import com.trimettransit.tracker.ui.screens.components.LoadingState
import com.trimettransit.tracker.ui.screens.components.transitColor
import com.trimettransit.tracker.ui.screens.components.transitInitial
import com.trimettransit.tracker.util.ConnectionUtils
import com.trimettransit.tracker.util.Constants2
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

            when {
                isLoading && allStops == null -> {
                    LoadingState()
                }

                hasError && allStops == null -> {
                    ErrorState(message = "No connection.\nPlease check your internet.")
                }

                !hasSearched && query.isBlank() -> {
                    EmptyState(message = "Type to search stops")
                }

                hasSearched && results.isEmpty() -> {
                    EmptyState(message = "No stops found.")
                }

                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(results, key = { it.locId }) { stop ->
                            StopSearchItem(
                                stop = stop,
                                onClick = { onNavigateToArrivals(stop, -1) }
                            )
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
    onClick: () -> Unit
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
                    color = MaterialTheme.colorScheme.surface,
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

@Suppress("unused")
private fun addStopToFavorites(context: Context, stop: Stop): String {
    return try {
        val db = DatabaseHelper(context.applicationContext)
        db.addFavorite(stop, null)
        context.getString(R.string.favorite_added_text)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to add favorite", e)
        "Failed to add favorite"
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
        val key = Constants2.getTrimetApiKey()
        if (key.isBlank()) return null

        if (!ConnectionUtils.isOnline(context)) return null

        val url = context.getString(R.string.base_route_url) + "/appID/$key/dir/true/stops/true"
        val json = JSONParser().fetch(url) ?: return null
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
                    stop.setLocId(locId)
                    stop.setDesc(obj.optString("desc", ""))
                    val stopDir = obj.optString("dir", "")
                    stop.setDirDesc(if (stopDir == "") dirDesc else stopDir)
                    stop.setLatitude(obj.optDouble("lat", 0.0))
                    stop.setLongitude(obj.optDouble("lng", 0.0))
                    stop.setRouteNum(routeNum)

                    stops.add(stop)
                }
            }
        }
        return stops
    }
}
