package com.trimettransit.tracker.wear

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

@Composable
fun HomeScreen(
    onOpenFavorites: () -> Unit,
    onOpenRecent: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TimeText()
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp)
        ) {
            item { ListHeader { Text("PDX Bus") } }
            item {
                Button(
                    onClick = onOpenFavorites,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Favorites")
                }
            }
            item {
                Button(
                    onClick = onOpenRecent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Recent stops")
                }
            }
        }
    }
}