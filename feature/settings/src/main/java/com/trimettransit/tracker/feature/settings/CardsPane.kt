package com.trimettransit.tracker.feature.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsCornerOption
import com.trimettransit.tracker.ui.components.SettingsSwitchOption
import com.trimettransit.tracker.ui.theme.m3ContentExpand
import com.trimettransit.tracker.ui.theme.m3ContentShrink
import kotlin.math.roundToInt
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
