package com.trimettransit.tracker.widget.settings

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.R
import com.trimettransit.tracker.ui.components.SectionHeader
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsIconCircle
import com.trimettransit.tracker.ui.components.SettingsRadioOption
import com.trimettransit.tracker.ui.components.SettingsRowOption
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
    var placedWidgetIds by remember { mutableStateOf(placedWidgetIds(context)) }

    SectionHeader(title = stringResource(R.string.widget_settings_title))

    SettingsCard {
        Text(
            text = stringResource(R.string.widget_settings_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
        )
        refreshIntervals.forEach { minutes ->
            SettingsRadioOption(
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

    SettingsCard {
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