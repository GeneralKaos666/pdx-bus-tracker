package com.trimettransit.tracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.GlanceId
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.DynamicThemeColorProviders
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.trimettransit.tracker.R
import com.trimettransit.tracker.activities.MainActivity

class NextArrivalsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache.snapshot(context)
        provideContent {
            Content(snapshot)
        }
    }
}

@Composable
private fun Content(snapshot: WidgetSnapshotCache.Snapshot) {
    val context = LocalContext.current
    GlanceTheme(DynamicThemeColorProviders) {
        val c = GlanceTheme.colors
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(c.background)
                .padding(12.dp)
        ) {
            Text(
                text = context.getString(R.string.next_arrivals_widget_label),
                style = TextStyle(fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.padding(bottom = 6.dp)
            )
            when {
                snapshot.rows.isNotEmpty() -> StopList(snapshot)
                !snapshot.hasFavorites && snapshot.updatedAtMillis == 0L -> EmptyState(
                    hint = context.getString(R.string.widget_empty_no_favorites),
                    ctx = context
                )
                else -> EmptyState(
                    hint = context.getString(R.string.widget_empty_refreshing),
                    ctx = context
                )
            }
        }
    }
}

@Composable
private fun StopList(snapshot: WidgetSnapshotCache.Snapshot) {
    val now = System.currentTimeMillis()
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        itemsIndexed(snapshot.rows) { _, row ->
            StopRow(row = row, now = now)
        }
    }
}

@Composable
private fun StopRow(row: WidgetSnapshotCache.Row, now: Long) {
    val c = GlanceTheme.colors
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
    ) {
        Box(
            modifier = GlanceModifier
                .size(26.dp)
                .background(c.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = routeBadgeText(row.stop.routeNum),
                style = TextStyle(color = c.onPrimary, fontWeight = FontWeight.Bold),
                maxLines = 1
            )
        }
        Spacer(GlanceModifier.width(10.dp))
        Column(GlanceModifier.defaultWeight()) {
            Text(
                text = row.stop.desc,
                style = TextStyle(color = c.onBackground),
                maxLines = 1
            )
            Text(
                text = row.arrivals.joinToString("  \u00b7  ") { a ->
                    countdownLabel(row.minutesFrom(now, a))
                },
                style = TextStyle(color = c.onBackground),
                maxLines = 1,
                modifier = GlanceModifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun EmptyState(hint: String, ctx: Context) {
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(Intent(ctx, MainActivity::class.java))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = hint,
            style = TextStyle(color = GlanceTheme.colors.onBackground),
            maxLines = 2,
            modifier = GlanceModifier.padding(horizontal = 4.dp)
        )
    }
}

private fun routeBadgeText(routeNum: Int): String = if (routeNum > 0) routeNum.toString() else "B"

private fun countdownLabel(minutes: Long): String = when {
    minutes <= 0L -> "Due"
    minutes == 1L -> "1 min"
    else -> "$minutes min"
}
