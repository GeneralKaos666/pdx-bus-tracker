package com.trimettransit.tracker.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.trimettransit.tracker.feature.settings.BuildConfig
import com.trimettransit.tracker.ui.components.ContentEntrance
import com.trimettransit.tracker.ui.components.pressScale

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    val currentTheme = remember { prefs.getString("theme", "system") ?: "system" }
    var selectedTheme by remember { mutableStateOf(currentTheme) }
    val onlySelectedRoute = remember { prefs.getBoolean("pref_key_only_show_route_selected", true) }
    var onlyShowSelectedRoute by remember { mutableStateOf(onlySelectedRoute) }

    ContentEntrance(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Theme section
            SectionHeader(title = "Theme")

            SettingsRadioOption(
                label = "System default",
                selected = selectedTheme == "system",
                onClick = {
                    selectedTheme = "system"
                    prefs.edit().putString("theme", "system").apply()
                }
            )
            SettingsRadioOption(
                label = "Light",
                selected = selectedTheme == "light",
                onClick = {
                    selectedTheme = "light"
                    prefs.edit().putString("theme", "light").apply()
                }
            )
            SettingsRadioOption(
                label = "Dark",
                selected = selectedTheme == "dark",
                onClick = {
                    selectedTheme = "dark"
                    prefs.edit().putString("theme", "dark").apply()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Route filter section
            SectionHeader(title = "Arrivals")

            SettingsSwitchOption(
                label = "Only show selected route's arrivals",
                checked = onlyShowSelectedRoute,
                onCheckedChange = {
                    onlyShowSelectedRoute = it
                    prefs.edit().putBoolean("pref_key_only_show_route_selected", it).apply()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // About section
            SectionHeader(title = "About")

            Text(
                text = "TriMet Bus Tracker",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
            Text(
                text = "Version ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsRadioOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

@Composable
private fun SettingsSwitchOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(interactionSource = interactionSource, indication = LocalIndication.current) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp)
        )
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}
