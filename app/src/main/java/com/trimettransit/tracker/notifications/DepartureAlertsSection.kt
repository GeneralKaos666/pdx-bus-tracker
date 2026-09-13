package com.trimettransit.tracker.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.trimettransit.tracker.R
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.FavoritesRepositoryImpl
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.components.SettingsCard
import com.trimettransit.tracker.ui.components.SettingsRadioOption
import com.trimettransit.tracker.ui.components.SettingsSwitchOption

private val windowOptions = listOf(5, 10, 15)

/**
 * Departure-alert section rendered inside the app's Settings screen (injected as
 * a slot by [com.trimettransit.tracker.feature.settings.SettingsScreen], the same
 * way the widget settings are). Everything is opt-in: the phase is off by default,
 * and a stop is monitored only when its alert toggle is on.
 */
@Composable
fun DepartureAlertsSection() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(DepartureAlertPrefs.isEnabled(context)) }
    var windowMinutes by remember { mutableIntStateOf(DepartureAlertPrefs.windowMinutes(context)) }
    var favorites by remember { mutableStateOf<List<Stop>>(emptyList()) }
    var alertedIds by remember { mutableStateOf(favoritesAlertedIds(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        favorites = FavoritesRepositoryImpl(DatabaseHelper(context)).getFavorites()
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        DepartureAlertPrefs.setEnabled(context, value)
        if (value) {
            DepartureNotifications.ensureChannel(context)
            if (needsPermissionRequest(context)) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            DepartureAlertScheduler.checkNow(context)
        } else {
            DepartureAlertScheduler.stop(context)
        }
    }

    SettingsCard {
        SettingsSwitchOption(
            label = stringResource(R.string.notifications_master_label),
            subtitle = stringResource(R.string.notifications_master_subtitle),
            icon = Icons.Filled.NotificationsActive,
            checked = enabled,
            onCheckedChange = { setEnabled(it) }
        )

        if (enabled) {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            windowOptions.forEach { minutes ->
                SettingsRadioOption(
                    label = stringResource(R.string.notifications_window_label_min, minutes),
                    subtitle = pluralStringResource(R.plurals.notifications_window_desc, minutes, minutes),
                    icon = Icons.Filled.Schedule,
                    selected = windowMinutes == minutes,
                    onClick = {
                        if (minutes != windowMinutes) {
                            windowMinutes = minutes
                            DepartureAlertPrefs.setWindowMinutes(context, minutes)
                        }
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            Text(
                text = stringResource(R.string.notifications_stops_header),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
            )

            if (favorites.isEmpty()) {
                Text(
                    text = stringResource(R.string.notifications_no_favorites),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            } else {
                favorites.forEach { stop ->
                    SettingsSwitchOption(
                        label = stop.desc,
                        subtitle = stop.dirDesc,
                        icon = Icons.Filled.Place,
                        checked = stop.locId in alertedIds,
                        onCheckedChange = { on ->
                            alertedIds = if (on) alertedIds + stop.locId else alertedIds - stop.locId
                            DepartureAlertPrefs.setStopAlerted(context, stop.locId, on)
                        }
                    )
                }
            }
        }
    }
}

private fun favoritesAlertedIds(context: android.content.Context): Set<Int> {
    val prefs = DepartureAlertPrefs.prefs(context)
    return prefs.all.keys
        .filter { it.startsWith(KEY_STOP_PREFIX) }
        .mapNotNull { prefKey ->
            val locId = prefKey.removePrefix(KEY_STOP_PREFIX).toIntOrNull() ?: return@mapNotNull null
            locId.takeIf { prefs.getBoolean(prefKey, false) }
        }
        .toSet()
}

private const val KEY_STOP_PREFIX = "pref_key_departure_alerts_stop_"

private fun needsPermissionRequest(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED