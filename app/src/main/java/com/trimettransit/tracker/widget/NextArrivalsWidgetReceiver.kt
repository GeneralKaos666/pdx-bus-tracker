package com.trimettransit.tracker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/** Receives home-screen widget lifecycle callbacks and routes them to [NextArrivalsWidget]. */
class NextArrivalsWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = NextArrivalsWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        // Force a refresh pass when the widget is added/resized so a brand-new widget
        // doesn't sit on a stale snapshot for up to the 30-minute cadence.
        WidgetScheduler.refreshNow(context)
    }
}