package com.trimettransit.tracker.wear

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.trimettransit.tracker.data.local.DatabaseHelper
import com.trimettransit.tracker.data.local.StopSyncContract
import com.trimettransit.tracker.data.local.toStopSyncJson
import com.trimettransit.tracker.model.Stop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resumeWithException
import timber.log.Timber

/**
 * Publishes the phone's favorites and recent stops to the paired Wear device via
 * the Wearable Data Layer. Both apps share the same applicationId + signature, so
 * the platform guarantees only this pair can read the payloads.
 *
 * A full snapshot is published on every change (and once on app start). The watch
 * replaces its local copy wholesale, so an empty list correctly clears it.
 */
object WearDataSync {

    private const val KEY_STOPS = StopSyncContract.KEY_STOPS

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun start(context: Context) {
        DatabaseHelper.onStopListsChanged = { publishAll(context.applicationContext) }
        publishAll(context.applicationContext)
    }

    fun publishAll(context: Context) {
        scope.launch {
            runCatching {
                val db = DatabaseHelper(context)
                publish(context, StopSyncContract.PATH_FAVORITES, db.favorites)
                publish(context, StopSyncContract.PATH_RECENT, db.recentStops)
            }.onFailure {
                Timber.w(it, "Failed to publish stop lists to Wear")
            }
        }
    }

    private suspend fun publish(context: Context, path: String, stops: List<Stop>) {
        val request = PutDataMapRequest.create(path).apply {
            dataMap.putString(KEY_STOPS, stops.toStopSyncJson())
        }
        Wearable.getDataClient(context).putDataItem(request.asPutDataRequest()).await()
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result) { _, _, _ -> }
            } else {
                continuation.resumeWithException(task.exception ?: RuntimeException("Task failed"))
            }
        }
    }

    fun stop() {
        DatabaseHelper.onStopListsChanged = null
        scope.cancel()
    }
}