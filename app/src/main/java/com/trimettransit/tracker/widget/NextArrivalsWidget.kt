package com.trimettransit.tracker.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.trimettransit.tracker.R
import com.trimettransit.tracker.activities.MainActivity
import com.trimettransit.tracker.widget.WidgetSnapshotCache.Snapshot

class NextArrivalsWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache.snapshot(context)
        provideContent {
            val config = WidgetConfig.fromPersistentMap(currentState<Preferences>().toConfigMap())
            Content(snapshot, config)
        }
    }
}

@Composable
private fun Content(snapshot: Snapshot, config: WidgetConfig) {
    val context = LocalContext.current
    GlanceTheme(widgetColorProviders(config.theme)) {
        val c = GlanceTheme.colors
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(c.background)
                .padding(12.dp)
        ) {
            if (!config.hideTitle) {
                Text(
                    text = config.titleText?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.next_arrivals_widget_label),
                    style = TextStyle(fontWeight = FontWeight.Bold),
                    modifier = GlanceModifier.padding(bottom = 6.dp)
                )
            }
            when {
                snapshot.rows.isNotEmpty() -> StopList(snapshot, config)
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
private fun StopList(snapshot: Snapshot, config: WidgetConfig) {
    val now = System.currentTimeMillis()
    val rows = orderedRows(snapshot.rows, config.selectedStopIds)
    LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
        itemsIndexed(rows) { _, row ->
            StopRow(row, config, now)
        }
    }
}

private fun orderedRows(
    rows: List<WidgetSnapshotCache.Row>,
    selectedStopIds: List<String>
): List<WidgetSnapshotCache.Row> =
    if (selectedStopIds.isEmpty()) {
        rows
    } else {
        val byId = rows.associateBy { it.stop.locId.toString() }
        selectedStopIds.mapNotNull { byId[it] }
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

private fun Preferences.toConfigMap(): Map<String, String> = buildMap {
    val knownNames = setOf(
        WidgetConfig.KEY_STOP_IDS,
        WidgetConfig.KEY_ARRIVALS_PER_STOP,
        WidgetConfig.KEY_SHOW_CLOCK_TIME,
        WidgetConfig.KEY_THEME,
        WidgetConfig.KEY_COMPACT_ROWS,
        WidgetConfig.KEY_TITLE_TEXT,
        WidgetConfig.KEY_HIDE_TITLE
    )
    asMap().forEach { (key, value) ->
        if (key.name in knownNames) put(key.name, value.toString())
    }
}