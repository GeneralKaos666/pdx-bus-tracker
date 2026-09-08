package com.trimettransit.tracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
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
    if (config.compactRows) {
        CompactRow(row, config, now)
    } else {
        DetailRow(row, config, now)
    }
}

@Composable
private fun DetailRow(row: Row, config: WidgetConfig, now: Long) {
    val c = GlanceTheme.colors
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        RouteBadge(row.stop.routeNum)
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
        }
    }
}

@Composable
private fun CompactRow(row: Row, config: WidgetConfig, now: Long) {
    val c = GlanceTheme.colors
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        RouteBadge(row.stop.routeNum)
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
private fun RouteBadge(routeNum: Int) {
    val c = GlanceTheme.colors
    Box(
        modifier = GlanceModifier
            .size(26.dp)
            .background(c.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = routeBadgeText(routeNum),
            style = TextStyle(color = c.onPrimary, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
    }
}

private fun routeBadgeText(routeNum: Int): String = if (routeNum > 0) routeNum.toString() else "B"

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
): String = when {
    arrival.dropOffOnly -> context.getString(R.string.widget_dropoff_only)
    config.showClockTime -> widgetClockTime().format(Instant.ofEpochMilli(arrival.atMillis))
    else -> countdownLabel(row.minutesFrom(now, arrival), context)
}