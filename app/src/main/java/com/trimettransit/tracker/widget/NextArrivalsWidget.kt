package com.trimettransit.tracker.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider as GlanceColorProvider
import androidx.preference.PreferenceManager
import com.trimettransit.tracker.R
import com.trimettransit.tracker.activities.MainActivity
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.ui.appearance.AppearanceStyle
import com.trimettransit.tracker.ui.appearance.readAppearanceStyle
import com.trimettransit.tracker.widget.WidgetSnapshotCache.Snapshot

class NextArrivalsWidget : GlanceAppWidget() {

    /**
     * Responsive doubles as [androidx.glance.appwidget.PreviewSizeMode], so the
     * same sizes feed the Android 15+ generated picker previews. Exact would be
     * lighter per size but is excluded from preview generation.
     */
    override val sizeMode: SizeMode = SizeMode.Responsive(
        setOf(
            DpSize(180.dp, 110.dp),
            DpSize(250.dp, 180.dp),
            DpSize(250.dp, 320.dp),
            DpSize(400.dp, 320.dp)
        )
    )

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = WidgetSnapshotCache.snapshot(context)
        val appearance = readAppearanceStyle(
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        val isSystemDark =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        provideContent {
            val config = WidgetConfig.fromPersistentMap(currentState<Preferences>().toConfigMap())
            Content(snapshot, config, appearance, isSystemDark, preview = false)
        }
    }

    /**
     * Picker preview composition: real cached arrivals when present, static
     * sample rows otherwise so the picker never shows a blank tile. Single
     * composition, no recomposition or effects — data is passed in.
     */
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val cached = WidgetSnapshotCache.snapshot(context)
        val snapshot = cached.takeIf { it.rows.isNotEmpty() }
            ?: samplePreviewSnapshot(System.currentTimeMillis())
        val appearance = readAppearanceStyle(
            PreferenceManager.getDefaultSharedPreferences(context)
        )
        val isSystemDark =
            (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        provideContent {
            Content(snapshot, WidgetConfig(), appearance, isSystemDark, preview = true)
        }
    }
}

/**
 * Static picker fallback: two sample rows with future arrivals. Pure and
 * Context-free so unit tests pin the shape.
 */
fun samplePreviewSnapshot(nowMillis: Long): Snapshot {
    fun row(name: String, route: Int, vararg mins: Long) = WidgetSnapshotCache.Row(
        stop = Stop(
            desc = name,
            dirDesc = "",
            latitude = 0.0,
            longitude = 0.0,
            transitType = "bus",
            locId = route,
            routeNum = route
        ),
        arrivals = mins.map { min ->
            WidgetSnapshotCache.ArrivalOnScreen(
                sign = "$route-Downtown",
                atMillis = nowMillis + min * 60_000L
            )
        }
    )
    return Snapshot(
        rows = listOf(row("SW 5th & Stark", 12, 3, 11), row("Powell & 82nd", 9, 6, 18)),
        hasFavorites = true,
        updatedAtMillis = nowMillis
    )
}

@Composable
private fun Content(
    snapshot: Snapshot,
    config: WidgetConfig,
    appearance: AppearanceStyle,
    isSystemDark: Boolean,
    preview: Boolean
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val layout = layoutForSize(size.width.value.toInt(), size.height.value.toInt())
    GlanceTheme(widgetColorProviders(config.theme, appearance, isSystemDark)) {
        val c = GlanceTheme.colors
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(widgetBackgroundProvider(config, c.background, context))
                .padding(12.dp)
        ) {
            if (layout != WidgetLayout.COMPACT && !config.hideTitle) {
                HeaderRow(
                    title = config.titleText?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.next_arrivals_widget_label),
                    preview = preview
                )
            }
            when {
                snapshot.rows.isNotEmpty() -> StopList(snapshot, config, layout)
                !snapshot.hasFavorites && snapshot.updatedAtMillis == 0L -> EmptyState(
                    hint = context.getString(R.string.widget_empty_no_favorites),
                    ctx = context
                )
                else -> EmptyState(
                    hint = context.getString(R.string.widget_empty_refreshing),
                    ctx = context
                )
            }
            // Freshness label, same under-a-minute rule as Settings. Hidden in the
            // compact bucket and the empty states, where there is no room for it.
            if (layout != WidgetLayout.COMPACT && snapshot.rows.isNotEmpty()) {
                Text(
                    text = snapshot.ageText(
                        System.currentTimeMillis(),
                        context.getString(R.string.widget_updated_minutes),
                        context.getString(R.string.widget_updated_now)
                    ) ?: context.getString(R.string.widget_never_updated),
                    style = TextStyle(color = c.onBackground, fontSize = 12.sp),
                    maxLines = 1,
                    modifier = GlanceModifier.fillMaxWidth().padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun HeaderRow(title: String, preview: Boolean) {
    val c = GlanceTheme.colors
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = TextStyle(fontWeight = FontWeight.Bold, color = c.onBackground),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight()
        )
        if (!preview) {
            Text(
                text = context.getString(R.string.widget_refresh),
                style = TextStyle(color = c.onBackground),
                maxLines = 1,
                modifier = GlanceModifier
                    .padding(start = 8.dp)
                    .clickable(actionRunCallback<WidgetRefreshAction>())
            )
        }
    }
}

@Composable
private fun ColumnScope.StopList(snapshot: Snapshot, config: WidgetConfig, layout: WidgetLayout) {
    val now = System.currentTimeMillis()
    val rows = applyRowConfig(snapshot.rows, config)
        .let { if (layout == WidgetLayout.COMPACT) it.take(1) else it }
    val outlineColor = widgetOutlineProvider(config, GlanceTheme.colors.outline)
    // Weight (not fillMaxSize) leaves room for the freshness footer below.
    // Rounded clip so scrolling content respects the launcher's widget shape.
    Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight().cornerRadius(8.dp)) {
        if (columnsForLayout(layout) == 1) {
            LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
                itemsIndexed(rows, { index, row -> (row.stop.locId.toLong() shl 32) xor index.toLong() }) { index, row ->
                    StopRow(row, config, now)
                    if (config.showGridDividers && index < rows.lastIndex) {
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .height(1.dp)
                                .background(outlineColor)
                        ) {}
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                gridCells = GridCells.Fixed(2),
                modifier = GlanceModifier.fillMaxSize()
            ) {
                items(rows, { row -> row.stop.locId.toLong() }) { row ->
                    if (config.showGridDividers) {
                        // No border modifier in Glance: simulate an outlined card with a
                        // nested background — the outer outline color shows through the
                        // inner padding as a border around each grid cell.
                        Box(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .padding(6.dp)
                                .background(outlineColor)
                                .cornerRadius(8.dp)
                        ) {
                            Box(modifier = GlanceModifier.fillMaxWidth().padding(8.dp)) {
                                StopRow(row, config, now, inGrid = true)
                            }
                        }
                    } else {
                        StopRow(row, config, now, inGrid = true)
                    }
                }
            }
        }
    }
}

