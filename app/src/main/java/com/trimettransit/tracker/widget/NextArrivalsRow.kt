package com.trimettransit.tracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.trimettransit.tracker.R
import com.trimettransit.tracker.activities.MainActivity
import com.trimettransit.tracker.widget.WidgetSnapshotCache.ArrivalOnScreen
import com.trimettransit.tracker.widget.WidgetSnapshotCache.Row
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun widgetClockTime(): DateTimeFormatter =
    DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
        .withLocale(Locale.getDefault())
        .withZone(ZoneId.systemDefault())

@Composable
internal fun StopRow(row: Row, config: WidgetConfig, now: Long) {
    val context = LocalContext.current
    val action = actionStartActivity(
        Intent(context, MainActivity::class.java)
            .putExtra(WidgetLaunch.EXTRA_STOP_ID, row.stop.locId.toLong())
            .putExtra(WidgetLaunch.EXTRA_STOP_NAME, row.stop.desc)
            .putExtra(WidgetLaunch.EXTRA_ROUTE_ID, row.stop.routeNum)
            .putExtra(WidgetLaunch.EXTRA_LAT, row.stop.latitude)
            .putExtra(WidgetLaunch.EXTRA_LNG, row.stop.longitude)
    )
    if (config.compactRows) {
        CompactRow(row, config, now, action)
    } else {
        DetailRow(row, config, now, action)
    }
}

@Composable
private fun DetailRow(row: Row, config: WidgetConfig, now: Long, action: androidx.glance.action.Action) {
    val c = GlanceTheme.colors
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clickable(action)
    ) {
        RouteBadge(row.stop.routeNum, row.stop.transitType, config.showRouteBadge)
        Spacer(GlanceModifier.width(10.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                text = row.stop.desc,
                style = TextStyle(color = c.onBackground),
                maxLines = 1
            )
            Text(
                text = row.arrivals
                    .take(config.arrivalsPerStop)
                    .joinToString(context.getString(R.string.widget_arrival_separator)) { a ->
                        arrivalTimeLabel(row, a, config, now, context)
                    },
                style = TextStyle(color = c.onBackground),
                maxLines = 1,
                modifier = GlanceModifier.padding(top = 2.dp)
            )
            val detours = lineDetours(row)
            if (config.showDetourAlerts && detours.isNotEmpty()) {
                DetourPill(detours.joinToString(" / ") { it.desc })
            }
        }
    }
}

@Composable
private fun CompactRow(row: Row, config: WidgetConfig, now: Long, action: androidx.glance.action.Action) {
    val c = GlanceTheme.colors
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable(action)
    ) {
        RouteBadge(row.stop.routeNum, row.stop.transitType, config.showRouteBadge)
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = row.stop.desc,
            style = TextStyle(color = c.onBackground),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        Spacer(GlanceModifier.width(8.dp))
        Text(
            text = row.arrivals.take(config.arrivalsPerStop).firstOrNull()?.let {
                arrivalTimeLabel(row, it, config, now, context)
            } ?: "",
            style = TextStyle(color = c.onBackground),
            maxLines = 1
        )
    }
}

@Composable
private fun RouteBadge(routeNum: Int, transitType: String, showBadge: Boolean) {
    if (!showBadge) return
    val c = GlanceTheme.colors
    val (background, label, labelColor) = if (routeNum > 0) {
        Triple(c.primary, routeNum.toString(), c.onPrimary)
    } else {
        when (transitType.uppercase()) {
            "M" -> Triple(c.secondary, "M", c.onSecondary)
            "W" -> Triple(c.outline, "W", c.onPrimary)
            "R" -> Triple(c.tertiary, "R", c.onTertiary)
            else -> Triple(c.primary, "B", c.onPrimary)
        }
    }
    Box(
        modifier = GlanceModifier
            .size(26.dp)
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = TextStyle(color = labelColor, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

@Composable
private fun DetourPill(detail: String) {
    val c = GlanceTheme.colors
    Box(
        modifier = GlanceModifier
            .padding(top = 2.dp)
            .background(c.errorContainer)
    ) {
        Text(
            text = detail,
            style = TextStyle(
                color = c.onErrorContainer,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 1,
            modifier = GlanceModifier.padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}

private fun lineDetours(row: Row): List<WidgetSnapshotCache.WidgetDetour> =
    row.detours.filter { it.routeIds.contains(row.stop.routeNum) }

private fun countdownLabel(minutes: Long, context: Context): String = when {
    minutes <= 0L -> context.getString(R.string.widget_due)
    else -> context.getString(R.string.widget_countdown_min, minutes)
}

private fun arrivalTimeLabel(
    row: Row,
    arrival: ArrivalOnScreen,
    config: WidgetConfig,
    now: Long,
    context: Context
): String {
    val label = when {
        arrival.dropOffOnly -> context.getString(R.string.widget_dropoff_only)
        config.showClockTime -> widgetClockTime().format(Instant.ofEpochMilli(arrival.atMillis))
        else -> countdownLabel(row.minutesFrom(now, arrival), context)
    }
    if (!config.showArrivalStatus) return label
    return when (arrival.status) {
        "canceled" -> "$label ${context.getString(R.string.widget_canceled)}"
        "delayed" -> "$label ${context.getString(R.string.widget_delayed)}"
        else -> label
    }
}