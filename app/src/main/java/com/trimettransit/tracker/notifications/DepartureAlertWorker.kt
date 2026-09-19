package com.trimettransit.tracker.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.trimettransit.tracker.R
import com.trimettransit.tracker.activities.MainActivity
import com.trimettransit.tracker.repos
import com.trimettransit.tracker.retryFetch
import com.trimettransit.tracker.model.Arrival
import com.trimettransit.tracker.model.ArrivalsResult
import com.trimettransit.tracker.model.Stop
import com.trimettransit.tracker.widget.WidgetLaunch
import com.trimettransit.tracker.widget.selectMine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Checks monitored favorites for departures inside the alert window and posts a
 * local notification for each new one (deduped by (stop, trip)). The scheduler
 * re-enqueues the chain every run, keeping the cadence near five minutes — far
 * below PeriodicWorkRequest's 15-minute floor.
 */
class DepartureAlertWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        // Keep the chain alive regardless of what this run decided to do.
        DepartureAlertScheduler.ensureScheduled(app)
        if (!DepartureAlertPrefs.isEnabled(app)) return Result.success()
        if (!NotificationManagerCompat.from(app).areNotificationsEnabled()) return Result.success()
        val now = System.currentTimeMillis()
        if (DepartureAlertRules.isQuietHours(now)) return Result.success()
        return withContext(Dispatchers.IO) {
            val favoritesRepository = app.repos().favorites
            val transitRepository = app.repos().transit
            if (!transitRepository.isConfigured()) return@withContext Result.success()
            val stops = DepartureAlertPrefs.monitoredStops(app, favoritesRepository.getFavorites())
            if (stops.isEmpty()) return@withContext Result.success()

            val windowMinutes = DepartureAlertPrefs.windowMinutes(app)
            val fired = DepartureAlertPrefs.getFired(app)
            // One batched request for all monitored stops instead of a sequential
            // per-stop fetch: arrivals carry their stop's locid for client-side split.
            val result = retryFetch(attempts = MAX_ATTEMPTS, label = "Departure") {
                transitRepository.getArrivals(
                    locIds = stops.map { it.locId },
                    minutes = windowMinutes + SLACK_MINUTES,
                    maxArrivals = MAX_ARRIVALS * stops.size
                )
            }
            if (result == null) {
                Timber.w("Departure check failed for %d stops", stops.size)
                return@withContext Result.success()
            }
            val requestedIds = stops.map { it.locId }.toSet()
            for (stop in stops) {
                checkStop(app, result, stop, now, windowMinutes, fired, requestedIds)
            }
            DepartureAlertPrefs.setFired(app, DepartureAlertRules.prune(fired))
            Result.success()
        }
    }

    private fun checkStop(
        app: Context,
        result: ArrivalsResult,
        stop: Stop,
        now: Long,
        windowMinutes: Int,
        fired: MutableSet<String>,
        requestedIds: Set<Int>
    ) {
        // Prefer locid-attributed arrivals; fall back to the full list only when
        // the backend omits locid so a missing field never silences every alert,
        // while a genuinely empty stop stays silent instead of inheriting buses.
        val mine = selectMine(result.arrivals, stop.locId, requestedIds)
        val pending = DepartureAlertRules.filterNew(
            DepartureAlertRules.actionableArrivals(mine, now, windowMinutes),
            fired,
            stop.locId
        )
        for (arrival in pending) {
            post(app, stop, arrival)
            fired.add(DepartureAlertRules.firedKey(stop.locId, arrival.tripID))
        }
    }

    private fun post(app: Context, stop: Stop, arrival: Arrival) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        DepartureNotifications.ensureChannel(app)
        val minutes = DepartureAlertRules.minutesUntil(arrival, System.currentTimeMillis()) ?: return
        val title = arrival.fullSign.ifBlank { app.getString(R.string.departure_alert_default_title) }
        val body = if (minutes <= 0L) {
            app.getString(R.string.departure_alert_now, stop.desc)
        } else {
            app.getString(R.string.departure_alert_minutes, stop.desc, minutes)
        }
        val intent = Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(WidgetLaunch.EXTRA_STOP_ID, stop.locId.toLong())
            .putExtra(WidgetLaunch.EXTRA_STOP_NAME, stop.desc)
            .putExtra(WidgetLaunch.EXTRA_ROUTE_ID, stop.routeNum)
            .putExtra(WidgetLaunch.EXTRA_LAT, stop.latitude)
            .putExtra(WidgetLaunch.EXTRA_LNG, stop.longitude)
        val contentIntent = PendingIntent.getActivity(
            app,
            stop.locId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(app, DepartureNotifications.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .build()
        NotificationManagerCompat.from(app).notify(stop.locId, notification)
    }

    companion object {
        const val SLACK_MINUTES = 5
        const val MAX_ARRIVALS = 4
        const val MAX_ATTEMPTS = 2
    }
}