package com.trimettransit.tracker.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback

/**
 * On-widget refresh affordance: enqueues an immediate [WidgetRefreshWorker]
 * pass via [WidgetScheduler]. A worker (not a Service) owns the fetch, so a
 * run-callback is the correct Glance mechanism — not `actionStartService`.
 */
class WidgetRefreshAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        WidgetScheduler.refreshNow(context)
    }
}
