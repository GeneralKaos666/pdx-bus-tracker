package com.trimettransit.tracker.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.trimettransit.tracker.R

/** Owns the departure-alert notification channel. */
object DepartureNotifications {
    const val CHANNEL_ID = "departure_alerts"

    /** Idempotent; call before posting and the first time alerts are enabled. */
    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.departure_alert_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )
        manager.createNotificationChannel(channel)
    }
}