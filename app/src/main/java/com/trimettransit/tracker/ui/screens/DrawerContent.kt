package com.trimettransit.tracker.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class DrawerActions(
    val selectedItem: String = "",
    val onHomeClick: () -> Unit = {},
    val onRoutesClick: () -> Unit = {},
    val onVehiclesClick: () -> Unit = {},
    val onSettings: () -> Unit = {},
)

@Composable
fun DrawerContent(actions: DrawerActions) {
    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(24.dp)
            ) {
                Text(
                    text = "TriMet Bus Tracker",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Portland Transit",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Nav items
            NavigationDrawerItem(
                label = { Text("Home") },
                selected = actions.selectedItem == "home",
                onClick = actions.onHomeClick,
                icon = { Icon(Icons.Default.Home, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Routes") },
                selected = actions.selectedItem == "stops",
                onClick = actions.onRoutesClick,
                icon = { Icon(Icons.Default.Map, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            NavigationDrawerItem(
                label = { Text("Vehicles") },
                selected = actions.selectedItem == "vehicle_positions",
                onClick = actions.onVehiclesClick,
                icon = { Icon(Icons.Default.DirectionsBus, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            NavigationDrawerItem(
                label = { Text("Settings") },
                selected = actions.selectedItem == "settings",
                onClick = actions.onSettings,
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
            )
        }
    }
}
