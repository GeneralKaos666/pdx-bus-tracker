package com.something15525.trimetgo.trimet_go.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class DrawerActions(
    val onHomeClick: () -> Unit = {},
    val onRoutesClick: () -> Unit = {},
    val onSettings: () -> Unit = {},
)

@Composable

fun DrawerContent(actions: DrawerActions) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "TriMet Go",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )
                Text(
                    text = "Portland Transit",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        // Nav items
        Text(
            text = "Home",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { actions.onHomeClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = "Routes",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { actions.onRoutesClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        Text(
            text = "Settings",
            modifier = Modifier
                .fillMaxWidth()
                .clickable { actions.onSettings() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
