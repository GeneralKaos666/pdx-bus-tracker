package com.trimettransit.tracker.feature.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import com.trimettransit.tracker.ui.appearance.MapStyles
import com.trimettransit.tracker.ui.appearance.colorToSpec
import com.trimettransit.tracker.ui.appearance.parseColorSpec
import com.trimettransit.tracker.ui.components.SectionHeader
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsCornerOption
import com.trimettransit.tracker.ui.components.SettingsIconCircle
import com.trimettransit.tracker.ui.components.SettingsRadioOption
import com.trimettransit.tracker.ui.components.SettingsSwitchOption
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.components.transitColor
import com.trimettransit.tracker.ui.theme.TrimetBlue
import com.trimettransit.tracker.ui.theme.appCardShape
import com.trimettransit.tracker.ui.theme.m3ContentExpand
import com.trimettransit.tracker.ui.theme.m3ContentShrink
import kotlin.math.roundToInt
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

/** Display pane: density, font scale, and motion options. */
@Composable
internal fun DisplayPane(
    densityRaw: String,
    onDensityChange: (String) -> Unit,
    fontScaleRaw: String,
    onFontScaleChange: (String) -> Unit,
    motionRaw: String,
    onMotionChange: (String) -> Unit
)
{
    SettingsCard {
            Column {
                SettingsRadioOption(
                    label = stringResource(R.string.density_comfortable),
                    subtitle = stringResource(R.string.density_comfortable_subtitle),
                    icon = Icons.Filled.ViewStream,
                    selected = densityRaw == "comfortable",
                    onClick = {
                        onDensityChange("comfortable")
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.density_compact),
                    subtitle = stringResource(R.string.density_compact_subtitle),
                    icon = Icons.Filled.ViewAgenda,
                    selected = densityRaw == "compact",
                    onClick = {
                        onDensityChange("compact")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                SettingsRadioOption(
                    label = stringResource(R.string.font_smaller),
                    subtitle = stringResource(R.string.font_smaller_subtitle),
                    icon = Icons.Filled.TextDecrease,
                    selected = fontScaleRaw == "smaller",
                    onClick = {
                        onFontScaleChange("smaller")
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.font_default),
                    subtitle = stringResource(R.string.font_default_subtitle),
                    icon = Icons.Filled.FormatSize,
                    selected = fontScaleRaw == "default",
                    onClick = {
                        onFontScaleChange("default")
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.font_larger),
                    subtitle = stringResource(R.string.font_larger_subtitle),
                    icon = Icons.Filled.TextIncrease,
                    selected = fontScaleRaw == "larger",
                    onClick = {
                        onFontScaleChange("larger")
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                SettingsRadioOption(
                    label = stringResource(R.string.motion_expressive),
                    subtitle = stringResource(R.string.motion_expressive_subtitle),
                    icon = Icons.Filled.MotionPhotosOn,
                    selected = motionRaw == "expressive",
                    onClick = {
                        onMotionChange("expressive")
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.motion_default),
                    subtitle = stringResource(R.string.motion_default_subtitle),
                    icon = Icons.Filled.MotionPhotosOn,
                    selected = motionRaw == "default",
                    onClick = {
                        onMotionChange("default")
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.motion_low),
                    subtitle = stringResource(R.string.motion_low_subtitle),
                    icon = Icons.Filled.MotionPhotosOn,
                    selected = motionRaw == "low",
                    onClick = {
                        onMotionChange("low")
                    }
                )
            }
    }}

/** Cards pane: outlines, corner style, and corner radius. */
@Composable
internal fun CardsPane(
    cardOutlines: Boolean,
    onCardOutlinesChange: (Boolean) -> Unit,
    cardOutlineColorRaw: String,
    cornerRadius: Float,
    onCornerRadiusChange: (Float) -> Unit,
    onCornerRadiusFinished: () -> Unit,
    cornerStyle: String,
    onCornerStyleChange: (String) -> Unit,
    onPickColour: (ColourTarget) -> Unit
)
{
    SettingsCard {
            Column {
                SettingsSwitchOption(
                    label = stringResource(R.string.card_outlines),
                    subtitle = stringResource(R.string.card_outlines_subtitle),
                    icon = Icons.Filled.BorderAll,
                    checked = cardOutlines,
                    onCheckedChange = {
                        onCardOutlinesChange(it)
                    }
                )
                AnimatedVisibility(
                    visible = cardOutlines,
                    enter = m3ContentExpand(),
                    exit = m3ContentShrink()
                ) {
                    SettingsColourOption(
                        label = stringResource(R.string.card_outline_colour),
                        subtitle = stringResource(R.string.card_outline_colour_subtitle),
                        icon = Icons.Filled.Colorize,
                        colour = outlinePreviewColour(cardOutlineColorRaw),
                        onClick = { onPickColour(ColourTarget.CARD_OUTLINE) }
                    )
                }
                SettingsCornerOption(
                    label = stringResource(R.string.corner_style_rounded),
                    subtitle = stringResource(R.string.corner_style_rounded_subtitle),
                    cut = false,
                    selected = cornerStyle == "rounded",
                    onClick = {
                        onCornerStyleChange("rounded")
                    }
                )
                SettingsCornerOption(
                    label = stringResource(R.string.corner_style_cut),
                    subtitle = stringResource(R.string.corner_style_cut_subtitle),
                    cut = true,
                    selected = cornerStyle == "cut",
                    onClick = {
                        onCornerStyleChange("cut")
                    }
                )
                SettingsSliderOption(
                    label = stringResource(R.string.card_corner_radius),
                    icon = Icons.Filled.Tune,
                    value = cornerRadius,
                    valueLabel = stringResource(R.string.card_corner_radius_dp, cornerRadius.roundToInt()),
                    valueRange = 0f..28f,
                    onValueChange = onCornerRadiusChange,
                    onValueChangeFinished = onCornerRadiusFinished
                )
            }
    }}

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

/** About pane: app version, disclaimers, attribution, and the privacy-policy row. */
@Composable
internal fun AboutPane()
{
    val context = LocalContext.current
    SettingsCard {
        val appIcon = remember {
            context.packageManager.getApplicationIcon(context.packageName)
                .toBitmap().asImageBitmap()
        }
        val versionName = remember {
            runCatching {
                val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
                } else {
                    context.packageManager.getPackageInfo(context.packageName, 0)
                }
                info.versionName
            }.getOrNull() ?: "0.0.0"
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = appCardShape(),
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_title),
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = stringResource(R.string.version_license, versionName),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Text(
            text = stringResource(R.string.unofficial_disclaimer),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
        )
        Text(
            text = stringResource(R.string.data_provider),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 4.dp)
        )
        Text(
            text = stringResource(R.string.trademark_notice),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

        val policyInteractionSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(policyInteractionSource)
                .clickable(
                    interactionSource = policyInteractionSource,
                    indication = LocalIndication.current
                ) {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        "https://github.com/GeneralKaos666/pdx-bus-tracker/blob/master/docs/privacy-policy.md".toUri()
                    )
                    context.startActivity(intent)
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconCircle(icon = Icons.Filled.Info, highlighted = false)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.privacy_policy),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Licenses pane: static third-party license entries. */
@Composable
internal fun LicensesPane()
{
    SettingsCard {
            Column(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            ) {
                LicenseEntry(stringResource(R.string.license_androidx), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_kotlin), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_okhttp), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_maplibre), stringResource(R.string.license_bsd_2))
                LicenseEntry(stringResource(R.string.license_joda), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_timber), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_glance), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_workmanager), stringResource(R.string.license_apache_2))
                LicenseEntry(stringResource(R.string.license_materialkolor), stringResource(R.string.license_mit))
                LicenseEntry(stringResource(R.string.license_bricolage), stringResource(R.string.license_ofl))
                LicenseEntry(
                    stringResource(R.string.license_map_data),
                    "",
                    isNote = true
                )
                LicenseEntry(
                    stringResource(R.string.license_full_texts),
                    "",
                    isNote = true
                )
            }
    }
}

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

