package com.trimettransit.tracker.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.appearance.colorToSpec
import com.trimettransit.tracker.ui.appearance.parseColorSpec
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsRadioOption
import com.trimettransit.tracker.ui.components.SettingsSwitchOption
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.theme.TrimetBlue
import com.trimettransit.tracker.ui.theme.m3ContentExpand
import com.trimettransit.tracker.ui.theme.m3ContentShrink
/** Colours pane: dynamic/seed accent, vibrancy, AMOLED dark, pill and transit-type colours. */
@Composable
internal fun ColoursPane(
    accentMode: String,
    onAccentModeChange: (String) -> Unit,
    accentColorRaw: String,
    onAccentColorChange: (String) -> Unit,
    vibrancyRaw: String,
    onVibrancyChange: (String) -> Unit,
    amoledDark: Boolean,
    onAmoledDarkChange: (Boolean) -> Unit,
    pillAccentRaw: String,
    transitBusRaw: String,
    transitRailRaw: String,
    transitStreetcarRaw: String,
    transitWesRaw: String,
    onPickColour: (ColourTarget) -> Unit
)
{
    SettingsCard {
            Column {
                SettingsRadioOption(
                    label = stringResource(R.string.colour_mode_dynamic),
                    subtitle = stringResource(R.string.colour_mode_dynamic_subtitle),
                    icon = Icons.Filled.BrightnessAuto,
                    selected = accentMode == "dynamic",
                    onClick = {
                        onAccentModeChange("dynamic")
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.colour_mode_seed),
                    subtitle = stringResource(R.string.colour_mode_seed_subtitle),
                    icon = Icons.Filled.Colorize,
                    selected = accentMode == "seed",
                    onClick = {
                        onAccentModeChange("seed")
                    }
                )
                AnimatedVisibility(
                    visible = accentMode == "seed",
                    enter = m3ContentExpand(),
                    exit = m3ContentShrink()
                ) {
                    Column {
                        AccentPresetRow(
                            selected = parseColorSpec(accentColorRaw),
                            onSelect = { color ->
                                onAccentColorChange(colorToSpec(color))
                            }
                        )
                        SettingsColourOption(
                            label = stringResource(R.string.accent_custom),
                            subtitle = stringResource(R.string.accent_custom_subtitle),
                            icon = Icons.Filled.Colorize,
                            colour = parseColorSpec(accentColorRaw) ?: TrimetBlue,
                            onClick = { onPickColour(ColourTarget.ACCENT) }
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                SettingsRadioOption(
                    label = stringResource(R.string.vibrancy_muted),
                    subtitle = stringResource(R.string.vibrancy_muted_subtitle),
                    icon = Icons.Filled.BrightnessAuto,
                    selected = vibrancyRaw == "muted",
                    onClick = {
                        onVibrancyChange("muted")
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.vibrancy_default),
                    subtitle = stringResource(R.string.vibrancy_default_subtitle),
                    icon = Icons.Filled.Palette,
                    selected = vibrancyRaw == "default",
                    onClick = {
                        onVibrancyChange("default")
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.vibrancy_vibrant),
                    subtitle = stringResource(R.string.vibrancy_vibrant_subtitle),
                    icon = Icons.Filled.Colorize,
                    selected = vibrancyRaw == "vibrant",
                    onClick = {
                        onVibrancyChange("vibrant")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                SettingsSwitchOption(
                    label = stringResource(R.string.amoled_dark),
                    subtitle = stringResource(R.string.amoled_dark_subtitle),
                    icon = Icons.Filled.DarkMode,
                    checked = amoledDark,
                    onCheckedChange = {
                        onAmoledDarkChange(it)
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                val scheme = MaterialTheme.colorScheme
                SettingsColourOption(
                    label = stringResource(R.string.pill_accent),
                    subtitle = stringResource(R.string.pill_accent_subtitle),
                    icon = Icons.Filled.Colorize,
                    colour = parseColorSpec(pillAccentRaw) ?: scheme.primary,
                    onClick = { onPickColour(ColourTarget.PILL_ACCENT) }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                SettingsColourOption(
                    label = stringResource(R.string.transit_bus),
                    subtitle = stringResource(R.string.transit_bus_subtitle),
                    icon = Icons.Filled.Route,
                    colour = parseColorSpec(transitBusRaw) ?: transitColor("B", scheme),
                    onClick = { onPickColour(ColourTarget.TRANSIT_BUS) }
                )
                SettingsColourOption(
                    label = stringResource(R.string.transit_rail),
                    subtitle = stringResource(R.string.transit_rail_subtitle),
                    icon = Icons.Filled.Route,
                    colour = parseColorSpec(transitRailRaw) ?: transitColor("M", scheme),
                    onClick = { onPickColour(ColourTarget.TRANSIT_RAIL) }
                )
                SettingsColourOption(
                    label = stringResource(R.string.transit_streetcar),
                    subtitle = stringResource(R.string.transit_streetcar_subtitle),
                    icon = Icons.Filled.Route,
                    colour = parseColorSpec(transitStreetcarRaw) ?: transitColor("S", scheme),
                    onClick = { onPickColour(ColourTarget.TRANSIT_STREETCAR) }
                )
                SettingsColourOption(
                    label = stringResource(R.string.transit_wes),
                    subtitle = stringResource(R.string.transit_wes_subtitle),
                    icon = Icons.Filled.Route,
                    colour = parseColorSpec(transitWesRaw) ?: transitColor("W", scheme),
                    onClick = { onPickColour(ColourTarget.TRANSIT_WES) }
                )
            }
    }}
