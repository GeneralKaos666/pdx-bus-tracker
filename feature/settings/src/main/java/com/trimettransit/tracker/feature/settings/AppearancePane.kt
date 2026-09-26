package com.trimettransit.tracker.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsRadioOption
/** Appearance pane: light/dark/system theme radio options. */
@Composable
internal fun AppearancePane(
    selectedTheme: String,
    onThemeChange: (String) -> Unit
)
{
    SettingsCard {
        SettingsRadioOption(
            label = stringResource(R.string.theme_system),
            subtitle = stringResource(R.string.theme_system_subtitle),
            icon = Icons.Filled.BrightnessAuto,
            selected = selectedTheme == "system",
            onClick = {
                onThemeChange("system")
            }
        )
        SettingsRadioOption(
            label = stringResource(R.string.theme_light),
            subtitle = stringResource(R.string.theme_light_subtitle),
            icon = Icons.Filled.LightMode,
            selected = selectedTheme == "light",
            onClick = {
                onThemeChange("light")
            }
        )
        SettingsRadioOption(
            label = stringResource(R.string.theme_dark),
            subtitle = stringResource(R.string.theme_dark_subtitle),
            icon = Icons.Filled.DarkMode,
            selected = selectedTheme == "dark",
            onClick = {
                onThemeChange("dark")
            }
        )
    }}