/**
 * Colour-picker overlay shared by the Colours and Cards panes.
 * A blank raw means "follow scheme". [onPick] writes state+prefs; the shell
 * clears the target via [onDismiss].
 */
@Composable
internal fun ColourPickerHost(
    target: ColourTarget,
    cardOutlineColorRaw: String,
    accentColorRaw: String,
    pillAccentRaw: String,
    transitBusRaw: String,
    transitRailRaw: String,
    transitStreetcarRaw: String,
    transitWesRaw: String,
    onDismiss: () -> Unit,
    onPick: (raw: String) -> Unit
) {
    val title = when (target) {
        ColourTarget.CARD_OUTLINE -> stringResource(R.string.card_outline_colour)
        ColourTarget.ACCENT -> stringResource(R.string.accent_colour)
        ColourTarget.PILL_ACCENT -> stringResource(R.string.pill_accent)
        ColourTarget.TRANSIT_BUS -> stringResource(R.string.transit_bus)
        ColourTarget.TRANSIT_RAIL -> stringResource(R.string.transit_rail)
        ColourTarget.TRANSIT_STREETCAR -> stringResource(R.string.transit_streetcar)
        ColourTarget.TRANSIT_WES -> stringResource(R.string.transit_wes)
    }
    val initial = when (target) {
        ColourTarget.CARD_OUTLINE -> cardOutlineColorRaw
        ColourTarget.ACCENT -> accentColorRaw
        ColourTarget.PILL_ACCENT -> pillAccentRaw
        ColourTarget.TRANSIT_BUS -> transitBusRaw
        ColourTarget.TRANSIT_RAIL -> transitRailRaw
        ColourTarget.TRANSIT_STREETCAR -> transitStreetcarRaw
        ColourTarget.TRANSIT_WES -> transitWesRaw
    }
    ColourPickerDialog(
        title = title,
        autoLabel = stringResource(R.string.follow_scheme),
        initialArgb = initial.takeUnless { it.isBlank() },
        onDismiss = onDismiss,
        onAuto = { onPick(""); onDismiss() },
        onConfirm = { argb -> onPick(argb); onDismiss() }
    )
}
