package com.trimettransit.tracker.widget.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.R
import com.trimettransit.tracker.ui.components.pressScale
import com.trimettransit.tracker.ui.theme.LocalCardStyle
import com.trimettransit.tracker.ui.theme.appCardBorder
import com.trimettransit.tracker.widget.NextArrivalsWidgetReceiver
import com.trimettransit.tracker.widget.WidgetScheduler
import com.trimettransit.tracker.widget.config.WidgetConfigActivity

private const val DEFAULT_INTERVAL_MIN = 30

private val refreshIntervals = listOf(15, 30, 45, 60)

/**
 * Widget section rendered inside the app's Settings screen (injected as a slot by
 * [com.trimettransit.tracker.feature.settings.SettingsScreen], so that module never
 * depends on app/widget code). Exposes the global home-screen refresh cadence and the
 * list of placed widget instances, each reopenable for editing.
 */
@Composable
fun WidgetSettingsSection() {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }
    var intervalMin by remember {
        mutableIntStateOf(prefs.getInt(WidgetScheduler.KEY_REFRESH_INTERVAL_MIN, DEFAULT_INTERVAL_MIN))
    }
    val placedWidgetIds = remember { placedWidgetIds(context) }

    SectionHeader(title = stringResource(R.string.widget_settings_title))

    WidgetSettingsCard {
        Text(
            text = stringResource(R.string.widget_settings_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        refreshIntervals.forEach { minutes ->
            WidgetRadioOption(
                label = stringResource(R.string.widget_settings_interval_min, minutes),
                subtitle = pluralStringResource(R.plurals.widget_settings_interval_desc, minutes, minutes),
                icon = Icons.Filled.Schedule,
                selected = intervalMin == minutes,
                onClick = {
                    if (minutes != intervalMin) {
                        intervalMin = minutes
                        prefs.edit { putInt(WidgetScheduler.KEY_REFRESH_INTERVAL_MIN, minutes) }
                        // UPDATE policy re-arms the periodic work with the new cadence.
                        WidgetScheduler.ensureScheduled(context)
                    }
                }
            )
        }
    }

    SectionHeader(title = stringResource(R.string.widget_settings_placed_title))

    WidgetSettingsCard {
        if (placedWidgetIds.isEmpty()) {
            Text(
                text = stringResource(R.string.widget_settings_no_widgets),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        } else {
            placedWidgetIds.forEach { widgetId ->
                WidgetPlacedRow(widgetId = widgetId)
            }
        }
    }
}

/** Widget instance ids currently on the home screen. */
private fun placedWidgetIds(context: Context): List<Int> =
    AppWidgetManager.getInstance(context)
        .getAppWidgetIds(ComponentName(context, NextArrivalsWidgetReceiver::class.java))
        .toList()

/** Reopen the per-widget config for an existing instance (same reconfig path as the launcher). */
private fun openWidgetConfig(context: Context, appWidgetId: Int) {
    context.startActivity(
        Intent(context, WidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
    )
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun WidgetSettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = appCardBorder(),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}

@Composable
private fun WidgetRadioOption(
    label: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconCircle(icon = icon, highlighted = selected)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        RadioButton(
            selected = selected,
            onClick = null
        )
    }
}

@Composable
private fun SettingsIconCircle(icon: ImageVector, highlighted: Boolean) {
    Surface(
        modifier = Modifier.size(40.dp),
        shape = RoundedCornerShape(LocalCardStyle.current.cornerRadius),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer
                       else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun WidgetPlacedRow(widgetId: Int) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.widget_settings_widget_id, widgetId),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = { openWidgetConfig(context, widgetId) }) {
            Text(stringResource(R.string.widget_settings_edit))
        }
    }
}