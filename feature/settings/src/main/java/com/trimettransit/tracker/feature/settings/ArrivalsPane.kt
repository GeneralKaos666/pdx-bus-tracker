package com.trimettransit.tracker.feature.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsRadioOption
import com.trimettransit.tracker.ui.components.SettingsSwitchOption
/** Arrivals pane: route filter, clocks/badges/vehicle toggles, and refresh cadence. */
@Composable
internal fun ArrivalsPane(
    onlyShowSelectedRoute: Boolean,
    onOnlyShowSelectedRouteChange: (Boolean) -> Unit,
    showArrivalClock: Boolean,
    onShowArrivalClockChange: (Boolean) -> Unit,
    showArrivalRouteBadges: Boolean,
    onShowArrivalRouteBadgesChange: (Boolean) -> Unit,
    showArrivalVehicleInfo: Boolean,
    onShowArrivalVehicleInfoChange: (Boolean) -> Unit,
    arrivalsRefreshSeconds: Int,
    onArrivalsRefreshChange: (Int) -> Unit
)
{
    SettingsCard {
        SettingsSwitchOption(
            label = stringResource(R.string.only_show_selected_route),
            subtitle = stringResource(R.string.only_show_selected_route_subtitle),
            icon = Icons.Filled.Route,
            checked = onlyShowSelectedRoute,
            onCheckedChange = {
                onOnlyShowSelectedRouteChange(it)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        SettingsSwitchOption(
            label = stringResource(R.string.arrival_show_clock),
            subtitle = stringResource(R.string.arrival_show_clock_subtitle),
            icon = Icons.Filled.AccessTime,
            checked = showArrivalClock,
            onCheckedChange = {
                onShowArrivalClockChange(it)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        SettingsSwitchOption(
            label = stringResource(R.string.arrival_show_route_badges),
            subtitle = stringResource(R.string.arrival_show_route_badges_subtitle),
            icon = Icons.Filled.Route,
            checked = showArrivalRouteBadges,
            onCheckedChange = {
                onShowArrivalRouteBadgesChange(it)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        SettingsSwitchOption(
            label = stringResource(R.string.arrival_show_vehicle_info),
            subtitle = stringResource(R.string.arrival_show_vehicle_info_subtitle),
            icon = Icons.Filled.DirectionsBus,
            checked = showArrivalVehicleInfo,
            onCheckedChange = {
                onShowArrivalVehicleInfoChange(it)
            }
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        SettingsRadioOption(
            label = stringResource(R.string.refresh_cadence_15),
            subtitle = stringResource(R.string.refresh_cadence_subtitle),
            icon = Icons.Filled.Refresh,
            selected = arrivalsRefreshSeconds == 15,
            onClick = {
                onArrivalsRefreshChange(15)
            }
        )
        SettingsRadioOption(
            label = stringResource(R.string.refresh_cadence_30),
            subtitle = stringResource(R.string.refresh_cadence_subtitle),
            icon = Icons.Filled.Refresh,
            selected = arrivalsRefreshSeconds == 30,
            onClick = {
                onArrivalsRefreshChange(30)
            }
        )
        SettingsRadioOption(
            label = stringResource(R.string.refresh_cadence_60),
            subtitle = stringResource(R.string.refresh_cadence_subtitle),
            icon = Icons.Filled.Refresh,
            selected = arrivalsRefreshSeconds == 60,
            onClick = {
                onArrivalsRefreshChange(60)
            }
        )
        SettingsRadioOption(
            label = stringResource(R.string.refresh_cadence_120),
            subtitle = stringResource(R.string.refresh_cadence_subtitle),
            icon = Icons.Filled.Refresh,
            selected = arrivalsRefreshSeconds == 120,
            onClick = {
                onArrivalsRefreshChange(120)
            }
        )
    }}
