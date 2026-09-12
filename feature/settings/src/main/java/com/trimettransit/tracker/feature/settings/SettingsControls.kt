package com.trimettransit.tracker.feature.settings

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSliderState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.trimettransit.tracker.ui.components.SettingsIconCircle
import com.trimettransit.tracker.ui.components.SettingsRowOption
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.theme.LocalCardStyle
import com.trimettransit.tracker.ui.theme.appCardShape
import java.util.Locale
import kotlin.math.roundToInt

/** The preview colour of an outline option row, or the scheme's outlineVariant when "auto". */
@Composable
internal fun outlinePreviewColour(raw: String): Color =
    parseOutlineColour(raw, MaterialTheme.colorScheme.outlineVariant)

/** Parses a persisted outline-colour pref into a [Color]; "auto" (or an unparseable value) → [fallback]. */
internal fun parseOutlineColour(raw: String, fallback: Color): Color {
    if (raw == "auto") return fallback
    return runCatching { Color(raw.toColorInt()) }.getOrElse { fallback }
}

@Composable
internal fun LicenseEntry(name: String, license: String, isNote: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = name,
            style = if (isNote) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium,
            color = if (isNote) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            fontStyle = if (isNote) FontStyle.Italic else FontStyle.Normal
        )
        if (license.isNotEmpty()) {
            Text(
                text = license,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SettingsColourOption(
    label: String,
    subtitle: String,
    icon: ImageVector,
    colour: Color,
    onClick: () -> Unit
) {
    SettingsRowOption(
        label = label,
        subtitle = subtitle,
        icon = icon,
        highlighted = false,
        onClick = onClick,
        trailing = {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = appCardShape(),
                color = colour,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {}
        }
    )
}

@Composable
internal fun SettingsSliderOption(
    label: String,
    icon: ImageVector,
    value: Float,
    valueLabel: String,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit
) {
    val sliderState = rememberSliderState(value, 27, valueRange)
    LaunchedEffect(value) { sliderState.value = value }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.trimettransit.tracker.ui.components.SettingsIconCircle(icon = icon, highlighted = false)
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = valueLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            state = sliderState,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 56.dp, end = 8.dp)
        )
    }
}

@Composable
internal fun CardOutlineColourDialog(
    initial: String,
    onDismiss: () -> Unit,
    onAuto: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val fallback = scheme.outlineVariant
    val startColor = parseOutlineColour(initial, fallback)
    val initialHsv = remember(startColor) {
        FloatArray(3).also { AndroidColor.colorToHSV(startColor.toArgb(), it) }
    }
    var isAuto by remember(initial) { mutableStateOf(initial == "auto") }
    var hue by remember(initial) { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember(initial) { mutableFloatStateOf(initialHsv[1]) }
    var value by remember(initial) { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember(initial) { mutableFloatStateOf(startColor.alpha) }

    val draft = remember(hue, sat, value, alpha) {
        Color(AndroidColor.HSVToColor((alpha * 255).roundToInt(), floatArrayOf(hue, sat, value)))
    }

    fun markCustom() {
        isAuto = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.card_outline_colour)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                val autoSource = remember { MutableInteractionSource() }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pressScale(autoSource)
                        .clickable(
                            interactionSource = autoSource,
                            indication = LocalIndication.current
                        ) { isAuto = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.card_outline_auto),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f)
                    )
                    RadioButton(selected = isAuto, onClick = null)
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = appCardShape(),
                    color = draft,
                    border = BorderStroke(1.dp, scheme.outlineVariant)
                ) {}
                Spacer(modifier = Modifier.height(16.dp))
                ColourSlider(
                    label = stringResource(R.string.color_hue),
                    value = hue,
                    valueRange = 0f..360f,
                    steps = 35,
                    onValueChange = {
                        hue = it
                        markCustom()
                    }
                )
                ColourSlider(
                    label = stringResource(R.string.color_saturation),
                    value = sat,
                    onValueChange = {
                        sat = it
                        markCustom()
                    }
                )
                ColourSlider(
                    label = stringResource(R.string.color_value),
                    value = value,
                    onValueChange = {
                        value = it
                        markCustom()
                    }
                )
                ColourSlider(
                    label = stringResource(R.string.color_opacity),
                    value = alpha,
                    onValueChange = {
                        alpha = it
                        markCustom()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isAuto) onAuto() else onConfirm(String.format(Locale.US, "#%08X", draft.toArgb()))
                }
            ) {
                Text(stringResource(R.string.done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
internal fun ColourSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    val sliderState = rememberSliderState(value, steps, valueRange)
    LaunchedEffect(value) { sliderState.value = value }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
            state = sliderState,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onValueChange
        )
    }
}