/**
 * Resolves the widget background fill from the per-widget background settings:
 * a custom hex color (with the opacity slider applied), otherwise the theme
 * background with the opacity slider applied. Fully transparent opacity skips
 * the fill so the launcher wallpaper shows through.
 */
internal fun widgetBackgroundProvider(
    config: WidgetConfig,
    systemBackground: GlanceColorProvider,
    context: Context
): GlanceColorProvider {
    val alpha = backgroundOpacityFraction(config.backgroundOpacity)
    val customArgb = if (config.backgroundMode == WidgetBackgroundMode.CUSTOM) {
        parseWidgetBackgroundArgb(config.customBackgroundHex)
    } else {
        null
    }
    if (customArgb != null) {
        val base = Color(customArgb)
        if (alpha <= 0.001f) return androidx.glance.unit.ColorProvider(Color.Transparent)
        return androidx.glance.unit.ColorProvider(base.copy(alpha = base.alpha * alpha))
    }
    if (alpha >= 0.999f) return systemBackground
    if (alpha <= 0.001f) return androidx.glance.unit.ColorProvider(Color.Transparent)
    val resolved = systemBackground.getColor(context)
    return androidx.glance.unit.ColorProvider(resolved.copy(alpha = resolved.alpha * alpha))
}

/**
 * Resolves the outline/divider color from the per-widget outline settings: a
 * custom hex color when the outline mode is custom and parses, otherwise the
 * theme outline. Drives both the two-column card outlines and the
 * single-column divider lines.
 */
internal fun widgetOutlineProvider(
    config: WidgetConfig,
    systemOutline: GlanceColorProvider
): GlanceColorProvider {
    val customArgb = if (config.outlineMode == WidgetBackgroundMode.CUSTOM) {
        parseWidgetBackgroundArgb(config.customOutlineHex)
    } else {
        null
    }
    return if (customArgb != null) {
        androidx.glance.unit.ColorProvider(Color(customArgb))
    } else {
        systemOutline
    }
}

/** Orders rows by [WidgetConfig.selectedStopIds] and caps at [WidgetConfig.maxStops]. */
internal fun applyRowConfig(
    rows: List<WidgetSnapshotCache.Row>,
    config: WidgetConfig
): List<WidgetSnapshotCache.Row> {
    val ordered = if (config.selectedStopIds.isEmpty()) {
        rows
    } else {
        val byId = rows.associateBy { it.stop.locId.toString() }
        config.selectedStopIds.mapNotNull { byId[it] }
    }
    return ordered.take(config.maxStops)
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

internal fun Preferences.toConfigMap(): Map<String, String> = buildMap {
    val knownNames = setOf(
        WidgetConfig.KEY_STOP_IDS,
        WidgetConfig.KEY_ARRIVALS_PER_STOP,
        WidgetConfig.KEY_SHOW_CLOCK_TIME,
        WidgetConfig.KEY_THEME,
        WidgetConfig.KEY_COMPACT_ROWS,
        WidgetConfig.KEY_TITLE_TEXT,
        WidgetConfig.KEY_HIDE_TITLE,
        WidgetConfig.KEY_SHOW_ROUTE_BADGE,
        WidgetConfig.KEY_SHOW_DETOUR_ALERTS,
        WidgetConfig.KEY_SHOW_ARRIVAL_STATUS,
        WidgetConfig.KEY_MAX_STOPS,
        WidgetConfig.KEY_BACKGROUND_MODE,
        WidgetConfig.KEY_CUSTOM_BACKGROUND,
        WidgetConfig.KEY_BACKGROUND_OPACITY,
        WidgetConfig.KEY_SHOW_GRID_DIVIDERS,
        WidgetConfig.KEY_OUTLINE_MODE,
        WidgetConfig.KEY_CUSTOM_OUTLINE
    )
    asMap().forEach { (key, value) ->
        if (key.name in knownNames) put(key.name, value.toString())
    }
}
