package com.trimettransit.tracker.feature.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.trimettransit.tracker.ui.appearance.MapStyles
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsRadioOption
/** Maps pane: base-map style radio options. */
@Composable
internal fun MapsPane(
    mapStyleRaw: String,
    onMapStyleChange: (String) -> Unit
)
{
    SettingsCard {
        SettingsRadioOption(
            label = stringResource(R.string.map_style_streets),
            subtitle = stringResource(R.string.map_style_streets_subtitle),
            icon = Icons.Filled.Map,
            selected = mapStyleRaw == MapStyles.DEFAULT,
            onClick = {
                onMapStyleChange(MapStyles.DEFAULT)
            }
        )
        SettingsRadioOption(
            label = stringResource(R.string.map_style_bright),
            subtitle = stringResource(R.string.map_style_bright_subtitle),
            icon = Icons.Filled.Map,
            selected = mapStyleRaw == MapStyles.BRIGHT,
            onClick = {
                onMapStyleChange(MapStyles.BRIGHT)
            }
        )
        SettingsRadioOption(
            label = stringResource(R.string.map_style_positron),
            subtitle = stringResource(R.string.map_style_positron_subtitle),
            icon = Icons.Filled.Map,
            selected = mapStyleRaw == MapStyles.POSITRON,
            onClick = {
                onMapStyleChange(MapStyles.POSITRON)
            }
        )
        SettingsRadioOption(
            label = stringResource(R.string.map_style_dark),
            subtitle = stringResource(R.string.map_style_dark_subtitle),
            icon = Icons.Filled.Map,
            selected = mapStyleRaw == MapStyles.DARK,
            onClick = {
                onMapStyleChange(MapStyles.DARK)
            }
        )
    }}
