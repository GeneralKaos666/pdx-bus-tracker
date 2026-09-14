package com.trimettransit.tracker.feature.settings

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.trimettransit.tracker.ui.appearance.MapStyles
import com.trimettransit.tracker.ui.components.SettingsIconCircle
import com.trimettransit.tracker.ui.components.SettingsRowOption
import com.trimettransit.tracker.ui.components.pressScale
import kotlin.math.roundToInt

/** One tappable category row on the root Settings menu, with a chevron filler. */
@Composable
internal fun MenuRow(
    label: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    SettingsRowOption(
        label = label,
        subtitle = subtitle,
        icon = icon,
        highlighted = false,
        onClick = onClick,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

/** Top-of-pane header: back arrow + title, plus an optional intro line. Tapping the header row returns to the Settings menu. */
@Composable
internal fun SubmenuHeader(title: String, subtitle: String? = null, onBack: () -> Unit) {
    Column {
        val interactionSource = remember { MutableInteractionSource() }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressScale(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onBack
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SettingsIconCircle(icon = Icons.AutoMirrored.Filled.ArrowBack, highlighted = false)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 60.dp, end = 16.dp, bottom = 8.dp)
            )
        }
    }
}

@Composable
internal fun themeSummary(selectedTheme: String): String =
    when (selectedTheme) {
        "system" -> stringResource(R.string.theme_system)
        "light" -> stringResource(R.string.theme_light)
        else -> stringResource(R.string.theme_dark)
    }

@Composable
internal fun coloursSummary(accentMode: String, amoledDark: Boolean): String {
    val mode = when (accentMode) {
        "dynamic" -> stringResource(R.string.colour_mode_dynamic)
        else -> stringResource(R.string.colour_mode_seed)
    }
    return if (amoledDark) {
        mode + " · " + stringResource(R.string.amoled_dark)
    } else {
        mode
    }
}

@Composable
internal fun displaySummary(densityRaw: String, fontScaleRaw: String): String {
    val density = when (densityRaw) {
        "compact" -> stringResource(R.string.density_compact)
        else -> stringResource(R.string.density_comfortable)
    }
    val font = when (fontScaleRaw) {
        "smaller" -> stringResource(R.string.font_smaller)
        "larger" -> stringResource(R.string.font_larger)
        else -> stringResource(R.string.font_default)
    }
    return density + " · " + font
}

@Composable
internal fun cardsSummary(cornerStyle: String, cornerRadius: Float): String {
    val style = when (cornerStyle) {
        "cut" -> stringResource(R.string.corner_style_cut)
        else -> stringResource(R.string.corner_style_rounded)
    }
    return style + " · " + stringResource(R.string.card_corner_radius_dp, cornerRadius.roundToInt())
}

@Composable
internal fun mapsSummary(mapStyleRaw: String): String =
    when (mapStyleRaw) {
        MapStyles.BRIGHT -> stringResource(R.string.map_style_bright)
        MapStyles.POSITRON -> stringResource(R.string.map_style_positron)
        MapStyles.DARK -> stringResource(R.string.map_style_dark)
        else -> stringResource(R.string.map_style_streets)
    }

@Composable
internal fun arrivalsSummary(refreshSeconds: Int): String =
    when (refreshSeconds) {
        15 -> stringResource(R.string.refresh_cadence_15)
        60 -> stringResource(R.string.refresh_cadence_60)
        120 -> stringResource(R.string.refresh_cadence_120)
        else -> stringResource(R.string.refresh_cadence_30)
    }