package com.trimettransit.tracker.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsRadioOption
/** Display pane: density, font scale, motion, and favorites columns options. */
@Composable
internal fun DisplayPane(
    densityRaw: String,
    onDensityChange: (String) -> Unit,
    fontScaleRaw: String,
    onFontScaleChange: (String) -> Unit,
    motionRaw: String,
    onMotionChange: (String) -> Unit,
    favoritesColumnsRaw: String,
    onFavoritesColumnsChange: (String) -> Unit
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

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                SettingsRadioOption(
                    label = stringResource(R.string.favorites_columns_auto),
                    subtitle = stringResource(R.string.favorites_columns_auto_subtitle),
                    icon = Icons.Filled.ViewModule,
                    selected = favoritesColumnsRaw == "auto",
                    onClick = {
                        onFavoritesColumnsChange("auto")
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.favorites_columns_one),
                    subtitle = stringResource(R.string.favorites_columns_one_subtitle),
                    icon = Icons.Filled.ViewStream,
                    selected = favoritesColumnsRaw == "one",
                    onClick = {
                        onFavoritesColumnsChange("one")
                    }
                )
                SettingsRadioOption(
                    label = stringResource(R.string.favorites_columns_two),
                    subtitle = stringResource(R.string.favorites_columns_two_subtitle),
                    icon = Icons.Filled.ViewColumn,
                    selected = favoritesColumnsRaw == "two",
                    onClick = {
                        onFavoritesColumnsChange("two")
                    }
                )
            }
    }}
