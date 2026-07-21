package com.something15525.trimetgo.trimet_go.ui.screens.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.something15525.trimetgo.trimet_go.data.local.DatabaseHelper
import com.something15525.trimetgo.trimet_go.data.model.Stop
import com.something15525.trimetgo.trimet_go.ui.theme.TrimetBlue
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToArrivals: (Stop) -> Unit
) {
    val context = LocalContext.current
    var favorites by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var recentStops by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var isLoadingFavorites by remember { mutableStateOf(true) }
    var isLoadingRecent by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = DatabaseHelper(context)
        favorites = db.favorites
        isLoadingFavorites = false
    }

    LaunchedEffect(Unit) {
        val db = DatabaseHelper(context)
        recentStops = db.recentStops
        isLoadingRecent = false
    }

    val tabs = listOf("Favorites", "Recent")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.fillMaxWidth(),
            containerColor = TrimetBlue,
            contentColor = Color.White,
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = Color.White
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (pagerState.currentPage == index) Color.White
                                    else Color.White.copy(alpha = 0.7f)
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> HomeStopListScreen(
                    stops = favorites,
                    isLoading = isLoadingFavorites,
                    emptyText = "No favorite stops yet.\nAdd a stop from the Tracker tab.",
                    onNavigateToArrivals = onNavigateToArrivals
                )
                1 -> HomeStopListScreen(
                    stops = recentStops,
                    isLoading = isLoadingRecent,
                    emptyText = "No recent stops.",
                    onNavigateToArrivals = onNavigateToArrivals
                )
            }
        }
    }
}
