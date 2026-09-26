package com.trimettransit.tracker.feature.settings

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.components.SectionHeader
import com.trimettransit.tracker.ui.components.SettingsCard
/**
 * Root Settings menu: one row per section with a live summary subtitle.
 * Hosted inside the shell's AnimatedContent; [onNavigate] opens a pane.
 */
@Composable
internal fun ColumnScope.SettingsMenu(
    settings: SettingsPreferenceState,
    notificationsSection: (@Composable ColumnScope.() -> Unit)?,
    notificationsEnabled: Boolean?,
    widgetSection: (@Composable ColumnScope.() -> Unit)?,
    widgetRefreshIntervalMin: Int?,
    onNavigate: (SettingsSection) -> Unit
) {
    SectionHeader(title = stringResource(R.string.settings_title))

    SettingsCard {
        SettingsGroupHeader(stringResource(R.string.settings_group_personalize))
        MenuRow(
            label = stringResource(R.string.section_appearance),
            subtitle = themeSummary(settings.theme),
            icon = Icons.Filled.BrightnessAuto,
            onClick = { onNavigate(SettingsSection.APPEARANCE) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        MenuRow(
            label = stringResource(R.string.section_colours),
            subtitle = coloursSummary(settings.colorMode, settings.amoledDark),
            icon = Icons.Filled.Palette,
            onClick = { onNavigate(SettingsSection.COLOURS) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        MenuRow(
            label = stringResource(R.string.section_display),
            subtitle = displaySummary(settings.density, settings.fontScale),
            icon = Icons.Filled.ViewStream,
            onClick = { onNavigate(SettingsSection.DISPLAY) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        MenuRow(
            label = stringResource(R.string.section_cards),
            subtitle = cardsSummary(settings.cornerStyle, settings.cornerRadius),
            icon = Icons.Filled.BorderAll,
            onClick = { onNavigate(SettingsSection.CARDS) }
        )
        SettingsGroupHeader(stringResource(R.string.settings_group_transit))
        MenuRow(
            label = stringResource(R.string.section_maps),
            subtitle = mapsSummary(settings.mapStyle),
            icon = Icons.Filled.Map,
            onClick = { onNavigate(SettingsSection.MAPS) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        MenuRow(
            label = stringResource(R.string.section_arrivals),
            subtitle = arrivalsSummary(settings.arrivalsRefreshSeconds),
            icon = Icons.Filled.Schedule,
            onClick = { onNavigate(SettingsSection.ARRIVALS) }
        )
        if (notificationsSection != null || widgetSection != null) {
            SettingsGroupHeader(stringResource(R.string.settings_group_app))
        }
        if (notificationsSection != null) {
            MenuRow(
                label = stringResource(R.string.menu_notifications),
                subtitle = notificationsEnabled?.let { on ->
                    if (on) stringResource(R.string.value_on) else stringResource(R.string.value_off)
                }.orEmpty(),
                icon = Icons.Filled.NotificationsActive,
                onClick = { onNavigate(SettingsSection.NOTIFICATIONS) }
            )
        }
        if (widgetSection != null) {
            MenuRow(
                label = stringResource(R.string.menu_widget),
                subtitle = widgetRefreshIntervalMin
                    ?.let { stringResource(R.string.menu_widget_summary, it) }
                    .orEmpty(),
                icon = Icons.Filled.Widgets,
                onClick = { onNavigate(SettingsSection.WIDGET) }
            )
        }
        SettingsGroupHeader(stringResource(R.string.settings_group_app_info))
        MenuRow(
            label = stringResource(R.string.section_about),
            subtitle = stringResource(R.string.about_subtitle),
            icon = Icons.Filled.Info,
            onClick = { onNavigate(SettingsSection.ABOUT) }
        )
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        MenuRow(
            label = stringResource(R.string.open_source_licenses),
            subtitle = stringResource(R.string.libraries_terms),
            icon = Icons.Filled.Info,
            onClick = { onNavigate(SettingsSection.LICENSES) }
        )
    }
}
